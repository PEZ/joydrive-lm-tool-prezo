(ns ai-mood-selector
  (:require ["vscode" :as vscode]
            [promesa.core :as p]
            [clojure.string :as string]))

(defonce !mood-state (atom {:current-mood nil
                            :status-bar-item nil}))

(defn ^:export get-current-mood []
  (:current-mood @!mood-state))

(defn ws-root []
  (if (not= js/undefined vscode/workspace.workspaceFolders)
    (.-uri (first vscode/workspace.workspaceFolders))
    (vscode/Uri.parse ".")))

(defn create-quick-pick-item [filename file-uri]
  (clj->js {:label (string/replace filename #"-instructions.md$" "")
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
                      (create-quick-pick-item filename file-uri)))
                  files)))))

(defn read-common-files+ []
  (let [begin-uri (vscode/Uri.joinPath (ws-root) "prompts" "system-common-begin.md")
        end-uri (vscode/Uri.joinPath (ws-root) "prompts" "system-common-end.md")]
    (p/let [begin-data (.readFile vscode/workspace.fs begin-uri)
            end-data (.readFile vscode/workspace.fs end-uri)
            begin-content (-> (js/Buffer.from begin-data) (.toString "utf-8"))
            end-content (-> (js/Buffer.from end-data) (.toString "utf-8"))]
      {:begin begin-content
       :end end-content})))

(defn extract-mood-name [filename]
  (-> filename
      (string/replace #"-instructions\.md$" "")
      (string/replace #"-" " ")
      (string/capitalize)))

(defn update-status-bar! [mood-name]
  (let [item (or (:status-bar-item @!mood-state)
                 (vscode/window.createStatusBarItem vscode/StatusBarAlignment.Left -1001))]
    (set! (.-text item) (str "🎭 " mood-name))
    (set! (.-tooltip item) "Current AI mood - click to change")
    (set! (.-command item) (clj->js {:command "joyride.runCode"
                                     :arguments ["(ai-mood-selector/show-ai-mood-picker!)"]}))
    (.show item)
    (swap! !mood-state assoc
           :current-mood mood-name
           :status-bar-item item)
    #js {:dispose (fn []
                    (swap! !mood-state dissoc :status-bar-item)
                    (.dispose item))}))

;; New pure functions
(defn compose-mood-content
  "Pure function to compose the full mood content from its parts"
  [begin-content mood-content end-content]
  (str begin-content
       (when (seq begin-content) "\n\n")
       mood-content
       (when (seq end-content) "\n\n")
       end-content))

(defn build-mood-config
  "Pure function to build mood configuration data"
  [mood-name source-uri target-uri]
  {:mood-name mood-name
   :source-uri source-uri
   :target-uri target-uri})

;; New I/O functions
(defn gather-mood-sources+
  "Gather all source content needed for mood activation"
  [config]
  (p/let [common-files (read-common-files+)
          mood-data (.readFile vscode/workspace.fs (:source-uri config))
          mood-content (-> (js/Buffer.from mood-data) (.toString "utf-8"))]
    (merge common-files {:mood mood-content})))

(defn get-current-system-instructions+
  "Read the current system instructions from the active mood."
  []
  (let [current-mood (get-current-mood)]
    (if current-mood
      (p/let [instructions-uri (vscode/Uri.joinPath
                                (ws-root)
                                ".github"
                                "copilot-instructions.md")
              file-data (vscode/workspace.fs.readFile instructions-uri)
              content (-> (js/Buffer.from file-data) (.toString "utf-8"))]
        content)
      (p/resolved nil))))

(defn get-system-prompt-for-mood+
  "Get the complete system prompt for a given mood name"
  [mood-name]
  (let [filename (str mood-name "-instructions.md")
        prompts-dir (vscode/Uri.joinPath (ws-root) "prompts" "system")
        source-uri (vscode/Uri.joinPath prompts-dir filename)
        config (build-mood-config mood-name source-uri nil)]
    (p/let [sources (gather-mood-sources+ config)]
      (compose-mood-content (:begin sources)
                          (:mood sources)
                          (:end sources)))))

(defn write-mood-file!+
  "Write the composed mood content to target file"
  [config content]
  (.writeFile vscode/workspace.fs (:target-uri config)
              (js/Buffer.from content "utf-8")))

;; Refactored main function
(defn activate-mood!
  "Activate an AI mood with data-oriented approach"
  ([mood-name]
   (let [filename (str mood-name "-instructions.md")
         prompts-dir (vscode/Uri.joinPath (ws-root) "prompts" "system")
         source-uri (vscode/Uri.joinPath prompts-dir filename)]
     (activate-mood! source-uri filename)))
  ([source-uri filename]
   (let [config (build-mood-config
                 (extract-mood-name filename)
                 source-uri
                 (vscode/Uri.joinPath (ws-root) ".github" "copilot-instructions.md"))]
     (p/let [sources (gather-mood-sources+ config)
             content (compose-mood-content (:begin sources)
                                           (:mood sources)
                                           (:end sources))]
       (write-mood-file!+ config content)
       (update-status-bar! (:mood-name config))))))

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