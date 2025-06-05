# API Reference Documentation

## Core Functions

### next-slide Namespace

#### Navigation Functions
```clojure
(next-slide/activate! [config-path])
;; Activates the presentation system
;; config-path: optional vector, defaults to ["slides.edn"]

(next-slide/deactivate!)
;; Deactivates the presentation system

(next-slide/next! [forward?])
;; Navigate to next/previous slide
;; forward?: boolean, true for next, false for previous

(next-slide/current!)
;; Display current slide

(next-slide/restart!)
;; Jump to first slide

(next-slide/show-slide-by-name!+ slide-name)
;; Show specific slide by filename
;; slide-name: string, e.g., "hello.md"

(next-slide/get-current-slide-name+)
;; Returns promise of current slide filename
```

### ai-presenter.audio-generation Namespace

#### Audio Generation Functions
```clojure
(generate-slide-audio!+ slide-name script-text)
;; Generate audio file for slide
;; slide-name: string, slide identifier
;; script-text: string, narration text
;; Returns: promise of {:success boolean :slide-name string ...}

(generate-and-play-message!+ text)
;; Generate and immediately play audio
;; text: string, text to speak
;; Returns: promise of result map

(validate-environment)
;; Check OpenAI API key availability
;; Returns: {:api-key-present? boolean :api-key-length number}
```

### ai-presenter.audio-playback Namespace

#### Playback Control Functions
```clojure
(init-audio-service!)
;; Initialize audio webview service
;; Returns: webview instance

(load-audio-promise!+ file-path & {:keys [id timeout-ms]})
;; Load audio file with promise
;; file-path: string, relative or absolute path
;; id: optional string, audio instance identifier
;; timeout-ms: optional number, defaults to 10000
;; Returns: promise that resolves when audio ready

(play-audio! & {:keys [id]})
;; Play loaded audio
;; id: optional string, audio instance identifier

(pause-audio! & {:keys [id]})
;; Pause audio playback

(stop-audio! & {:keys [id]})
;; Stop audio playback

(set-volume! volume & {:keys [id]})
;; Set audio volume
;; volume: number 0.0-1.0

(get-audio-status!+)
;; Get current audio system status
;; Returns: promise of status map

(load-and-play-audio!+ file-path)
;; High-level function: load and play audio
;; Handles user gesture requirements automatically
;; file-path: string, path to audio file
;; Returns: promise of {:success boolean ...}
```

#### Utility Functions
```clojure
(check-user-gesture!+)
;; Check if user has completed required gesture
;; Returns: promise of boolean

(prompt-user-for-audio-gesture!+)
;; Prompt user to enable audio
;; Returns: promise that resolves when user confirms
```

### ai-presenter.core Namespace (Pure Functions)

#### State Management
```clojure
(initial-state)
;; Create initial presenter state
;; Returns: state map

(activate-presenter state slides)
;; Activate presenter with slide list
;; state: current state map
;; slides: vector of slide paths
;; Returns: updated state map

(start-presenting state)
;; Transition to presenting mode
;; Returns: updated state map

(next-slide state)
(previous-slide state)
;; Navigate slides
;; Returns: updated state map

(deactivate-presenter state)
;; Deactivate and reset state
;; Returns: clean state map
```

## Configuration

### slides.edn Format
```clojure
{:slides ["slides/intro.md"
          "slides/demo.md"
          "slides/conclusion.md"]}
```

### Environment Variables
```bash
export OPENAI_API_KEY="sk-..."  # Required for audio generation
```

### Keyboard Shortcuts
Configure in VS Code `keybindings.json`:
```json
[
  {
    "key": "ctrl+alt+j s",
    "command": "joyride.runCode",
    "args": "(next-slide/activate!)"
  },
  {
    "key": "right",
    "command": "joyride.runCode",
    "args": "(next-slide/next! true)",
    "when": "next-slide:active && !inputFocus"
  }
]
```

## Error Handling

### Common Errors and Solutions

#### "OPENAI_API_KEY not found"
```clojure
;; Check environment
(ai-presenter.audio-generation/validate-environment)
;; => {:api-key-present? false :api-key-length nil}

;; Solution: Set environment variable
```

#### "Audio load timeout"
```clojure
;; Check audio status
(p/let [status (ai-presenter.audio-playback/get-audio-status!+)]
  (println status))

;; Common causes:
;; - File not found
;; - User gesture not completed
;; - Webview not initialized
```

#### "Slide not found"
```clojure
;; List available slides
(p/let [slides (next-slide/slides-list+)]
  (println "Available slides:" slides))
```

## Integration Examples

### Basic Presentation Flow
```clojure
;; 1. Activate presentation
(next-slide/activate!)

;; 2. Initialize audio service
(ai-presenter.audio-playback/init-audio-service!)

;; 3. Show first slide
(next-slide/show-slide-by-name!+ "intro.md")

;; 4. Generate audio if needed
(p/let [result (ai-presenter.audio-generation/generate-slide-audio!+
                "intro"
                "Welcome to our presentation about Joyride!")]
  (println "Audio generated:" (:success result)))

;; 5. Play audio
(ai-presenter.audio-playback/load-and-play-audio!+ "slides/voice/intro.mp3")
```

### AI-Assisted Workflow
```clojure
;; Combined workflow with error handling
(p/let [;; Activate system
        _ (next-slide/activate!)
        _ (ai-presenter.audio-playback/init-audio-service!)
        
        ;; Navigate to slide
        _ (next-slide/show-slide-by-name!+ "demo.md")
        
        ;; Generate audio with error handling
        gen-result (p/catch
                    (ai-presenter.audio-generation/generate-slide-audio!+
                     "demo"
                     "This is our demo slide with exciting content!")
                    (fn [error]
                      {:success false :error (.-message error)}))
        
        ;; Play audio if generation succeeded
        play-result (if (:success gen-result)
                      (ai-presenter.audio-playback/load-and-play-audio!+
                       "slides/voice/demo.mp3")
                      {:success false :reason "audio-generation-failed"})]
  
  {:generation gen-result
   :playback play-result})
```
