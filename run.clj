(require '[babashka.process :refer [sh shell]]
         '[clojure.string :as str])

(def input-file "inputs/test_new.mov")
(def input-playlist "inputs/test_movie.m3u")
(def output-file "outputs/out.mp4")

(def inc-fact 1.14)

(defn print-round [num, prec]
  (apply str (take prec (str num))))

(defn convert-time [seconds]
  (let [split-time (str/split seconds #"\.")
        raw-seconds (first split-time)
        raw-millis (second split-time)
        raw-seconds-int (Integer/parseInt raw-seconds)
        full-hours (quot raw-seconds-int 3600)
        remaining-minutes (rem raw-seconds-int 3600)
        full-minutes (quot remaining-minutes 60)
        full-seconds (rem remaining-minutes 60)]
    (str
     (format "%02d" full-hours) "\\:"
     (format "%02d" full-minutes) "\\:"
     (format "%02d" full-seconds) "." raw-millis)))

(defn get-speeds-for-index [idx]
  (let [raw-speed (reduce * (repeat (+ 1 idx) (float inc-fact)))
        inverse (/ 1 raw-speed)]
    {:audio (print-round raw-speed 6)
     :video (print-round inverse 6)}))

(def stamps-file (slurp input-playlist))

(def stamps-match (re-seq #"(?s)time=(\d+.\d+)}" stamps-file))

(def timestamps (map second stamps-match))

(def num-segments (+ (count timestamps) 1))

(def command-start (str "ffmpeg -y -i " input-file "  -filter_complex "))

(def first-segment
  (let [first-stamp (convert-time (first timestamps))]
    (str "[0:v]trim=end='" first-stamp
         "',setpts=(PTS-STARTPTS)[sv];[0:a]atrim=end='"
         first-stamp
         "',asetpts=(PTS-STARTPTS)[sa];")))

(def iterator (map vector timestamps (rest timestamps)))

(def chunkss
  (map-indexed
   (fn [idx stamps]
     (let [start (convert-time (first stamps))
           end (convert-time (second stamps))
           speeds (get-speeds-for-index idx)]
       (str "[0:v]trim=start='" start "':end='" end "',setpts=" (:video speeds) "*(PTS-STARTPTS)[v" idx "];"
            "[0:a]atrim=start='" start "':end='" end "',asetpts=(PTS-STARTPTS),atempo=" (:audio speeds) "[a" idx "];")))
   iterator))

(def last-segment
  (let [speeds (get-speeds-for-index (- num-segments 2))]
    (str "[0:v]trim=start='" (convert-time (last timestamps)) "',setpts=" (:video speeds) "*(PTS-STARTPTS)[ev];"
         "[0:a]atrim=start='" (convert-time (last timestamps)) "',asetpts=(PTS-STARTPTS),atempo=" (:audio speeds) "[ea];")))

(def last-line
  (str "[sv][sa]"
       (apply str (map-indexed (fn [idx _] (str "[v" idx "]" "[a" idx "]")) iterator))
       "[ev][ea]concat=n=" num-segments ":v=1:a=1"))

(def final
  (str "set -x\n\n" command-start "\"" first-segment (apply str chunkss) last-segment last-line "\" " output-file))

(spit "run.sh" final)

(shell "sh run.sh")

(sh "rm -rf run.sh")
