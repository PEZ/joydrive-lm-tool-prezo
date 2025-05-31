(ns tts-fiddle
  (:require [promesa.core :as p]
            ["ai-text-to-speech" :as tts]
            ["fs" :as fs]))

;; Install: npm install ai-text-to-speech
;; Docs: #fetch https://www.npmjs.com/package/ai-text-to-speech

;; Requires: OPENAI_API_KEY environment variable

(comment
  ;; Step 1: Get the TTS function
  (def ai-speech (.-default tts))

  (def audio-dir "/Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/audio")

  ;; Step 2: Ensure audio directory exists
  (when-not (fs/existsSync audio-dir)
    (fs/mkdirSync audio-dir #js {:recursive true}))

  ;; Step 3: Simple TTS call with just :input and :dest_dir
  (p/let [file-path (ai-speech #js {:input "Hello from Joyride TTS fiddle!"
                                    :dest_dir audio-dir})]
    (def file-path file-path))
  ;;     ^^^^^^^^^ evaluate `file-path` to check the result

  :rcf)
