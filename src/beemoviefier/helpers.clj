(ns beemoviefier.helpers
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(defn round [precision number]
  (.setScale (bigdec number) precision java.math.RoundingMode/HALF_EVEN))

(defn timestamp-out-file []
  (str "out_" (str/replace (str (java.time.LocalDateTime/now)) #"[-T:.]" "_") ".mp4"))

(defn ffmpeg-nice-print [process-command]
  (let [command process-command]
    (with-open [stream (io/reader (:out command))]
      (doseq [line (line-seq stream)]
        (println line)))))

(defn file-exists [path]
  (.exists (io/as-file path)))
