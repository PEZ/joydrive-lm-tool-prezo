(ns test.seatbelt
  (:require [clojure.string :as string]
            [cljs.test]
            [promesa.core :as p]
            [test.db :as db]
            ["vscode" :as vscode]))

(defn- write [& xs]
  (js/process.stdout.write (string/join " " xs)))

(defn- writeln [& xs]
  (apply write xs)
  (js/process.stdout.write "\n"))

;; Simple custom reporting that avoids recursion
(defn handle-end-run-tests [_m]
  (let [{:keys [running pass fail error]} @db/!state
        passed-minimum-threshold 2
        fail-reason (cond
                      (< 0 (+ fail error)) "FAILURE: Some tests failed or errored"
                      (< pass passed-minimum-threshold) (str "FAILURE: Less than " passed-minimum-threshold " assertions passed")
                      :else nil)]

    (when running
      (if fail-reason
        (p/reject! running fail-reason)
        (p/resolve! running true)))))

(defn custom-report [m]
  ;; Handle our custom reporting without calling original to avoid recursion
  (case (:type m)
    :pass (do
            (swap! db/!state update :pass inc)
            (write "."))
    :fail (do
            (swap! db/!state update :fail inc)
            (write "F"))
    :error (do
             (swap! db/!state update :error inc)
             (write "E"))
    :begin-test-ns (writeln (str "\nTesting " (:ns m)))
    :end-test-ns (writeln (str "Completed " (:test m) " tests"))
    :end-run-tests (do
                     (handle-end-run-tests m)
                     (writeln (str "\nResults: " (:pass @db/!state) " passed, "
                                   (:fail @db/!state) " failed, "
                                   (:error @db/!state) " errors")))
    :summary nil ;; Skip default summary to avoid duplication
    nil))

;; Install the custom report function
(set! cljs.test/report custom-report)

(defn- file->ns [src-path file]
  (-> file
      (subs (inc (count src-path)))
      (string/replace #"\.cljs$" "")
      (string/replace #"/" ".")
      (string/replace #"_" "-")))

(defn- find-test-nss+ [src-path]
  (p/let [file-uris (vscode/workspace.findFiles (str src-path "/**/*test.cljs"))
          files (.map file-uris (fn [uri]
                                  (vscode/workspace.asRelativePath uri false)))
          nss-strings (-> files
                          (.map (partial file->ns src-path))
                          sort)]
    (mapv symbol nss-strings)))

(defn- run-nss-syms! [nss-syms]
  (-> (p/do
        (println "Running tests in" nss-syms)
        (apply require nss-syms)
        (apply cljs.test/run-tests nss-syms)
        (:running @db/!state))
      (p/catch (fn [e]
                 (p/reject! (:running @db/!state) e)))))

(defn run-all-tests! [src-path]
  (let [running (p/deferred)]
    (swap! db/!state assoc :running running)
    (p/let [nss-syms (find-test-nss+ src-path)
            p (run-nss-syms! nss-syms)]
      (println "Runner: tests run, results:" (select-keys  @db/!state [:pass :fail :error]))
      (swap! db/!state merge db/default-db)
      p)
    running))

(comment
  (require '[test.simple-test] :reload)
  (p/let [p+ (run-all-tests! ".joyride/src")]
    (def p+ p+))
  )