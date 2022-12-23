(ns beemoviefier.core
  (:require
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as str]
   [beemoviefier.runner :refer [run-ffmpeg]]
   [beemoviefier.remote :refer [run-remote]]))

;; TODO: fix all this
(def cli-options
  [["-i" "--increase-rate INCREASE_RATE" "Rate of speed increase"
    :default 1.15
    :parse-fn #(Float/parseFloat %)
    :validate [#(< 0 % 5.0) "Must be a number between 0 and 5"]]
   ["-o" "--out OUTPUT_FILE" "File to write output video"
    :default "outputs/out.mp4"]
   ["-p" "--playlist PLAYLIST_FILE" "File to "
    :default "inputs/test_movie.m3u"]
   ["-r" "--remote-host REMOTE_HOST" "Remote host for running ffmpeg" :default nil]
   ["-p" "--remote-port REMOTE_PORT" "Remote host for running ffmpeg" :default 22]
   ["-u" "--remote-user REMOTE_USER" "Remote host for running ffmpeg" :default nil]
   ["-k" "--remote-private-key REMOTE_PRIVATE_KEY" "remote host foe" :default "~/.ssh/id_rsa"]
   ["-d" "--remote-directory REMOTE_DIRECTORY" "remote host foe" :default "/tmp"]
   ["-h" "--help"]])

;; TODO: fix this
;; TODO: remote validations
(defn usage [options-summary]
  (->> ["This is my program. There are many like it, but this one is mine."
        ""
        "Usage: program-name [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  start    Start a new server"
        "  stop     Stop an existing server"
        "  status   Print a server's status"
        ""
        "Please refer to the manual page for more information."]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) {:exit-message (usage summary) :ok? true}
      errors {:exit-message (error-msg errors)}
      (= 1 (count arguments)) {:action (first arguments) :options options}
      :else {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)

      ;; TODO: pass in as map? fix this
      (if (:remote-host options)
        (run-remote action options)
        (run-ffmpeg action (:out options) (:playlist options) (:increase-rate options))))))