(ns beemoviefier.runner
  (:require [babashka.process :refer [sh shell]]
           [clojure.string :as str]))

(def rate-precision 6)

(defn rounded-number-string [num precision]
  (apply str (take precision (str num))))

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

(defn get-speeds-for-index [idx rate]
  (let [raw-speed (reduce * (repeat (+ 1 idx) rate))
        inverse (/ 1 raw-speed)]
    {:audio (rounded-number-string raw-speed rate-precision)
     :video (rounded-number-string inverse rate-precision)}))

(defn run-ffmpeg [input-file output-file playlist-file inc-rate]
  (let [stamps-file (slurp playlist-file)
        stamps-match (re-seq #"(?s)time=(\d+.\d+)}" stamps-file)
        timestamps (map second stamps-match)
        num-segments (+ (count timestamps) 1)
        command-start (str "ffmpeg -y -i " input-file "  -filter_complex ")
        first-segment (let [first-stamp (convert-time (first timestamps))]
                        (str "[0:v]trim=end='" first-stamp
                             "',setpts=(PTS-STARTPTS)[sv];[0:a]atrim=end='"
                             first-stamp
                             "',asetpts=(PTS-STARTPTS)[sa];"))
        iterator (map vector timestamps (rest timestamps))
        chunkss (map-indexed
                 (fn [idx stamps]
                   (let [start (convert-time (first stamps))
                         end (convert-time (second stamps))
                         speeds (get-speeds-for-index idx inc-rate)]
                     (str "[0:v]trim=start='" start "':end='" end "',setpts=" (:video speeds) "*(PTS-STARTPTS)[v" idx "];"
                          "[0:a]atrim=start='" start "':end='" end "',asetpts=(PTS-STARTPTS),atempo=" (:audio speeds) "[a" idx "];")))
                 iterator)
        last-segment (let [speeds (get-speeds-for-index (- num-segments 2) inc-rate)]
                       (str "[0:v]trim=start='" (convert-time (last timestamps)) "',setpts=" (:video speeds) "*(PTS-STARTPTS)[ev];"
                            "[0:a]atrim=start='" (convert-time (last timestamps)) "',asetpts=(PTS-STARTPTS),atempo=" (:audio speeds) "[ea];"))
        last-line (str "[sv][sa]"
                       (apply str (map-indexed (fn [idx _] (str "[v" idx "]" "[a" idx "]")) iterator))
                       "[ev][ea]concat=n=" num-segments ":v=1:a=1")
        final (str "set -x\n\n" command-start "\"" first-segment (apply str chunkss) last-segment last-line "\" " output-file)]

    (spit "run.sh" final)
    (shell "sh run.sh")
    (sh "rm -rf run.sh")))
