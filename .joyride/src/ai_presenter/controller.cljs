(ns ai-presenter.controller
  (:require [ai-presenter.integration :as integration]
            [ai-presenter.audio :as audio]
            [promesa.core :as p]
            ["vscode" :as vscode]))

;; =============================================================================
;; AI Presenter Controller - Main interface for AI-powered slide presentation
;; =============================================================================

;; Global state reference
(defonce !presenter-state (atom (integration/create-unified-state [])))

(defn activate!
  "Activate AI presenter with slides configuration"
  ([slides-config]
   (activate! slides-config (audio/default-voice-config)))
  ([slides-config voice-config]
   (let [initial-state (integration/create-unified-state slides-config)
         new-state (-> (integration/activate-unified initial-state)
                       (assoc :ai-presenter/voice-config voice-config))]
     (reset! !presenter-state new-state)
     (vscode/window.showInformationMessage "🎤 AI Presenter activated! Use start-presentation to begin.")
     new-state)))

(defn deactivate!
  "Deactivate AI presenter"
  []
  (let [new-state (integration/deactivate-unified @!presenter-state)]
    (reset! !presenter-state new-state)
    (vscode/window.showInformationMessage "🎤 AI Presenter deactivated.")
    new-state))

(defn start-presentation!
  "Start AI-powered presentation"
  []
  (if (not= "active" (:status @!presenter-state))
    (do
      (vscode/window.showErrorMessage "AI Presenter must be activated first!")
      @!presenter-state)
    (p/let [slides (:slides @!presenter-state)
            first-slide (first slides)
            voice-config (:voice-config @!presenter-state)
            _ (when first-slide
                (vscode/window.showInformationMessage
                  (str "🎵 Generating narration for slide: " first-slide)))
            audio-path (when first-slide
                         (audio/generate-audio+
                           (str "Welcome to the presentation. Starting with slide: " first-slide)
                           voice-config))
            new-state (integration/start-presenting-unified @!presenter-state)]
      (reset! !presenter-state new-state)
      (when audio-path
        (vscode/window.showInformationMessage
          (str "🎤 AI Presentation started! Audio: " audio-path)))
      new-state)))

(defn next-slide!
  "Move to next slide with AI narration"
  []
  (if (= "presenting" (:status @!presenter-state))
    (p/let [slides (:slides @!presenter-state)
            next-state (integration/next-slide-unified @!presenter-state)
            next-idx (:slide-index next-state)]
      (reset! !presenter-state next-state)

      (when (< next-idx (count slides))
        (let [slide (nth slides next-idx)
              voice-config (:voice-config @!presenter-state)]
          (p/let [audio-path (audio/generate-audio+
                               (str "Now presenting slide: " slide)
                               voice-config)]
            (vscode/window.showInformationMessage
              (str "🎵 Slide " (inc next-idx) "/" (count slides)
                   " - Audio: " audio-path)))))
      next-state)
    (do
      (vscode/window.showWarningMessage "AI Presenter is not currently presenting!")
      @!presenter-state)))

(defn pause-presentation!
  "Pause AI presentation"
  []
  (let [new-state (integration/pause-presenting-unified @!presenter-state)]
    (reset! !presenter-state new-state)
    (vscode/window.showInformationMessage "⏸️ AI Presentation paused.")
    new-state))

(defn resume-presentation!
  "Resume AI presentation"
  []
  (let [new-state (integration/resume-presenting-unified @!presenter-state)]
    (reset! !presenter-state new-state)
    (vscode/window.showInformationMessage "▶️ AI Presentation resumed.")
    new-state))

(defn stop-presentation!
  "Stop AI presentation"
  []
  (let [new-state (integration/deactivate-unified @!presenter-state)]
    (reset! !presenter-state new-state)
    (vscode/window.showInformationMessage "⏹️ AI Presentation stopped.")
    new-state))

(defn get-state
  "Get current presenter state"
  []
  @!presenter-state)

(defn get-status
  "Get current presentation status"
  []
  (:status @!presenter-state))

(comment
  ;; Demo usage:
  (activate! ["slide1.md" "slide2.md" "slide3.md"])
  (start-presentation!)
  (next-slide!)
  (pause-presentation!)
  (resume-presentation!)
  (stop-presentation!)
  (deactivate!)
  :rcf)
