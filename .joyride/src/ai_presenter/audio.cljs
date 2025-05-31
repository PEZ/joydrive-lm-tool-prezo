(ns ai-presenter.audio
  (:require [promesa.core :as p]
            ["ai-text-to-speech" :as tts]))

;; Audio generation functions
;; Rebuilt using TDD approach based on plan spike results

;; Constants for validation (from the plan spike results)
(def ^:private allowed-voices ["alloy" "echo" "fable" "onyx" "nova" "shimmer"])
(def ^:private allowed-models ["tts-1" "tts-1-hd"])
(def ^:private allowed-formats ["mp3" "opus" "aac" "flac" "wav" "pcm"])

;; Based on the spike results: (def ai-speech (.-default tts))
(def ai-speech (.-default tts))

(defn available-voices
  "Get list of available TTS voices"
  []
  ["alloy" "echo" "fable" "onyx" "nova" "shimmer"])

(defn default-voice-config
  "Get default voice configuration"
  []
  {:voice "nova"
   :model "tts-1"
   :format "mp3"})

(defn valid-voice-config?
  "Validate voice configuration"
  [{:keys [voice model format]}]
  (boolean (and (some #{voice} allowed-voices)
                (some #{model} allowed-models)
                (some #{format} allowed-formats))))

(defn valid-text?
  "Validate text input for TTS"
  [text]
  (boolean (and (string? text)
                (seq text)
                (<= (count text) 4000))))

(defn api-key-available?
  "Check if OpenAI API key is available"
  []
  (boolean (.-OPENAI_API_KEY js/process.env)))

(defn generate-audio+
  "Generate audio from text using OpenAI TTS
   Returns a promise that resolves to the file path of the generated audio"
  [text config]
  (cond
    (not (valid-text? text))
    (p/rejected (js/Error. "Cannot generate audio from empty text"))

    (not (valid-voice-config? config))
    (p/rejected (js/Error. "Invalid voice configuration"))

    :else
    ;; From spike: (p/let [file-path (ai-speech #js {:input "text" :dest_dir dir})])
    (let [tts-config #js {:input text
                          :voice (:voice config)
                          :response_format (:format config)}]
      (ai-speech tts-config))))
