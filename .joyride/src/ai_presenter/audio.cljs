(ns ai-presenter.audio
  (:require [promesa.core :as p]
            ["ai-text-to-speech" :as tts]
            [joyride.core :as joyride]
            ["vscode" :as vscode]
            ["path" :as path]))

;; Audio generation and playback functions
;; This namespace provides the bridge to npm text-to-speech modules
;; Following the AI Presenter Plan: audio lifecycle management with session control

;; Audio Registry (Separate from main state)
(defonce !audio-registry
  (atom {:active-audio-id nil
         :audio-sessions {}  ; id → {:file-path, :status, :element}
         :available-files #{}}))

(comment
  (boolean (.-OPENAI_API_KEY js/process.env))
  ;;=> true
  (def config #js {:input "Hello!"
                   :voice "alloy"
                   :response_format "mp3"})
  (joyride/js-properties tts)
  ;;=> ("ALLOWED_FORMATS" "ALLOWED_MODELS" "ALLOWED_SUFFIX_TYPES" "ALLOWED_VOICES" "__esModule" "default")
  (.-ALLOWED_VOICES tts)
  (def aiSpeech (.-default tts))
  ;;=> #js ["alloy" "echo" "fable" "onyx" "nova" "shimmer"]

  (p/let [file-path (aiSpeech config)]
    (println file-path)
    (def file-path file-path))
  )

;; Constants from the npm module
(def ^:private allowed-voices ["alloy" "echo" "fable" "onyx" "nova" "shimmer"])
(def ^:private allowed-models ["tts-1" "tts-1-hd"])
(def ^:private allowed-formats ["mp3" "opus" "aac" "flac" "wav" "pcm"])

(defn available-voices
  "Get list of available TTS voices"
  []
  allowed-voices)

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
  (and (string? text)
       (seq text)
       (<= (count text) 4096)))

(defn- get-voice-directory
  "Get the slides/voice directory path"
  []
  (str "/Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/slides/voice"))

(defn- ensure-voice-directory!
  "Ensure slides/voice directory exists"
  []
  (let [voice-dir (get-voice-directory)
        fs (js/require "fs")]
    (when-not (.existsSync fs voice-dir)
      (.mkdirSync fs voice-dir #js {:recursive true}))
    voice-dir))

(defn- slide-name->audio-path
  "Convert slide name to audio file path following plan convention"
  [slide-name]
  (let [voice-dir (get-voice-directory)
        ;; Remove .md extension if present, add .mp3
        base-name (if (.endsWith slide-name ".md")
                    (.slice slide-name 0 -3)
                    slide-name)]
    (str voice-dir "/" base-name ".mp3")))

(defn api-key-available?
  "Check if OpenAI API key is available"
  []
  (boolean (or (.-OPENAI_API_KEY js/process.env)
               ;; Could add other sources here
               false)))

(defn prepare-slide-audio!
  "Generate audio for a specific slide (FR-2.1, FR-2.2)
   slide-name: 'hello.md' or 'hello' 
   script-text: The text to convert to speech
   Returns promise resolving to {:success true :file-path path} or {:success false :error msg}"
  [slide-name script-text]
  (p/create
    (fn [resolve _reject]
      (try
        (ensure-voice-directory!)
        (let [audio-path (slide-name->audio-path slide-name)
              voice-config (default-voice-config)
              tts-options #js {:input script-text
                               :voice (:voice voice-config)
                               :model (:model voice-config)
                               :response_format (:format voice-config)}]
          (if (not (valid-text? script-text))
            (resolve {:success false :error "Invalid script text"})
            (-> (let [tts-fn (.-default tts)]
                  (tts-fn tts-options))
                (.then (fn [audio-data]
                         (let [fs (js/require "fs")]
                           (.writeFileSync fs audio-path audio-data)
                           (js/console.log (str "🎵 Generated slide audio: " slide-name " -> " audio-path))
                           (resolve {:success true :file-path audio-path}))))
                (.catch (fn [error]
                          (resolve {:success false :error (.-message error)}))))))
        (catch js/Error e
          (resolve {:success false :error (.-message e)}))))))

(defn- generate-real-audio+
  "Generate real audio using OpenAI TTS API"
  [text {:keys [voice model format] :or {voice "nova" model "tts-1" format "mp3"}}]
  (p/create
    (fn [resolve reject]
      (let [timestamp (js/Date.now)
            filename (str "joyride-" voice "-" timestamp "." format)
            output-path (str "/tmp/" filename)
            tts-options #js {:input text
                             :voice voice
                             :model model
                             :response_format format}]
        (-> (js/Promise.resolve)
            (.then #(let [tts-fn (.-default tts)]
                      (tts-fn tts-options)))
            (.then (fn [audio-data]
                     ;; Write the audio data to file
                     (let [fs (js/require "fs")]
                       (.writeFileSync fs output-path audio-data)
                       (js/console.log (str "🎵 Real TTS generated: \"" text "\" -> " output-path))
                       output-path)))
            (.then resolve)
            (.catch reject))))))

(defn- generate-mock-audio+
  "Generate mock audio file for testing without API key"
  [text {:keys [voice format] :or {voice "nova" format "mp3"}}]
  (p/create
    (fn [resolve _reject]
      ;; Simulate TTS generation delay
      (js/setTimeout
        (fn []
          (let [mock-path (str "/tmp/mock-" voice "-" (hash text) "." format)]
            (js/console.log (str "🎵 Mock TTS generated: \"" text "\" -> " mock-path))
            (resolve mock-path)))
        100))))

(defn generate-audio+
  "Generate audio from text using specified voice configuration
   Returns a promise that resolves to audio file path"
  [text voice-config]
  (if (not (valid-text? text))
    (p/rejected (js/Error. (str "Invalid text input: "
                                (cond
                                  (not (string? text)) "must be a string"
                                  (not (seq text)) "empty text not allowed"
                                  (> (count text) 4096) "text too long (max 4096 chars)"
                                  :else "unknown error"))))
    (if (not (valid-voice-config? voice-config))
      (p/rejected (js/Error. (str "Invalid voice configuration: " voice-config)))
      ;; Check if API key is available for real TTS
      (if (api-key-available?)
        (generate-real-audio+ text voice-config)
        (generate-mock-audio+ text voice-config)))))

(defn play-audio+
  "Play audio file using system default player
   Returns a promise that resolves when playback starts"
  [audio-path]
  (p/create
    (fn [resolve reject]
      (try
        ;; Use VS Code's external opener which delegates to system default app
        (-> (vscode/env.openExternal (vscode/Uri.file audio-path))
            (.then #(do
                      (js/console.log (str "🎵 Playing audio: " audio-path))
                      (resolve audio-path)))
            (.catch reject))
        (catch js/Error e
          (reject e))))))

;; ===== AUDIO SESSION MANAGEMENT (Plan Implementation) =====

(defn generate-audio-id 
  "Generate unique audio session ID"
  []
  (str "audio-" (js/Date.now) "-" (rand-int 10000)))

(defn register-audio 
  "Pure function: register audio session in registry
   Returns new registry state"
  [registry audio-id file-path]
  (-> registry
      (assoc-in [:audio-sessions audio-id] {:file-path file-path
                                            :status :ready
                                            :element nil})
      (update :available-files conj file-path)))

(defn get-audio-session
  "Get audio session by ID from registry"
  [registry audio-id]
  (get-in registry [:audio-sessions audio-id]))

(defn update-audio-session-status
  "Pure function: update audio session status
   Returns new registry state"
  [registry audio-id new-status]
  (assoc-in registry [:audio-sessions audio-id :status] new-status))

(defn stop-all-audio-sessions
  "Pure function: mark all audio sessions as stopped
   Returns new registry state with cleanup commands"
  [registry]
  (let [active-sessions (vals (:audio-sessions registry))
        cleanup-commands (map #(select-keys % [:file-path :element]) active-sessions)]
    {:registry (assoc registry 
                      :active-audio-id nil
                      :audio-sessions {})
     :cleanup-commands cleanup-commands}))

(defn set-active-audio
  "Pure function: set the active audio session
   Returns new registry state"
  [registry audio-id]
  (assoc registry :active-audio-id audio-id))

;; ===== AUDIO PLAYBACK CONTROL (Plan Implementation) =====

(defn play-audio!
  "Start audio playback (returns audio-id for control)
   Implements FR-2.3 audio session control"
  [file-path]
  (p/create
    (fn [resolve reject]
      (let [audio-id (generate-audio-id)]
        (try
          ;; Register the audio session
          (swap! !audio-registry register-audio audio-id file-path)
          (swap! !audio-registry set-active-audio audio-id)
          
          ;; Update status to playing
          (swap! !audio-registry update-audio-session-status audio-id :playing)
          
          ;; Use VS Code's external opener which delegates to system default app
          (-> (vscode/env.openExternal (vscode/Uri.file file-path))
              (.then #(do
                        (js/console.log (str "🎵 Playing audio [" audio-id "]: " file-path))
                        (resolve audio-id)))
              (.catch (fn [error]
                        ;; Update status to error on failure
                        (swap! !audio-registry update-audio-session-status audio-id :error)
                        (reject error))))
          (catch js/Error e
            (swap! !audio-registry update-audio-session-status audio-id :error)
            (reject e)))))))

(defn pause-audio!
  "Pause specific audio by id
   Note: System audio playback doesn't support pause, this updates status only"
  [audio-id]
  (let [session (get-audio-session @!audio-registry audio-id)]
    (if session
      (do
        (swap! !audio-registry update-audio-session-status audio-id :paused)
        (js/console.log (str "🎵 Audio paused [" audio-id "] (status only)"))
        true)
      (do
        (js/console.warn (str "🎵 Audio session not found: " audio-id))
        false))))

(defn resume-audio!
  "Resume specific audio by id
   Note: System audio playback requires re-playing the file"
  [audio-id]
  (let [session (get-audio-session @!audio-registry audio-id)]
    (if session
      (let [file-path (:file-path session)]
        (swap! !audio-registry update-audio-session-status audio-id :playing)
        (js/console.log (str "🎵 Audio resumed [" audio-id "]: " file-path))
        ;; For system playback, we'd need to re-open the file
        (vscode/env.openExternal (vscode/Uri.file file-path)))
      (do
        (js/console.warn (str "🎵 Audio session not found: " audio-id))
        false))))

(defn stop-audio!
  "Stop and cleanup specific audio
   Returns true if session existed, false otherwise"
  [audio-id]
  (let [session (get-audio-session @!audio-registry audio-id)]
    (if session
      (do
        ;; Remove the session from registry
        (swap! !audio-registry update :audio-sessions dissoc audio-id)
        ;; Clear active audio if this was the active one
        (when (= audio-id (:active-audio-id @!audio-registry))
          (swap! !audio-registry assoc :active-audio-id nil))
        (js/console.log (str "🎵 Audio stopped [" audio-id "]"))
        true)
      (do
        (js/console.warn (str "🎵 Audio session not found: " audio-id))
        false))))

(defn stop-all-audio!
  "Emergency stop all audio (chaos prevention!)
   Implements cleanup for mode switching"
  []
  (let [result (stop-all-audio-sessions @!audio-registry)
        cleanup-commands (:cleanup-commands result)]
    ;; Apply the new registry state
    (reset! !audio-registry (:registry result))
    ;; Log cleanup
    (js/console.log (str "🎵 Stopped all audio sessions (" (count cleanup-commands) " sessions)"))
    cleanup-commands))

(defn get-active-audio-id
  "Get currently active audio session ID"
  []
  (:active-audio-id @!audio-registry))

(defn get-audio-sessions
  "Get all current audio sessions"
  []
  (:audio-sessions @!audio-registry))

;; ===== EXISTING FUNCTIONS (Keep as-is) =====
