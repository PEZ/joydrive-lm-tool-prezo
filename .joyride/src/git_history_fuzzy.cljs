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

;; Format commit files for display in QuickPick
(defn format-file-for-quickpick [commit file-change]
  (let [hash (.-hash commit)
        short-hash (subs hash 0 7)
        message (.-message commit)
        author-name (.-authorName commit)
        commit-date (.-commitDate commit)
        formatted-date (when commit-date
                         (.toLocaleDateString commit-date))
        file-uri (.-uri file-change)
        file-path (vscode/workspace.asRelativePath file-uri)
        status (.-status file-change)]
    #js {:label (str "$(git-commit) " message)
         :description (str short-hash " - " file-path " - " author-name " - " formatted-date)
         :detail (str "Status: " status)
         :commit commit
         :fileChange file-change
         :hash hash}))

;; Show diff for a specific file
(defn show-file-diff!+ [repo commit file-change preview?]
  (p/let [git-api (get-git-api!+)]
    (when (and git-api commit file-change)
      (let [hash (.-hash commit)
            parents (.-parents commit)
            uri (.-uri file-change)
            file-path (vscode/workspace.asRelativePath uri)]
        (def git-api git-api)
        (def hash hash)
        (def parents parents)
        (def uri uri)
        (def file-path file-path)
        (if (empty? parents)
          ;; For initial commit, compare with empty tree
          (let [uri1 (.toGitUri git-api uri "HEAD^{}")  ;; Empty tree
                uri2 (.toGitUri git-api uri hash)
                title (str "Diff: " file-path " (Initial commit " (subs hash 0 7) ")")]
            (vscode/commands.executeCommand "vscode.diff" uri1 uri2 title #js {:preview preview?
                                                                               :preserveFocus preview?}))
          ;; For other commits, compare with first parent
          (let [parent-hash (first parents)
                _ (def parent-hash parent-hash)
                uri1 (.toGitUri git-api uri parent-hash)
                uri2 (.toGitUri git-api uri hash)
                title (str "Diff: " file-path " (" (subs parent-hash 0 7) " → " (subs hash 0 7) ")")]
            (def uri1 uri1)
            (def uri2 uri2)
            (def title title)
            (def preview? preview?)
            (vscode/commands.executeCommand "vscode.diff"
                                            (.toGitUri git-api uri "HEAD^{}")
                                            uri2 title #js {:preview preview?
                                                                               :preserveFocus preview?})
            (vscode/commands.executeCommand "vscode.diff" uri1 uri2 title #js {:preview preview?
                                                                               :preserveFocus preview?})))))))

;; Show diff for a specific file in a preview mode (when navigating)
(defn preview-file-diff!+ [repo commit file-change]
  (show-file-diff!+ repo commit file-change true))

;; Get all the files changed in a commit
(defn get-commit-changes!+ [repo commit]
  (when (and repo commit)
    (let [hash (.-hash commit)
          parents (.-parents commit)]
      (if (empty? parents)
        ;; For initial commit, we need to compare with empty tree
        (p/let [changes (.diffWith repo hash)]
          changes)
        ;; For other commits, compare with first parent
        (p/let [parent-hash (first parents)
                changes (.diffBetween repo parent-hash hash)]
          changes)))))

;; Create the QuickPick UI for git history with files
(defn show-git-history-search!+ []
  (p/let [repo (get-current-repository!+)
          _ (when-not repo
              (throw (js/Error. "No Git repository found in the current workspace")))
          commits (get-commit-history!+ repo)
          ;; For each commit, get its changes and create items for each file change
          all-items-promises (map (fn [commit]
                                   (p/let [changes (get-commit-changes!+ repo commit)]
                                     (if (seq changes)
                                       (map #(format-file-for-quickpick commit %) changes)
                                       [(format-commit-for-quickpick commit)])))
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
                                         (preview-file-diff!+ repo (.-commit first-item) (.-fileChange first-item))))))

    (.onDidAccept quick-pick
                  (fn [_e]
                    (p/let [selected-item (first (.-selectedItems quick-pick))
                            commit (.-commit selected-item)
                            file-change (.-fileChange selected-item)]
                      ;; Open the diff in a non-preview editor
                      (when (and commit file-change)
                        (show-file-diff!+ repo commit file-change false))
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