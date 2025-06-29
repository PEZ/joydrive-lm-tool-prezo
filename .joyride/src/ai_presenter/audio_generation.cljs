(ns ai-presenter.audio-generation
  (:require
   ["ai-text-to-speech" :as tts]
   ["vscode" :as vscode]
   [ai-presenter.audio-playback :as audio-playback]
   [promesa.core :as p]))

;; =============================================================================
;; Audio Generation System
;; =============================================================================

;; Environment validation
(defn validate-environment
  "Validates that required environment variables are present"
  []
  (let [api-key (or js/process.env.OPENAI_API_KEY
                    (-> js/process .-env .-OPENAI_API_KEY))]
    {:api-key-present? (boolean api-key)
     :api-key-length (when api-key (count api-key))}))

;; Pure path utilities
(defn ws-root
  "Returns the workspace root URI"
  []
  (if (not= js/undefined vscode/workspace.workspaceFolders)
    (.-uri (first vscode/workspace.workspaceFolders))
    (vscode/Uri.parse ".")))

(defn voice-dir-path
  "Returns the slides/voice directory URI"
  []
  (let [root (ws-root)]
    (vscode/Uri.joinPath root "slides" "voice")))

(defn target-file-path
  "Pure function to determine target file path for a slide"
  [slide-name]
  (let [filename (str slide-name ".mp3")
        voice-dir (voice-dir-path)]
    (vscode/Uri.joinPath voice-dir filename)))

;; Side-effect utilities
(defn ensure-voice-dir!+
  "Ensures the voice directory exists, returns promise of directory URI"
  []
  (p/let [voice-dir (voice-dir-path)]
    (p/catch
     (vscode/workspace.fs.createDirectory voice-dir)
     (fn [error]
       ;; Directory might already exist, that's fine
       (when-not (= "FileExists" (.-code error))
         (throw error))))
    voice-dir))

(defn move-file!+
  "Moves file from source path to target URI"
  [source-path target-uri]
  (p/let [source-uri (vscode/Uri.file source-path)]
    (vscode/workspace.fs.copy source-uri target-uri #js {:overwrite true})))

;; Core TTS functionality
(def ai-speech (.-default tts))

(def audio-dir "/Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/audio")

(defn generate-slide-audio!+
  "Generates audio file for a slide from script text.
   Returns a promise that resolves to {:success true ...} or {:success false :error ...}"
  [slide-name script-text]
  (p/catch
   (p/let [;; Validate inputs
           _ (when (or (empty? slide-name) (not (string? slide-name)))
               (throw (js/Error. "slide-name must be a non-empty string")))
           _ (when (or (empty? script-text) (not (string? script-text)))
               (throw (js/Error. "script-text must be a non-empty string")))

           ;; Validate environment
           env-check (validate-environment)
           _ (when-not (:api-key-present? env-check)
               (throw (js/Error. "OPENAI_API_KEY not found in environment")))

           ;; Ensure voice directory exists
           _ (ensure-voice-dir!+)

           ;; Generate audio to temp location
           temp-file-path (ai-speech #js {:input script-text
                                          :dest_dir audio-dir
                                          :voice "nova"
                                          :model "tts-1-hd"
                                          :response_format "mp3"})

           ;; Calculate target path
           target-uri (target-file-path slide-name)

           ;; Move file to target location
           _ (move-file!+ temp-file-path target-uri)

           ;; Clean up temp file
           temp-uri (vscode/Uri.file temp-file-path)
           _ (vscode/workspace.fs.delete temp-uri)

           ;; Verify the target file exists
           file-stat (vscode/workspace.fs.stat target-uri)]

     {:success true
      :slide-name slide-name
      :target-path (str target-uri)
      :file-size (.-size file-stat)
      :script-length (count script-text)})

   (fn [error]
     {:success false
      :error (.-message error)
      :slide-name slide-name})))

;; Let's test the corrected generate-and-play-message!+ function
(defn generate-and-play-message!+ [text]
  (p/let [ws-root (ws-root)  ;; Remove namespace prefix - we're already in this namespace
          temp-dir-uri (vscode/Uri.joinPath ws-root ".joyride" "temp-audio")

          _ (p/catch
             (vscode/workspace.fs.createDirectory temp-dir-uri)
             (fn [error]
               (when-not (= "FileExists" (.-code error))
                 (throw error))))

          timestamp (js/Date.now)
          temp-filename (str "repl-audio-" timestamp ".mp3")

          env-check (validate-environment)  ;; Remove namespace prefix
          _ (when-not (:api-key-present? env-check)
              (throw (js/Error. "OPENAI_API_KEY not found in environment")))

          temp-file-path (ai-speech  ;; Remove namespace prefix
                          #js {:input text
                               :dest_dir audio-dir  ;; Remove namespace prefix
                               :voice "nova"
                               :model "tts-1-hd"
                               :response_format "mp3"})

          target-uri (vscode/Uri.joinPath temp-dir-uri temp-filename)
          _ (move-file!+ temp-file-path target-uri)  ;; Remove namespace prefix

          temp-uri (vscode/Uri.file temp-file-path)
          _ (vscode/workspace.fs.delete temp-uri)

          file-stat (vscode/workspace.fs.stat target-uri)]

    (p/let [relative-path (str ".joyride/temp-audio/" temp-filename)
            play-result (audio-playback/load-and-play-audio!+ relative-path)]
      {:success true
       :text text
       :temp-filename temp-filename
       :workspace-temp-path relative-path
       :file-size (.-size file-stat)
       :play-result play-result})))

(comment

  ;; Generate audio for a slide
  (generate-slide-audio!+ "demo-slide" "Welcome to the Joyride audio generation system! This is a demonstration of how we can create perfect audio files for slide presentations.")

  ;; Play generated audio (relative path)
  (ai-presenter.audio-playback/load-and-play-audio!+ ".joyride/temp-audio/demo-slide.mp3")

  ;; Generate and play audio in one step
  (generate-and-play-message!+ "This is a direct HD voice playback example.")

  (generate-slide-audio!+ "welcome" "Welcome, everyone.

    [Pause]

    I want to ask you something. How many times have you thought: \"I wish VS Code could just do this one thing differently\"?

    [Pause]

    Or \"If only there was an extension that did exactly what I need\"?

    [Pause]

    The problem isn't VS Code. It's not the extensions either. The problem is that no one else knows your workflow like you do. No one else feels that tiny friction you feel every day.

    [Pause]

    What if I told you that you don't have to wait for someone else to build what you need?

    [Pause]

    What if you could just... make it happen?

    [Longer pause]

    That's what we're here to explore. This is an invitation to join the ranks of VS Code hackers. People who don't adapt to our tools—we make our **tools** adapt—to us.")
  :rcf)
