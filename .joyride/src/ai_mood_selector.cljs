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
    
    ;; Handle active item changes (preview)
    (.onDidChangeActive quick-pick
      (fn [active-items]
        (when (> (.-length active-items) 0)
          (let [item (first active-items)
                uri (.-uri item)]
            (.executeCommand vscode/commands "vscode.open" uri #js{:preview true})))))
    
    ;; Handle selection (open for editing)
    (.onDidAccept quick-pick
      (fn []
        (when-let [selected (first (.-selectedItems quick-pick))]
          (let [uri (.-uri selected)]
            (.executeCommand vscode/commands "vscode.open" uri #js{:preview false})
            (.hide quick-pick)))))
    
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
