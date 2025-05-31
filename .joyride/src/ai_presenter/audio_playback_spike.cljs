(ns ai-presenter.audio-playback-spike
  "🎯 Spike: Programmatic Audio Playback Control in VS Code/Electron/Node.js

  Goal: Find the best way to achieve full programmatic control (play/pause/resume/stop)
  of audio files from within Joyride/VS Code environment.

  Spike Plan: /docs/audio-playback-spike-plan.md"
  (:require ["vscode" :as vscode]
            ["path" :as path]
            ["fs" :as fs]
            ["child_process" :as child-process]))


;; ========================================
;; 🎯 SPIKE FINDINGS: Working Audio Playback Control
;; ========================================

(defn play-audio-file
  "Play audio file and return control session data using child_process + afplay

   Returns map with:
   - :session-id - unique identifier
   - :file-path - path being played
   - :process - the child process (for stop)
   - :volume - volume level used
   - :status - current status atom (:playing, :stopped, :paused)
   - :started-at - timestamp
   - :controls - map of control functions

   Controls available:
   - :stop - stops audio playback (working ✅)
   - :pause - no-op for afplay (structure for future)
   - :resume - no-op for afplay (structure for future)
   - :get-status - returns current status

   Usage:
   (def session (play-audio-file \"/path/to/file.mp3\" :volume 0.8))
   (let [controls (:controls session)]
     ((controls :stop)))"
  [file-path & {:keys [volume] :or {volume 1}}]
  (let [session-id (random-uuid)
        status (atom :playing)
        process (child-process/spawn "afplay"
                                   #js [file-path "-v" (str volume)])
        session {:session-id session-id
                 :file-path file-path
                 :process process
                 :volume volume
                 :status status
                 :started-at (js/Date.now)
                 :controls {:stop (fn []
                                   (when process (.kill process "SIGTERM"))
                                   (reset! status :stopped))
                           :pause (fn []
                                    ;; No-op for afplay, but structure for future
                                    (reset! status :paused)
                                    nil)
                           :resume (fn []
                                     ;; No-op for afplay, but structure for future
                                     (reset! status :playing)
                                     nil)
                           :get-status (fn [] @status)}}]
    (vscode/window.showInformationMessage
     (str "🎵 Playing: " (path/basename file-path)))
    session))


;; ========================================
;; 🧪 SPIKE VALIDATION RESULTS
;; ========================================

(comment
  ;; ✅ CONFIRMED WORKING: Audio playback with child_process + afplay
  ;; ✅ CONFIRMED WORKING: Volume control (0.0 to 1.0+)
  ;; ✅ CONFIRMED WORKING: Stop functionality
  ;; ✅ CONFIRMED WORKING: Status tracking with atoms
  ;; ✅ CONFIRMED WORKING: Data-oriented session management
  ;; ⚠️  LIMITATION: No pause/resume (afplay doesn't support it)
  ;; ⚠️  LIMITATION: No completion event detection (only process exit)

  ;; Usage example:
  (def voice-dir "/Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/slides/voice")
  (def test-file (str voice-dir "/" "Hello, this is a test..mp3"))

  (def session (play-audio-file test-file :volume 0.5))
  (let [controls (:controls session)]
    ((controls :get-status))  ; => :playing
    ((controls :stop))        ; => stops audio
    ((controls :get-status))) ; => :stopped

  :rcf)

