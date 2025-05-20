;; == Keyboard shortcuts ==
;; Weird indent because of how comment block/selection works
    ;;{
    ;;    "key": "ctrl+alt+j ctrl+n",
    ;;    "command": "joyride.runCode",
    ;;    "args": "(next-slide-notes/prepare!)"
    ;;},
    ;;{
    ;;    "key": "ctrl+alt+j shift+n",
    ;;    "command": "joyride.runCode",
    ;;    "args": "(next-slide-notes/edit-active-note!)"
    ;;},
    ;;{
    ;;    "key": "ctrl+alt+j alt+n",
    ;;    "command": "joyride.runCode",
    ;;    "args": "(next-slide-notes/print!)"
    ;;},
    ;;{
    ;;    "key": "ctrl+alt+j ctrl+alt+n",
    ;;    "command": "joyride.runCode",
    ;;    "args": "(next-slide-notes/toggle!)"
    ;;},

(ns next-slide-notes
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]
            [clojure.set :as set]
            [clojure.string :as string]
            [next-slide :as next]))

;; create this file with whatever YAML frontmatter you want
;; (make it empty if you don't need any pandoc/TeX prelude)
(def notes-header-path "slides/_notes-header.md")

;; Use a font that prints something instead of nothing for missing characters
(def main-font "DejaVu Sans")

;; Path to speaker notes output file
(def notes-pdf "speaker-notes.pdf")

(defn log [& s]
  (.appendLine (joyride/output-channel) (string/join " " s)))

(defn- slide-header+ [slide-path]
  (p/let [slide-uri (vscode/Uri.joinPath (next/ws-root) slide-path)
          slide-text (p/-> slide-uri
                           vscode/workspace.fs.readFile
                           js/Buffer.from
                           (.toString "utf-8"))
          header (re-find #"(?<=\n)#.*(?=\n)" slide-text)]
    header))

(defn- notes-list+ [slides]
  (p/let [notes (map (fn [slide]
                       (string/replace-first slide #"\.md$" "-notes.md"))
                     slides)]
    notes))

(defn- headers-list+ [slides]
  (p/let [headers (p/all (map slide-header+ slides))]
    headers))

(defn- notes-and-headers+ [headers notes]
  (p/let [notes-and-headers (mapv (fn [note header]
                                    [note header])
                                  notes headers)]
    notes-and-headers))

(defn- missing-notes-paths+ [notes-and-headers]
  (p/let [notes-paths (map first notes-and-headers)
          glob (str "{" (string/join "," notes-paths) "}")
          existing-uris (vscode/workspace.findFiles glob)
          existing-notes (p/all (map vscode/workspace.asRelativePath existing-uris))
          missing-notes (set/difference (set notes-paths) (set (js->clj existing-notes)))]
    missing-notes))

(defn create-file!+ [relative-path content]
  (p/let [file-uri (vscode/Uri.joinPath (next/ws-root) relative-path)
          encoder (js/TextEncoder.)
          content-data (.encode encoder content)]

    (vscode.workspace.fs.writeFile file-uri content-data)))

(defn create-missing-notes!+ [notes->headers missing-notes]
  (p/do (p/doseq [note missing-notes]
          (let [header (notes->headers note)
                content (str header "\n\n\\newpage")]
            (log "  Creating " note "-" header)
            (create-file!+ note content)))
        (log "")))

(defn gather-missing-notes+ []
  (p/let [slides (next/slides-list+)
          notes (notes-list+ slides)
          headers (headers-list+ slides)
          nhs (notes-and-headers+ headers notes)
          notes->headers (into {} nhs)
          missing (missing-notes-paths+ nhs)]
    {:slides slides
     :notes notes
     :headers headers
     :notes->headers notes->headers
     :missing missing}))

(defn prepare! []
  (-> (p/let [{:keys [notes->headers missing]} (gather-missing-notes+)]
        (log "Creating" (count missing) "missing -notes.md files")
        (create-missing-notes!+ notes->headers missing)
        (log "Done creating missing notes")
        (vscode/window.showInformationMessage (str "Done creating " (count missing) " missing notes")))
      (p/catch (fn [e]
                 (vscode/window.showErrorMessage (str "Error creating missing notes: " e))))))

(defn edit-active-note! []
  (p/let [{:keys [slides notes->headers missing]} 
          (gather-missing-notes+)
          active-document-uri (some-> vscode/window.activeTextEditor .-document .-uri)
          active-document-path (when active-document-uri
                                 (vscode/workspace.asRelativePath
                                  active-document-uri))
          active-slide (or ((set slides) active-document-path)
                           (nth slides (:active-slide @next/!state)))
          active-note (string/replace active-slide #"\.md$" "-notes.md")
          missing-active-note (keep (fn [note]
                                      (when (= note active-note)
                                        note))
                                    missing)
          active-note-uri (vscode.Uri.joinPath (next/ws-root) active-note)]
    (when (seq missing-active-note)
      (log "Creating missing:" active-note)
      (create-missing-notes!+ notes->headers missing-active-note))
    (vscode.workspace.openTextDocument active-note-uri)
    (vscode.window.showTextDocument active-note-uri)))

(comment
  (#{"a" "b"} "a")
  :rcf)

(defn print! []
  (p/let [slides (next/slides-list+)
          notes (notes-list+ slides)
          command-line (into ["pandoc" notes-header-path]
                             (concat notes ["-o" notes-pdf
                                            "--pdf-engine=xelatex"
                                            "-V geometry:'landscape,a4paper,margin=2cm'"
                                            "-V" (str "mainfont='" main-font "'")]))
          command-line-string (string/join " " command-line)
          _ (println command-line-string)
          button (vscode/window.showInformationMessage
                  (str "Notes printing command line") "Copy to clipboard")]
    (log (str "Notes printing command line:\n" command-line-string))
    (when (= "Copy to clipboard" button)
      (p/do (vscode/env.clipboard.writeText command-line-string)
            (vscode/window.showInformationMessage (str "Notes printing command line copied to clipboard!") "OK")))))

(defn toggle-path! [path]
  (-> (when (.endsWith path ".md")
        (p/let [uri (-> (if (.endsWith path "-notes.md")
                          (string/replace-first path #"-notes\.md$" ".md")
                          (string/replace-first path #"\.md$" "-notes.md"))
                        vscode/Uri.file)
                document (vscode/workspace.openTextDocument uri)]
          (vscode.window.showTextDocument document
                                          #js {:preserveFocus false})))
      (p/catch (fn [e]
                 (log "ERROR:" e)))))

(defn toggle! []
  (-> (when-let [path (some-> vscode/window.activeTextEditor .-document .-uri .-fsPath)]
        (toggle-path! path))
      (p/catch (fn [_e]))))

(comment
  (prepare!)
  (print!)
  (edit-active-note!)
  (toggle-path! "/Users/pez/Projects/workshops/funnel-nov-10-2023/slides/hello.md")
  (toggle-path! "/Users/pez/Projects/workshops/funnel-nov-10-2023/slides/hello-notes.md")
  :rcf)
