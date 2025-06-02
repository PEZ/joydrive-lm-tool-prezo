;; == Keyboard shortcuts ==
;; Weird indent because of how comment block/selection works
  ;; {
  ;;   "key": "ctrl+alt+j s",
  ;;   "command": "joyride.runCode",
  ;;   "args": "(next-slide/activate!)"
  ;; },
  ;; {
  ;;   "key": "ctrl+alt+j ctrl+alt+s",
  ;;   "command": "joyride.runCode",
  ;;   "args": "(next-slide/deactivate!)"
  ;; },
  ;; {
  ;;   "key": "ctrl+alt+j ctrl+alt+m",
  ;;   "command": "markdown.showPreview"
  ;; },
  ;; {
  ;;   "key": "right",
  ;;   "command": "joyride.runCode",
  ;;   "args": "(next-slide/next! true)",
  ;;   "when": "next-slide:active && !inputFocus"
  ;; },
  ;; {
  ;;   "key": "left",
  ;;   "command": "joyride.runCode",
  ;;   "args": "(next-slide/next! false)",
  ;;   "when": "next-slide:active && !inputFocus"
  ;; },
  ;; {
  ;;   "key": "pagedown",
  ;;   "command": "joyride.runCode",
  ;;   "args": "(next-slide/next! true)",
  ;;   "when": "next-slide:active"
  ;; },
  ;; {
  ;;   "key": "pageup",
  ;;   "command": "joyride.runCode",
  ;;   "args": "(next-slide/next! false)",
  ;;   "when": "next-slide:active"
  ;; },
  ;; {
  ;;   "key": "F5",
  ;;   "command": "workbench.action.toggleZenMode",
  ;;   "when": "next-slide:active && !inZenMode"
  ;; },
  ;; {
  ;;   "key": "F5",
  ;;   "command": "joyride.runCode",
  ;;   "args": "(next-slide/current!)",
  ;;   "when": "next-slide:active && inZenMode"
  ;; },
  ;; {
  ;;   "key": "ctrl+alt+cmd+left",
  ;;   "command": "joyride.runCode",
  ;;   "args": "(next-slide/restart!)"
  ;; },

(ns next-slide
  (:require ["vscode" :as vscode]
            ["path" :as path]
            [joyride.core :as joyride]
            [promesa.core :as p]
            [clojure.edn :as edn]))

(def !state (atom {:next/active? false
                   :next/active-slide 0
                   :next/config-path nil}))

(defn ws-root []
  (if (not= js/undefined
            vscode/workspace.workspaceFolders)
    (.-uri (first vscode.workspace.workspaceFolders))
    (vscode/Uri.parse ".")))

(defn get-config!+ [config-path]
  (p/let [config-uri (vscode/Uri.joinPath (ws-root) config-path)
          config-data (vscode/workspace.fs.readFile config-uri)
          config-text (-> (js/Buffer.from config-data) (.toString "utf-8"))
          config (edn/read-string config-text)]
    config))

(defn slides-list+ []
  (p/let [config (get-config!+ (apply path/join (:next/config-path @!state)))]
    (:slides config)))

(defn current! []
  (p/let [slides (slides-list+)]
    (vscode/commands.executeCommand
     "markdown.showPreview"
     (vscode/Uri.joinPath (ws-root)
                          (nth slides (:next/active-slide @!state))))))

(defn next!
  ([]
   (next! true))
  ([forward?]
   (p/let [slides (slides-list+)
           next (if forward?
                  #(min (inc %) (dec (count slides)))
                  #(max (dec %) 0))]
     (swap! !state update :next/active-slide next)
     (current!))))

(defn restart!
  []
  (swap! !state assoc :next/active-slide 0)
  (current!))

(defn deactivate! []
  (swap! !state assoc :next/active? false)
  (vscode/commands.executeCommand "setContext" "next-slide:active" false) (vscode/window.showInformationMessage
   (str "next-slide:" "deactivated")))

(defn activate!
  ([]
   (activate! ["slides.edn"]))
  ([config-path]
   (swap! !state assoc
          :next/active? true
          :next/config-path config-path)
   (vscode/commands.executeCommand "setContext" "next-slide:active" true)
   (vscode/window.showInformationMessage
    (str "next-slide:" "activated"))))

(defn show-slide-by-name!+
  "Show a slide by its filename (e.g. 'hello.md' or 'slides/hello.md')"
  [slide-name]
  (p/let [slides (slides-list+)
          slide-index (->> slides
                           (map-indexed vector)
                           (filter (fn [[_idx slide-path]]
                                     (or (= slide-path slide-name)
                                         (.endsWith slide-path slide-name))))
                           ffirst)]
    (if slide-index
      (do
        (swap! !state assoc :next/active-slide slide-index)
        (current!))
      (do
        (js/console.warn "Slide not found:" slide-name "Available slides:" slides)
        (vscode/window.showWarningMessage (str "Slide not found: " slide-name))))))

(comment
  (show-slide-by-name!+ "ecosystem.md")
  :rcf)

(when (= (joyride/invoked-script) joyride/*file*)
  (activate!))

(comment
  @!state
  (next!)
  (next! false)
  (activate!)
  (restart!)
  (deactivate!)
  :rcf)