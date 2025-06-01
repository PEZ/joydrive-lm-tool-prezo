(ns ai-mood-selector
  (:require ["vscode" :as vscode]
            [promesa.core :as p]
            [clojure.string :as string]))

(defn ws-root []
  (if (not= js/undefined vscode/workspace.workspaceFolders)
    (.-uri (first vscode/workspace.workspaceFolders))
    (vscode/Uri.parse ".")))

(defn read-file-content+ [file-uri]
  (p/let [file-data (.readFile vscode/workspace.fs file-uri)
          content (-> (js/Buffer.from file-data) (.toString "utf-8"))
          lines (.split content "\n")
          first-line (first lines)]
    {:content content
     :first-line first-line}))

(defn create-quick-pick-item [filename file-uri first-line content]
  (clj->js {:label filename
            :description first-line
            :detail content
            :uri file-uri}))

(defn get-prompt-files+ []
  (let [prompts-dir (vscode/Uri.joinPath (ws-root) "prompts" "system")]
    (p/let [dir-entries (.readDirectory vscode/workspace.fs prompts-dir)
            files (->> dir-entries
                       js->clj
                       (filter (fn [[name type]]
                                 (and (= type 1) ; 1 = file, 2 = directory
                                      (.endsWith name ".md"))))
                       (map first))]
      (p/all (map (fn [filename]
                    (let [file-uri (vscode/Uri.joinPath prompts-dir filename)]
                      (p/let [{:keys [first-line content]} (read-file-content+ file-uri)]
                        (create-quick-pick-item filename file-uri first-line content))))
                  files)))))

;; State to track current mood and status bar item
(defonce !mood-state (atom {:current-mood nil
                            :status-bar-item nil}))

;; Function to read the begin and end common files
(defn read-common-files+ []
  (let [begin-uri (vscode/Uri.joinPath (ws-root) "prompts" "system-common-begin.md")
        end-uri (vscode/Uri.joinPath (ws-root) "prompts" "system-common-end.md")]
    (p/let [begin-data (.readFile vscode/workspace.fs begin-uri)
            end-data (.readFile vscode/workspace.fs end-uri)
            begin-content (-> (js/Buffer.from begin-data) (.toString "utf-8"))
            end-content (-> (js/Buffer.from end-data) (.toString "utf-8"))]
      {:begin begin-content
       :end end-content})))

;; Function to extract mood name from filename
(defn extract-mood-name [filename]
  (-> filename
      (string/replace #"-instructions\.md$" "")
      (string/replace #"-" " ")
      (string/capitalize)))

;; Function to create or update status bar item
(defn update-status-bar! [mood-name]
  (let [item (or (:status-bar-item @!mood-state)
                 (vscode/window.createStatusBarItem vscode/StatusBarAlignment.Left 100))]
    (set! (.-text item) (str "AI: " mood-name))
    (set! (.-tooltip item) "Current AI mood - click to change")
    (set! (.-command item) (clj->js {:command "joyride.runCode"
                                     :arguments ["(ai-mood-selector/show-ai-mood-picker!)"]}))
    (.show item)
    (swap! !mood-state assoc
           :current-mood mood-name
           :status-bar-item item)
    "Status bar updated"))

;; Enhanced function to activate mood with concatenation
(defn activate-mood!
  "Activate an AI mood. Can be called with either:
   - (activate-mood! 'mood-name') - convenient string-based activation
   - (activate-mood! source-uri filename) - URI-based activation"
  ([mood-name]
   (let [filename (str mood-name "-instructions.md")
         prompts-dir (vscode/Uri.joinPath (ws-root) "prompts" "system")
         source-uri (vscode/Uri.joinPath prompts-dir filename)]
     (activate-mood! source-uri filename)))
  ([source-uri filename]
   (p/let [common-files (read-common-files+)
           mood-data (.readFile vscode/workspace.fs source-uri)
           mood-content (-> (js/Buffer.from mood-data) (.toString "utf-8"))

           ;; Concatenate: begin + mood + end
           full-content (str (:begin common-files)
                            (when (not= "" (:begin common-files)) "\n\n")
                            mood-content
                            (when (not= "" (:end common-files)) "\n\n")
                            (:end common-files))

           target-uri (vscode/Uri.joinPath (ws-root) ".github" "copilot-instructions.md")
           mood-name (extract-mood-name filename)]

     (.writeFile vscode/workspace.fs target-uri (js/Buffer.from full-content "utf-8"))
     (update-status-bar! mood-name))))

(defn show-ai-mood-picker! []
  (p/let [items (get-prompt-files+)
          quick-pick (vscode/window.createQuickPick)]
    ;; Configure the quick pick
    (set! (.-title quick-pick) "Select AI mood")
    (set! (.-ignoreFocusOut quick-pick) true)
    (set! (.-items quick-pick) (clj->js items))
    (set! (.-matchOnDescription quick-pick) true)
    (set! (.-matchOnDetail quick-pick) true)

    ;; Handle selection (activate mood with concatenation)
    (.onDidAccept quick-pick
                  (fn []
                    (when-let [selected (first (.-selectedItems quick-pick))]
                      (let [source-uri (.-uri selected)
                            filename (.-label selected)]
                        (.hide quick-pick) ; Dismiss menu immediately
                        (activate-mood! source-uri filename)
                        (vscode/window.showInformationMessage
                         (str "AI Mood '" filename "' activated!"))))))

    ;; Handle hiding
    (.onDidHide quick-pick
                (fn []
                  (.dispose quick-pick)))

    ;; Show the picker
    (.show quick-pick)))

(comment
  ;; Test the AI mood picker
  (show-ai-mood-picker!)

  ;; Test individual functions
  (get-prompt-files+)

  :rcf)
