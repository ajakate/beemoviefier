(require '[babashka.process :refer [process check sh pipeline pb]])

(def input-file "inputs/test_new.mov")

;; TODO: implement
(def inc-fact 1.15)

(def timestamps
  ["00:08.500"
   "00:13.750"
   "00:17.500"])

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
           ]
       ;; TODO: fix le SPEED
       (str "[0:v]trim=start='" start "':end='" end "',setpts=0.50*(PTS-STARTPTS)[v" idx "];"
            "[0:a]atrim=start='" start "':end='" end "',asetpts=(PTS-STARTPTS),atempo=2.00[a" idx "];"
            )))
   iterator))

(def last-segment
  (str "[0:v]trim=start='" (safely (last timestamps)) "',setpts=0.25*(PTS-STARTPTS)[ev];"
       "[0:a]atrim=start='" (safely (last timestamps)) "',asetpts=(PTS-STARTPTS),atempo=4.00[ea];"))

(def last-line
  (str "[sv][sa]"
       
       (apply str (map-indexed (fn [idx _] (str "[v" idx "]" "[a" idx "]")) iterator))
       
       "[ev][ea]concat=n=4:v=1:a=1"))


(def final
  (str command-start "\"" first-segment (apply str chunkss) last-segment last-line "\"" " outputs/out.mov"))

(spit "run.sh" final)

(sh "sh run.sh")

(sh "rm -rf run.sh")
