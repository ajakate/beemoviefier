(require '[babashka.process :refer [sh]])

(def input-file "inputs/test_new.mov")

(def inc-fact 2.0)

(defn print-round [num, prec]
  (apply str (take prec (str num))))

(defn get-speeds-for-index [idx]
  (let [raw-speed (reduce * (repeat (+ 1 idx) (float inc-fact)))
        inverse (/ 1 raw-speed)]
    {:audio (print-round raw-speed 6)
     :video (print-round inverse 6)}))

(def timestamps
  ["00:08.500"
   "00:13.750"
   "00:17.500"])

(def num-segments (+ (count timestamps) 1))

(defn safely [stringg]
  (let [chary (seq (char-array stringg))]
    (reduce (fn [state item]
              (if (= item \:)
                (str state \\ \:)
                (str state item)))
            ""
            chary)))

(def firstone (first timestamps))

(def command-start (str "ffmpeg -y -i " input-file " -filter_complex "))

(def first-segment
  (str "[0:v]trim=" firstone
       ",setpts=(PTS-STARTPTS)[sv];[0:a]atrim="
       firstone
       ",asetpts=(PTS-STARTPTS)[sa];"))

(def iterator (map vector timestamps (rest timestamps)))

(def chunkss
  (map-indexed
   (fn [idx stamps]
     (let [start (safely (first stamps))
           end (safely (second stamps))
           speeds (get-speeds-for-index idx)]
       (str "[0:v]trim=start='" start "':end='" end "',setpts=" (:video speeds) "*(PTS-STARTPTS)[v" idx "];"
            "[0:a]atrim=start='" start "':end='" end "',asetpts=(PTS-STARTPTS),atempo=" (:audio speeds) "[a" idx "];")))
   iterator))

(def last-segment
  (let [speeds (get-speeds-for-index (- num-segments 1))] 
    (str "[0:v]trim=start='" (safely (last timestamps)) "',setpts=" (:video speeds) "*(PTS-STARTPTS)[ev];"
         "[0:a]atrim=start='" (safely (last timestamps)) "',asetpts=(PTS-STARTPTS),atempo=" (:audio speeds) "[ea];")))

(def last-line
  (str "[sv][sa]"
       (apply str (map-indexed (fn [idx _] (str "[v" idx "]" "[a" idx "]")) iterator))
       "[ev][ea]concat=n=" num-segments ":v=1:a=1"))

(def final
  (str command-start "\"" first-segment (apply str chunkss) last-segment last-line "\"" " outputs/out.mov"))

(spit "run.sh" final)

(sh "sh run.sh")

(sh "rm -rf run.sh")
