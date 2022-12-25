(ns beemoviefier.helpers
  (:require [clojure.string :as str]))

(defn round [precision number]
  (.setScale (bigdec number) precision java.math.RoundingMode/HALF_EVEN))

(defn timestamp-out-file []
  (str "out_" (str/replace (str (java.time.LocalDateTime/now)) #"[-T:.]" "_") ".mp4"))

(defn ffmpeg-nice-print [process-command]
  (let [command process-command
        line (atom "")]
    (loop [c (.read (:out command))]
      (when (not= c -1)
        (if (= \newline (char c))
          (if (str/ends-with? @line "progress=continue")
            (do (println @line)
                (reset! line ""))
            (swap! line  #(str % \tab)))
          (swap! line  #(str % (char c))))
        (recur (.read (:out command)))))))
