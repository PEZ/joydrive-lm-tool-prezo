(ns git-history-fuzzy
  (:require ["vscode" :as vscode]
            [clojure.string :as string]
            [joyride.core :as joyride]
            [promesa.core :as p]))

;; Get Git API and repositories
(defn get-git-api!+ []
  (some-> (vscode/extensions.getExtension  "vscode.git")
          .-exports
          (.getAPI 1)))

(defn get-repositories!+ []
  (some-> (get-git-api!+)
          .-repositories))

(defn get-current-repository!+ []
  (first (get-repositories!+)))

;; Fetch commit history with options
(defn get-commit-history!+
  ([repo] (get-commit-history!+ repo {}))
  ([repo options]
   (let [default-options {:maxEntries 1000}
         merged-options (merge default-options options)]
     (when repo
       (.log repo (clj->js merged-options))))))

;; Format commit for display in QuickPick
(defn format-commit-for-quickpick [commit]
  (def commit commit)
  (comment
    (joyride/js-properties commit)
    )
  (let [hash (.-hash commit)
        short-hash (subs hash 0 7)
        message (.-message commit)
        author-name (.-authorName commit)
        commit-date (.-commitDate commit)
        formatted-date (when commit-date
                         (.toLocaleDateString commit-date))
        ref-names (.-refNames commit)]
    #js {:label (str "$(git-commit) " message)
         :description (str short-hash " - " author-name " - " formatted-date)
         :detail (when-not (string/blank? ref-names)
                   (str "Branches: " ref-names))
         :commit commit
         :alwaysShow true
         :hash hash}))

(def ^:private !decorated-editor (atom nil))

(def line-decoration-type
  (vscode/window.createTextEditorDecorationType #js {:backgroundColor "rgba(255,255,255,0.15)"}))

(defn- clear-decorations! [editor]
  (.setDecorations editor line-decoration-type #js []))

(defn- highlight-item! [item preview?]
  (when (some-> item .-range)
    (p/let [document (vscode/workspace.openTextDocument (.-uri item))
            editor (vscode/window.showTextDocument document #js {:preview preview? :preserveFocus preview?})
            range (.-range item)]
      (.revealRange editor range vscode/TextEditorRevealType.InCenter)
      (clear-decorations! editor)
      (if preview?
        (do
          (.setDecorations editor line-decoration-type #js [range])
          (reset! !decorated-editor editor))
        (set! (.-selection editor)
              (vscode/Selection. (.-start range) (.-start range)))))))

;; Create the QuickPick UI for git history
(defn show-git-history-search!+ []
  (p/let [repo (get-current-repository!+)
          _ (when-not repo
              (throw (js/Error. "No Git repository found in the current workspace")))
          commits (get-commit-history!+ repo)
          commit-items (map format-commit-for-quickpick commits)
          quick-pick (vscode/window.createQuickPick)]

    (set! (.-items quick-pick) (into-array commit-items))
    (set! (.-title quick-pick) "Git History Search")
    (set! (.-placeHolder quick-pick) "Search commit messages, authors, or hashes")
    (set! (.-matchOnDescription quick-pick) true)
    (set! (.-matchOnDetail quick-pick) true)

    (.onDidChangeActive quick-pick (fn [active-items]
                          (highlight-item! (first active-items) true)))

    (.onDidAccept quick-pick
                  (fn [_e]
                    (p/let [selected-item (first (.-selectedItems quick-pick))
                            commit (.-commit selected-item)
                            hash (.-hash commit)]
                      ;; Use the VS Code git.viewCommit command to show the commit
                      (when hash
                        (vscode/commands.executeCommand "git.viewCommit"
                                                        (clj->js {:hash hash
                                                                  :repo repo})))
                      (.dispose quick-pick))))

    ;; Clean up when the QuickPick is hidden
    (.onDidHide quick-pick
                (fn [_e]
                  (.dispose quick-pick)))

    ;; Show the QuickPick
    (.show quick-pick)))

;; Make the function available for direct invocation
(defn show-git-history!+ []
  (p/catch
   (show-git-history-search!+)
   (fn [err]
     (vscode/window.showErrorMessage (str "Error: " err)))))

;; Execute the function when this file is evaluated
(show-git-history!+)