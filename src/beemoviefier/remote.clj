(ns beemoviefier.remote
  (:require [babashka.pods :as pods]
            [clojure.java.io :as io]
            [beemoviefier.runner :refer [build-script]]
            [beemoviefier.helpers :refer [round timestamp-out-file ffmpeg-nice-print]]
            [babashka.process :refer [shell]]))

(pods/load-pod 'epiccastle/bbssh "0.2.0")

(require '[pod.epiccastle.bbssh.core :as bbssh]
         '[pod.epiccastle.bbssh.scp :as scp])

(defn remote-filename [file path]
  (str path "/" (.getName (io/as-file file))))

(defn display-transfer-percentage [status]
  (->> (/  (float (:offset status)) (:size status))
       (* 100)
       (round 1)))

(defn progress-msg-function [msg]
  (fn [progress-context status]
    (prn (str msg (display-transfer-percentage status) "% complete"))
    (inc progress-context)))

(defn run-remote [input-file {:keys [remote-private-key remote-host remote-user remote-port remote-directory] :as options}]
  (shell (str "ssh-add " remote-private-key))

  (let [remote-input-filepath (remote-filename input-file remote-directory)
        script-string (build-script remote-input-filepath "out.mp4" options)]
    (spit "run.sh" script-string)

    (let [session (bbssh/ssh remote-host {:username remote-user
                                          :port remote-port
                                          :identity remote-private-key})]

      (scp/scp-to [(io/as-file input-file) (io/as-file "run.sh")]
                  remote-directory
                  {:session session
                   :progress-context 0
                   :progress-fn (progress-msg-function "Copying files to server... ")})

      (println "\n\nStarting FFMPEG...\n\n")

      (ffmpeg-nice-print (bbssh/exec session  (str "sh " remote-directory "/run.sh")
                                     {:out :stream}))

      (println "\n\nDONE!! Copying output video back to your machine...\n\n")

      (scp/scp-from "out.mp4"
                    (timestamp-out-file)
                    {:session session
                     :progress-context 0
                     :progress-fn (progress-msg-function "Downloading finished video... ")})

      (println "\n\nDONE!! Cleaning up...\n\n")

      ;; TODO: remote-directory better
      (-> (bbssh/exec session (str "rm " "out.mp4 "  remote-input-filepath " " remote-directory "/run.sh") {:out :string})
          deref
          :out))))
