(ns beemoviefier.runner
  (:require [babashka.process :refer [shell process]]
            [beemoviefier.helpers :refer [ffmpeg-nice-print]]
            [clojure.string :as str]))

(def rate-precision 6)

(defn convert-time [seconds offset]
  (let [split-time (str/split seconds #"\.")
        raw-seconds (first split-time)
        raw-millis (second split-time)
        raw-seconds-int (Integer/parseInt raw-seconds)
        raw-seconds-with-offset (- raw-seconds-int offset)
        full-hours (quot raw-seconds-with-offset 3600)
        remaining-minutes (rem raw-seconds-with-offset 3600)
        full-minutes (quot remaining-minutes 60)
        full-seconds (rem remaining-minutes 60)]
    (str
     (format "%02d" full-hours) "\\:"
     (format "%02d" full-minutes) "\\:"
     (format "%02d" full-seconds) "." raw-millis)))

(defn format-decimal [dec]
  (str/replace (format "%.200f" dec) #"0+$" "0"))

(defn format-audio-string [audios]
  (apply str (map #(str ",atempo=" %) audios)))

(defn get-audios [raw]
  (loop [running raw
         total []]
    (if (> 100.0 running) 
      (conj total running)
      (recur (with-precision rate-precision (/ running 100M)) (conj total 100M)))))

(defn get-speeds-for-index [idx rate limit]
  (let [raw (reduce * (repeat (+ 1 idx) rate))
        audio (if (and limit (> raw limit)) limit raw)
        inverse (with-precision rate-precision (/ 1M audio))
        audios (get-audios audio)]
    {:audio (map format-decimal audios)
     :video (format-decimal inverse)}))

(defn build-script [output-file {:keys [input-file playlist increase-rate limit offset]}]
  (let [timestamps (->> (slurp playlist)
                        (re-seq  #"(?s)time=(\d+.\d+)}")
                        (map second))
        num-segments (+ (count timestamps) 1)
        command-start (str "ffmpeg -y -i " input-file "  -filter_complex ")
        first-segment (let [first-stamp (convert-time (first timestamps) offset)]
                        (str "[0:v]trim=end='" first-stamp
                             "',setpts=(PTS-STARTPTS)[sv];[0:a]atrim=end='"
                             first-stamp
                             "',asetpts=(PTS-STARTPTS)[sa];"))
        iterator (map vector timestamps (rest timestamps))
        chunkss (map-indexed
                 (fn [idx stamps]
                   (let [start (convert-time (first stamps) offset)
                         end (convert-time (second stamps) offset)
                         speeds (get-speeds-for-index idx increase-rate limit)]
                     (str "[0:v]trim=start='" start "':end='" end "',setpts=" (:video speeds) "*(PTS-STARTPTS)[v" idx "];"
                          "[0:a]atrim=start='" start "':end='" end "',asetpts=(PTS-STARTPTS)" (format-audio-string (:audio speeds)) "[a" idx "];")))
                 iterator)
        last-segment (let [speeds (get-speeds-for-index (- num-segments 2) increase-rate limit)]
                       (str "[0:v]trim=start='" (convert-time (last timestamps) offset) "',setpts=" (:video speeds) "*(PTS-STARTPTS)[ev];"
                            "[0:a]atrim=start='" (convert-time (last timestamps) offset) "',asetpts=(PTS-STARTPTS)" (format-audio-string (:audio speeds)) "[ea];"))
        last-line (str "[sv][sa]"
                       (apply str (map-indexed (fn [idx _] (str "[v" idx "]" "[a" idx "]")) iterator))
                       "[ev][ea]concat=n=" num-segments ":v=1:a=1")
        final (str "set -x\n\n" command-start "\"" first-segment (apply str chunkss) last-segment last-line "\" " output-file " 2>&1")]
    final))

(defn run-local [output-file options]
  (let [full-script (build-script output-file options)]
    (spit "run.sh" full-script)
    (ffmpeg-nice-print (process "sh run.sh" {:out :stream}))
    (shell "rm -rf run.sh")))
