(ns ai-mood-selector
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

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
  (let [prompts-dir (vscode/Uri.joinPath (ws-root) "prompts")]
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

(defn show-ai-mood-picker! []
  (p/let [items (get-prompt-files+)
          quick-pick (vscode/window.createQuickPick)]
    ;; Configure the quick pick
    (set! (.-title quick-pick) "Select AI mood")
    (set! (.-ignoreFocusOut quick-pick) true)
    (set! (.-items quick-pick) (clj->js items))
    (set! (.-matchOnDescription quick-pick) true)
    (set! (.-matchOnDetail quick-pick) true)

    ;; Handle selection (copy file to .github/copilot-instructions.md)
    (.onDidAccept quick-pick
                  (fn []
                    (when-let [selected (first (.-selectedItems quick-pick))]
                      (let [source-uri (.-uri selected)
                            target-uri (vscode/Uri.joinPath (ws-root) ".github" "copilot-instructions.md")
                            filename (.-label selected)]
                        (.hide quick-pick) ; Dismiss menu immediately
                        (p/let [file-data (.readFile vscode/workspace.fs source-uri)]
                          (.writeFile vscode/workspace.fs target-uri file-data)
                          (-> (vscode/window.showInformationMessage
                               (str "AI Mood " filename " activated!")
                               "Open mood file")
                              (p/then (fn [selection]
                                        (when (= selection "Open mood file")
                                          (.executeCommand vscode/commands "vscode.open" source-uri))))))))))

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
