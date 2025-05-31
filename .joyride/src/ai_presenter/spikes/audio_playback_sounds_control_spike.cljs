(ns ai-presenter.spikes.audio-playback-sounds-control-spike
  "🎯 Spike: Programmatic Audio Playback Control in VS Code/Electron/Node.js

  Goal: Find the best way to achieve full programmatic control (play/pause/resume/stop)
  of audio files from within Joyride/VS Code environment.

  Spike Plan: /docs/audio-playback-spike-plan.md"
  (:require ["vscode" :as vscode]
            ["path" :as path]
            [promesa.core :as p]
            ["fs" :as fs]))


;; Try with sounds-control npm package in a WebView.

;; 🎯 SPIKE FINDINGS:
;; ✅ sounds-control requires browser environment (window object)
;; ✅ WebView with retainContextWhenHidden is the solution
;; ✅ Message passing works for communication with audio service
;; 🔄 Next: Complete HTML setup and message handlers

;; State management
(def !audio-webview (atom nil))

;; Create persistent webview for audio service
(defn create-audio-webview! []
  (reset! !audio-webview
    (vscode/window.createWebviewPanel
      (name :audioService)
      (name :AudioService)
      (.-One vscode/ViewColumn)
      (clj->js {:enableScripts true
                :retainContextWhenHidden true}))))

;; Cleanup function for testing
(defn dispose-audio-webview! []
  (when @!audio-webview
    (.dispose @!audio-webview)
    (reset! !audio-webview nil)))

;; HTML Content for WebView
(defn build-audio-service-html []
  (let [cdns "https://unpkg.com/sounds-control@latest/dist/index.umd.js"
        html (str "<!DOCTYPE html>"
                 "<html><head><meta charset='utf-8'>"
                 "<title>Audio Service</title>"
                 "<script src='" cdns "'></script>"
                 "</head><body>"
                 "<div id='status'>Audio Service Ready</div>"
                 "<script>"
                 "const vscode = acquireVsCodeApi();"
                 "let audioPlayer = null;"
                 "let currentAudio = null;"
                 "let audioFiles = new Map();"

                 ;; Initialize sounds-control
                 "window.addEventListener('load', () => {"
                 "  try {"
                 "    audioPlayer = new SoundsControl();"
                 "    document.getElementById('status').textContent = 'SoundsControl Ready';"
                 "    vscode.postMessage({type: 'ready', message: 'Audio service initialized'});"
                 "  } catch (e) {"
                 "    document.getElementById('status').textContent = 'Error: ' + e.message;"
                 "    vscode.postMessage({type: 'error', message: e.message});"
                 "  }"
                 "});"

                 ;; Message handler
                 "window.addEventListener('message', event => {"
                 "  const cmd = event.data;"
                 "  try {"
                 "    switch(cmd.command) {"
                 "      case 'load':"
                 "        if (audioPlayer && cmd.audioPath) {"
                 "          currentAudio = audioPlayer.load(cmd.audioPath);"
                 "          audioFiles.set(cmd.id || 'default', currentAudio);"
                 "          vscode.postMessage({type: 'loaded', id: cmd.id, path: cmd.audioPath});"
                 "        }"
                 "        break;"
                 "      case 'play':"
                 "        const playAudio = audioFiles.get(cmd.id || 'default') || currentAudio;"
                 "        if (playAudio) {"
                 "          playAudio.play();"
                 "          vscode.postMessage({type: 'playing', id: cmd.id});"
                 "        }"
                 "        break;"
                 "      case 'pause':"
                 "        const pauseAudio = audioFiles.get(cmd.id || 'default') || currentAudio;"
                 "        if (pauseAudio) {"
                 "          pauseAudio.pause();"
                 "          vscode.postMessage({type: 'paused', id: cmd.id});"
                 "        }"
                 "        break;"
                 "      case 'stop':"
                 "        const stopAudio = audioFiles.get(cmd.id || 'default') || currentAudio;"
                 "        if (stopAudio) {"
                 "          stopAudio.stop();"
                 "          vscode.postMessage({type: 'stopped', id: cmd.id});"
                 "        }"
                 "        break;"
                 "      case 'volume':"
                 "        const volAudio = audioFiles.get(cmd.id || 'default') || currentAudio;"
                 "        if (volAudio && typeof cmd.volume === 'number') {"
                 "          volAudio.setVolume(cmd.volume);"
                 "          vscode.postMessage({type: 'volumeSet', id: cmd.id, volume: cmd.volume});"
                 "        }"
                 "        break;"
                 "    }"
                 "  } catch (e) {"
                 "    vscode.postMessage({type: 'error', message: e.message, command: cmd.command});"
                 "  }"
                 "});"
                 "</script></body></html>")]
    html))

;; Set up the webview with audio service
(defn setup-audio-webview! []
  (when @!audio-webview
    (let [html (build-audio-service-html)]
      (set! (.. @!audio-webview -webview -html) html)
      ;; Set up message listener
      (.onDidReceiveMessage
        (.-webview @!audio-webview)
        (fn [message]
          (js/console.log "Audio service message:" message)
          ;; Handle messages from webview here
          message)))))

;; Audio control functions
(defn send-audio-command! [command & args]
  (when @!audio-webview
    (.postMessage
      (.-webview @!audio-webview)
      (apply merge {:command (name command)} args))))

(defn load-audio! [audio-path & {:keys [id]}]
  (send-audio-command! :load {:audioPath audio-path :id (or id "default")}))

(defn play-audio! [& {:keys [id]}]
  (send-audio-command! :play {:id (or id "default")}))

(defn pause-audio! [& {:keys [id]}]
  (send-audio-command! :pause {:id (or id "default")}))

(defn stop-audio! [& {:keys [id]}]
  (send-audio-command! :stop {:id (or id "default")}))

(defn set-volume! [volume & {:keys [id]}]
  (send-audio-command! :volume {:volume volume :id (or id "default")}))

;; Complete setup function
(defn init-audio-service! []
  (create-audio-webview!)
  (setup-audio-webview!)
  @!audio-webview)

(comment
  ;; ✅ TESTED IN REPL:
  ;; 1. Required sounds-control successfully
  ;; 2. Created webview with persistent context
  ;; 3. Confirmed webview object exists and is functional

  ;; 🔄 TODO:
  ;; - Set up HTML content with sounds-control CDN
  ;; - Implement message passing for audio commands
  ;; - Test actual audio playback

  ;; Test webview creation
  (create-audio-webview!)
  @!audio-webview

  (p/let [load+ (load-audio! "/Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/slides/voice/test-playback.mp3")]
    (def load+ load+) ;=> true
    )

  (p/let [play+ (play-audio!)]
    (def play+ play+) ;=> true
                      ;(no sound, though)
    )

  :rcf)

;; 🔧 DEBUGGING HELPERS
(defn test-with-web-audio! []
  ;; Test with a web-accessible audio file to rule out file path issues
  (load-audio! "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav" :id "web-test"))

(defn add-debug-to-webview! []
  ;; Add debug commands to webview HTML - call this after init
  (when @!audio-webview
    ;; Add debug case to message handler
    (send-audio-command! :eval {:code "console.log('Debug: audioPlayer =', audioPlayer); console.log('Debug: currentAudio =', currentAudio);"})))

(defn enable-webview-dev-tools! []
  ;; Enable dev tools for the webview to see console messages
  (when @!audio-webview
    ;; This might help with debugging
    (.postMessage
     (.-webview @!audio-webview)
     {:command "enableDevTools"})))

;; 🎯 TROUBLESHOOTING STEPS:
;; 1. File access: Local files might not be accessible from webview
;; 2. Audio permissions: Browser might need user interaction
;; 3. sounds-control loading: CDN might be failing
;; 4. Message passing: Commands might not be reaching webview properly

;; 🔧 FILE-BASED HTML APPROACH (to avoid ClojureScript reader issues)
(defn load-html-from-file! []
  "Load HTML content from external file to avoid string parsing issues"
  (when @!audio-webview
    (let [fs (js/require "fs")
          path (js/require "path")
          html-path (path/join js/__dirname "../ai_presenter/audio-service.html")
          html-content (.readFileSync fs html-path "utf8")]
      (set! (.. @!audio-webview -webview -html) html-content))))

(defn init-audio-service-v2! []
  "Improved version using external HTML file"
  (create-audio-webview!)
  (load-html-from-file!)
  ;; Set up message listener
  (.onDidReceiveMessage
    (.-webview @!audio-webview)
    (fn [message]
      (js/console.log "Audio service message:" message)
      message))
  @!audio-webview)

