(ns fuzzy
  (:require ["vscode" :as vscode]
            [clojure.string :as string]
            [joyride.core :as joyride]
            [promesa.core :as p]))

;; “Install” by adding something like this to your keybindings.json
  ;; {
  ;;   "key": "ctrl+alt+f",
  ;;   "command": "joyride.runCode",
  ;;   "args": "(require 'fuzzy :reload) (fuzzy/show-search-box! 3000)"
  ;; },

(defn- get-configured-exclude-patterns! []
  (->> ["search.exclude" "files.exclude"]
       (mapcat (fn [config-key]
                 (-> (.get (vscode/workspace.getConfiguration) config-key)
                     (js/Object.entries)
                     (.filter (fn [[_k v]] v))
                     (.map (fn [[k _v]] k)))))
       set))

(defn- make-exclude-pattern! []
  (str "{" (string/join "," (get-configured-exclude-patterns!)) "}"))

(defn- find-files!+ []
  (p/let [excludes (make-exclude-pattern!)]
    (vscode/workspace.findFiles "**/*" excludes)))

(defn- uri->line-items!+ [max-file-loc uri]
  (p/let [data (vscode/workspace.fs.readFile uri)
          relative-path (vscode/workspace.asRelativePath uri)]
    (if (.includes data 0) ; Excludes binary files
      []
      (let [lines (-> data (.toString "utf8") (.split "\n"))]
        (if (and max-file-loc
                 (> (count lines) max-file-loc))
          (do
            (.appendLine (joyride/output-channel) (str "Excluding: " relative-path " (too many lines:" (count lines) ")"))
            [])
          (keep-indexed (fn [idx line]
                          (when-not (string/blank? line)
                            #js {:label (str (.trim line) " - " relative-path)
                                 :detail (str relative-path ", Line: " (inc idx))
                                 :uri uri
                                 :range (vscode/Range. idx (.search line #"\S") idx (count line))}))
                        lines))))))

(defn- uris->line-items!+ [max-file-loc uris]
  (p/let [line-items (p/all (map (partial uri->line-items!+ max-file-loc) uris))]
    (apply concat line-items)))

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

(defn show-search-box! [max-file-loc]
  (p/let [uris (find-files!+)
          all-items (uris->line-items!+ max-file-loc uris)
          loc-item #js {:label "$(list-ordered)"
                        :description (str "Workspace LOC: " (count all-items))}
          quick-pick (vscode/window.createQuickPick)]
    (set! (.-items quick-pick) (into-array (concat [loc-item] all-items)))
    (set! (.-title quick-pick) "Fuzzy file search")
    (set! (.-placeHolder quick-pick) "Use `foo*bar` to match `foo<whatever>bar`")
    (set! (.-matchOnDetail quick-pick) true)
    (doto quick-pick
      (.onDidChangeActive (fn [active-items]
                            (highlight-item! (first active-items) true)))
      (.onDidAccept (fn [_e]
                      (highlight-item! (first (.-selectedItems quick-pick)) false)
                      (.dispose quick-pick)))
      (.onDidHide (fn [_e]
                    (.dispose quick-pick)
                    (when-let [editor @!decorated-editor]
                      (clear-decorations! editor))))
      (.show))))

(when (= (joyride/invoked-script) joyride/*file*)
  (show-search-box! nil))