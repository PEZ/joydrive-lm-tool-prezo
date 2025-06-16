(ns git-history-fuzzy
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

;; “Install” by adding something like this to your keybindings.json
  ;; {
  ;;   "key": "ctrl+alt+f",
  ;;   "command": "joyride.runCode",
  ;;   "args": "(git-history-fuzzy/show-git-history!+)"
  ;; },

;; vscode.git API
;; https://github.com/Microsoft/vscode/blob/main/extensions/git/src/api/git.d.ts

;; If you want to have the git fuzzy search feature outside of this project,
;; add it to your Joyride User scripts library. See:
;; https://github.com/BetterThanTomorrow/joyride/blob/master/README.md#user-and-workspace-scripts

(def max-entries 5000)

(defn get-git-api!+ []
  (some-> (vscode/extensions.getExtension  "vscode.git")
          .-exports
          (.getAPI 1)))

(defn get-repositories!+ []
  (some-> (get-git-api!+)
          .-repositories))

(defn get-current-repository!+ []
  (first (get-repositories!+)))

(defn get-commit-history!+
  ([repo] (get-commit-history!+ repo {}))
  ([repo options]
   (let [default-options {:maxEntries max-entries}
         merged-options (merge default-options options)]
     (when repo
       (.log repo (clj->js merged-options))))))

(defn format-file-for-quickpick [commit file-change]
  (let [hash (.-hash commit)
        short-hash (subs hash 0 7)
        message (.-message commit)
        author-name (.-authorName commit)
        commit-date (.-commitDate commit)
        formatted-date (when commit-date
                         (.toUTCString commit-date))
        file-uri (.-uri file-change)
        file-path (vscode/workspace.asRelativePath file-uri)
        status (.-status file-change)]
    #js {:label (str "$(file) " message)
         :description (str file-path)
         :detail (str "$(git-commit) " short-hash " - " author-name " - " formatted-date " Status: " status)
         :commit commit
         :fileChange file-change
         :hash hash}))

(defn show-file-diff!+ [commit file-change preview?]
  (p/let [git-api (get-git-api!+)]
    (when (and git-api commit file-change)
      (let [hash (.-hash commit)
            parents (.-parents commit)
            uri (.-uri file-change)
            file-path (vscode/workspace.asRelativePath uri)
            parent-hash (first parents)
            status (.-status file-change)
            uri1 (.toGitUri git-api uri (if (#{1 11} status)
                                          hash
                                          parent-hash))
            uri2 (.toGitUri git-api uri (if (= 6 status)
                                          parent-hash
                                          hash))
            title (str "Diff: " file-path " (" (subs parent-hash 0 7) " → " (subs hash 0 7) ")")]
        (vscode/commands.executeCommand "vscode.diff"
                                        uri1
                                        uri2
                                        title
                                        #js {:preview preview?
                                                                           :preserveFocus preview?})))))

(defn get-commit-changes!+ [repo commit]
  (when (and repo commit)
    (let [hash (.-hash commit)
          parents (.-parents commit)]
      (if (empty? parents)
        (.diffWith repo hash)
        (let [parent-hash (first parents)]
          (.diffBetween repo parent-hash hash))))))

(defn show-git-history-search!+ []
  (p/let [repo (get-current-repository!+)
          _ (when-not repo
              (throw (js/Error. "No Git repository found in the current workspace")))
          commits (get-commit-history!+ repo)
          all-items-promises (map (fn [commit]
                                    (p/let [changes (get-commit-changes!+ repo commit)]
                                      (map #(format-file-for-quickpick commit %) changes)))
                                  commits)
          all-items-nested (p/all all-items-promises)
          all-items (apply concat all-items-nested)
          quick-pick (vscode/window.createQuickPick)]

    (set! (.-items quick-pick) (into-array all-items))
    (set! (.-title quick-pick) "Git History Search")
    (set! (.-placeHolder quick-pick) "Search commit messages, files, authors, or hashes")
    (set! (.-matchOnDescription quick-pick) true)
    (set! (.-matchOnDetail quick-pick) true)

    (.onDidChangeActive quick-pick (fn [active-items]
                                     (let [first-item (first active-items)]
                                       (when (and first-item (.-fileChange first-item))
                                         (show-file-diff!+ (.-commit first-item) (.-fileChange first-item) true)))))
    (.onDidAccept quick-pick
                  (fn [_e]
                    (p/let [selected-item (first (.-selectedItems quick-pick))
                            commit (.-commit selected-item)
                            file-change (.-fileChange selected-item)]
                      (when (and commit file-change)
                        (show-file-diff!+ commit file-change false))
                      (.dispose quick-pick))))
    (.onDidHide quick-pick
                (fn [_e]
                  (.dispose quick-pick)))
    (.show quick-pick)))

(defn ^:export show-git-history!+ []
  (p/catch
   (show-git-history-search!+)
   (fn [err]
     (vscode/window.showErrorMessage (str "Error: " err)))))
