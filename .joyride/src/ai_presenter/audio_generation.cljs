(ns ai-presenter.audio-generation
  (:require [promesa.core :as p]
            ["ai-text-to-speech" :as tts]
            ["vscode" :as vscode]))

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
                                           :model "tts-1"
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

(comment
  ;; =============================================================================
  ;; REPL-Driven Development & Testing
  ;; =============================================================================
  
  ;; Test environment validation
  (validate-environment)
  ;; => {:api-key-present? true, :api-key-length 51}
  
  ;; Test path utilities
  {:workspace-root (str (ws-root))
   :voice-dir (str (voice-dir-path))
   :target-path (str (target-file-path "hello"))}
  ;; => {:workspace-root "file:///Users/pez/Projects/Meetup/joydrive-lm-tool-prezo"
  ;;     :voice-dir "file:///Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/slides/voice"
  ;;     :target-path "file:///Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/slides/voice/hello.mp3"}
  
  ;; Test directory creation
  (p/then (ensure-voice-dir!+)
          (fn [result] {:success true :voice-dir (str result)}))
  
  ;; Test complete audio generation
  (def !test-result (atom nil))
  
  (p/then
    (generate-slide-audio!+ "demo-slide" 
                            "Welcome to the Joyride audio generation system! This is a demonstration of how we can create perfect audio files for slide presentations.")
    (fn [result]
      (reset! !test-result result)
      result))
  
  ;; Check result
  @!test-result
  ;; => {:success true
  ;;     :slide-name "demo-slide"
  ;;     :target-path "file:///Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/slides/voice/demo-slide.mp3"
  ;;     :file-size 165888
  ;;     :script-length 145}
  
  ;; Test error handling
  (p/then
    (generate-slide-audio!+ "" "This should fail")
    (fn [result] result))
  ;; => {:success false, :error "slide-name must be a non-empty string", :slide-name ""}
  
  ;; Test with invalid script
  (p/then
    (generate-slide-audio!+ "test" "")
    (fn [result] result))
  ;; => {:success false, :error "script-text must be a non-empty string", :slide-name "test"}
  
  ;; Generate audio for actual slide content
  (p/then
    (generate-slide-audio!+ "joyride-intro"
                            "Welcome to Joyride, the amazing tool that lets you hack VS Code with Clojure! In this presentation, we'll explore how Joyride combines the power of Interactive Programming with VS Code's extension API to create a unique development experience.")
    (fn [result] result))
  
  :rcf)
