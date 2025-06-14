(ns git-history-fuzzy
  (:require ["vscode" :as vscode]
            [clojure.string :as string]
            [joyride.core :as joyride]
            [promesa.core :as p]))

;; Get Git API and repositories
(defn get-git-api!+ []
  (p/let [git-extension (.getExtension vscode/extensions "vscode.git")]
    (when git-extension
      (.. git-extension -exports (getAPI 1)))))

(defn get-repositories!+ []
  (p/let [git-api (get-git-api!+)]
    (when git-api
      (.-repositories git-api))))

(defn get-current-repository!+ []
  (p/let [repos (get-repositories!+)]
    (when (seq repos)
      (first repos))))

;; Fetch commit history with options
(defn get-commit-history!+
  ([repo] (get-commit-history!+ repo {}))
  ([repo options]
   (let [default-options {:maxEntries 100}
         merged-options (merge default-options options)]
     (when repo
       (.log repo (clj->js merged-options))))))

;; Format commit for display in QuickPick
(defn format-commit-for-quickpick [commit]
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

    ;; Handle selection
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