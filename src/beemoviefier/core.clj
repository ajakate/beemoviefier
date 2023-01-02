(ns beemoviefier.core
  (:require
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as str]
   [beemoviefier.helpers :refer [timestamp-out-file file-exists]]
   [beemoviefier.runner :refer [run-local]]
   [beemoviefier.remote :refer [run-remote]]
   [babashka.fs :as fs]))

(def cli-options
  [["-i" "--increase-rate RATE" "Rate of speed increase"
    :default 1.15
    :parse-fn #(bigdec %)
    :validate [#(< 0 % 5.0) "Must be a number between 0 and 5"]]
   ["-o" "--offset SECONDS" "VLC bookmark offset in seconds"
    :default 1
    :parse-fn #(Integer/parseInt %)]
   ["-l" "--limit RATE_LIMIT" "max rate to speed up video (may be needed for out of memory issues from ffmpeg)"
    :parse-fn #(bigdec %)]
   ["-r" "--remote-host HOSTNAME" "Remote host for running ffmpeg"]
   ["-p" "--remote-port PORT" "Port for remote host" :default 22]
   ["-u" "--remote-user USERNAME" "Username for remote host"]
   ["-k" "--remote-private-key PRIVATE_KEY" "Private key for remote host" :default "~/.ssh/id_rsa"]
   ["-d" "--remote-directory DIRECTORY" "Directory on remote host" :default "/tmp"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["beemoviefier Version 0.1.0"
        ""
        "Usage: bb run input_video_file playlist_file [options]"
        ""
        "Options:"
        options-summary
        ""
        ""
        "Please refer to https://github.com/ajakate/beemoviefier for full docs."]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        {:keys [remote-host remote-user]} options
        [video-file playlist-file] arguments]
    (cond
      (:help options) {:exit-message (usage summary) :ok? true}
      (not (and video-file (file-exists video-file))) {:exit-message "Please supply a valid input video and playlist file"}
      (not (and playlist-file (file-exists playlist-file))) {:exit-message "Please supply a valid input video and playlist file"}
      (and remote-host
           (not remote-user)) {:exit-message "If remote host is provided, the remaining remote options must also be supplied (run with --help for more info)"}
      errors {:exit-message (error-msg errors)}
      (= 2 (count arguments)) {:options (assoc options :input-file video-file :playlist playlist-file)}
      :else {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (dorun (->> (fs/glob "." "*babashka-pod-*.port" {:hidden true}) (map str) (map fs/delete-on-exit)))
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (if (:remote-host options)
        (run-remote options)
        (run-local (timestamp-out-file) options)))))
