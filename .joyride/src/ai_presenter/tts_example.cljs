(ns ai-presenter.tts-example
  "Joyride ClojureScript example demonstrating correct TTS API usage.
   This converts the working Node.js test to show how to properly handle
   the ai-text-to-speech module that returns file paths instead of binary data."
  (:require ["vscode" :as vscode]
            ["fs" :as fs]
            [promesa.core :as p]))

;; Example configuration
(def example-config
  {:voice "nova"
   :model "tts-1"
   :response-format "mp3"
   :voice-dir "slides/voice"})

(defn ensure-voice-dir!
  "Ensure the voice directory exists, creating it if necessary."
  [voice-dir-rel]
  (let [ws-root (-> vscode/workspace.workspaceFolders first .-uri)
        voice-dir (vscode/Uri.joinPath ws-root voice-dir-rel)]
    (p/catch
      (vscode/workspace.fs.stat voice-dir)
      (fn [_error]
        ;; Directory doesn't exist, create it
        (vscode/workspace.fs.createDirectory voice-dir)))))

(defn generate-audio-example!
  "Complete example of TTS generation with proper file handling.
   Returns a promise that resolves to the final audio file path."
  [{:keys [text voice model response-format voice-dir filename]
    :or {voice "nova"
         model "tts-1"
         response-format "mp3"
         voice-dir "slides/voice"
         filename "joyride-tts-example.mp3"}}]

  (p/let [;; Ensure voice directory exists
          _ (ensure-voice-dir! voice-dir)

          ;; Get workspace root and construct target path
          ws-root (-> vscode/workspace.workspaceFolders first .-uri)
          target-uri (vscode/Uri.joinPath ws-root voice-dir filename)
          target-path (.-fsPath target-uri)

          ;; Dynamic import of the TTS module (ES module)
          tts-module (js/import "ai-text-to-speech")
          ai-speech (.-default tts-module)

          ;; Call TTS API - this returns a file path, not binary data
          tts-result-path (ai-speech #js {:input text
                                         :voice voice
                                         :model model
                                         :response_format response-format})

          ;; Check the original file exists and get stats
          _ (when-not (.existsSync fs tts-result-path)
              (throw (js/Error. (str "TTS result file not found: " tts-result-path))))

          original-stats (.statSync fs tts-result-path)

          ;; Copy the file to our target location
          _ (.copyFileSync fs tts-result-path target-path)

          ;; Verify the copy
          target-stats (.statSync fs target-path)

          ;; Clean up the original temporary file
          _ (.unlinkSync fs tts-result-path)]

    ;; Return info about the operation
    {:success true
     :original-file tts-result-path
     :target-file target-path
     :original-size (.-size original-stats)
     :target-size (.-size target-stats)
     :message (str "Generated " (.-size target-stats) " byte MP3 file: " filename)}))

(defn run-tts-example!
  "Run a complete TTS example with user feedback."
  []
  (p/let [result (generate-audio-example!
                   {:text "Hello from Joyride! This is a demonstration of the corrected TTS integration that properly handles file paths instead of binary data."
                    :filename "hello-joyride-corrected.mp3"})]

    ;; Show success message
    (vscode/window.showInformationMessage
      (str "✅ TTS Example Complete!\n" (:message result)))

    ;; Return the result for REPL inspection
    result))

(defn test-api-key
  "Test if OpenAI API key is available."
  []
  (let [api-key (.. js/process -env -OPENAI_API_KEY)]
    (if api-key
      {:api-key-available true
       :message "✅ OpenAI API key is configured"}
      {:api-key-available false
       :message "❌ OpenAI API key not found in environment"})))

(comment
  ;; Quick API key check
  (test-api-key)

  ;; Run the full TTS example
  (run-tts-example!)

  ;; Custom example with different text
  (generate-audio-example!
    {:text "This is a custom message for testing the AI presenter system."
     :filename "custom-test.mp3"})

  ;; Test just the voice directory creation
  (ensure-voice-dir! "slides/voice")

  :rcf)
