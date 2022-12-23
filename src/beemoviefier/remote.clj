(ns beemoviefier.remote
  (:require [babashka.pods :as pods]
            [clojure.java.io :as io]
            [beemoviefier.runner :refer [build-script]]
            [babashka.process :refer [sh shell]]))

(pods/load-pod 'epiccastle/bbssh "0.2.0")

(require '[pod.epiccastle.bbssh.core :as bbssh]
         '[pod.epiccastle.bbssh.scp :as scp])

(defn remote-filename [file path]
  (str path "/" (.getName (io/as-file file))))

(defn round [s n]
  (.setScale (bigdec n) s java.math.RoundingMode/HALF_EVEN))

(defn run-remote [action {:keys [remote-private-key remote-host remote-user remote-port playlist remote-directory increase-rate] :as options}]
  (println (io/as-file playlist))
  (shell (str "ssh-add " remote-private-key))


  (let [remote-input-filepath (remote-filename action remote-directory)
        script-string (build-script remote-input-filepath "out.mp4" playlist increase-rate)]
    (spit "run.sh" script-string)

    (let [session (bbssh/ssh remote-host {:username remote-user
                                          :port remote-port
                                          :identity remote-private-key})]

      (scp/scp-to [(io/as-file action) (io/as-file "run.sh")]
                  remote-directory
                  {:session session
                   :progress-context 0
                   :progress-fn (fn [progress-context status]
                                  (prn (str "Copying files to server... " (round 3 (* 100 (/  (float (:offset status)) (:size status)))) "% complete"))
                                  (inc progress-context))})

      (println "\n\nStarting FFMPEG...\n\n")

      (let [command (bbssh/exec session  (str "sh " remote-directory "/run.sh")
                                {:out :stream :pty true})
            line (atom "")]
        (loop [c (.read (:out command))]
          (when (not= c -1)
            (if (= \newline (char c))
              (do
                (println @line)
                (reset! line ""))
              (swap! line  #(str % (char c))))
            (recur (.read (:out command))))))

      (println "\n\nDONE!! Copying output video back to your machine...\n\n")

      (scp/scp-from "out.mp4"
                    ;; TODO: str/replace
                    (str "out_" (clojure.string/replace (str (java.time.LocalDateTime/now)) #"[-T:.]" "_") ".mp4")
                    {:session session
                     :progress-context 0
                     :progress-fn (fn [progress-context status]
                                    (prn (str "Downloading finished video... " (round 3 (* 100 (/  (float (:offset status)) (:size status)))) "% complete"))
                                    (inc progress-context))})

      (println "\n\nDONE!! Cleaning up...\n\n")

      ;; TODO: remote-directory better
      (-> (bbssh/exec session (str "rm " "out.mp4 "  remote-input-filepath " " remote-directory "/run.sh" ) {:out :string})
          deref
          :out))))
