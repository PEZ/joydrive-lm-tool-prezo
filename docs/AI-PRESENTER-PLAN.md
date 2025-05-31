# AI Presenter Mode Implementation Plan

## Overview
Extend the existing slideshow script with an AI presenter mode that automatically presents slides using AI-generated voice narration. The implementation will integrate with the existing `next-slide` script and provide seamless user control.

## Requirements

### Functional Requirements

#### FR-1: AI Presenter Mode Activation
- **FR-1.1**: Provide a function to start AI presenter mode
- **FR-1.2**: When activated, begin presenting the current slide with AI voice
- **FR-1.3**: Automatically advance to next slide after voice presentation completes
- **FR-1.4**: Continue presenting subsequent slides until manually stopped or presentation ends

#### FR-2: Audio Preparation System
- **FR-2.1**: Provide a function to pre-generate audio files for presentation slides
- **FR-2.2**: Convert slide text/script to MP3 audio files using `ai-text-to-speech` module
- **FR-2.3**: Store generated audio in `slides/voice/` directory with slide-name matching (slide filename + `.mp3`)

#### FR-3: User Control Interface
- **FR-3.1**: Display status bar button when AI presenter mode is active
- **FR-3.2**: Status bar button opens control menu with options:
  - Pause/Resume voice presentation
  - Restart current slide presentation
  - Skip to next slide
  - Exit AI presenter mode
- **FR-3.3**: All control actions take immediate effect

#### FR-4: Mode Management
- **FR-4.1**: Clean exit from AI presenter mode stops any active voice playback
- **FR-4.2**: When mode is inactive, slideshow behaves as original implementation
- **FR-4.3**: Mode state persists across slide navigation within session
- **FR-4.4**: Handle edge cases: reaching end of presentation, missing audio files

### Non-Functional Requirements

#### NFR-1: Integration
- **NFR-1.1**: Maintain compatibility with existing `next-slide` script functionality
- **NFR-1.2**: Preserve existing keyboard shortcuts and navigation when not in AI mode
- **NFR-1.3**: Use existing slide configuration and state management patterns

#### NFR-2: Audio Quality
- **NFR-2.1**: Generate audio using OpenAI TTS with clear, professional voice
- **NFR-2.2**: Use MP3 format for cross-platform compatibility
- **NFR-2.3**: Consistent voice settings across all generated audio files

#### NFR-3: Error Handling
- **NFR-3.1**: Graceful fallback when audio files are missing
- **NFR-3.2**: Clear error messages for TTS generation failures
- **NFR-3.3**: Robust handling of API rate limits and network issues

#### NFR-4: User Experience
- **NFR-4.1**: Immediate feedback for all user actions
- **NFR-4.2**: Clear visual indicators for current mode state
- **NFR-4.3**: Non-blocking operations - UI remains responsive during audio playback

### Technical Constraints

#### TC-1: Dependencies
- **TC-1.1**: Must integrate with existing Joyride ClojureScript environment
- **TC-1.2**: Requires `ai-text-to-speech` npm module (v1.0.5+)
- **TC-1.3**: Depends on OpenAI API key configuration
- **TC-1.4**: Uses VS Code API for audio playback and UI controls

#### TC-2: File System
- **TC-2.1**: Audio files stored in workspace `slides/voice/` directory
- **TC-2.2**: File naming convention: `{slide-name}.mp3`
- **TC-2.3**: Directory structure automatically created if missing

#### TC-3: State Management
- **TC-3.1**: Implement state machine for AI presenter mode lifecycle
- **TC-3.2**: Maintain separation between presenter state and existing slide navigation state

## Success Criteria
1. User can seamlessly switch between manual and AI presenter modes
2. Audio preparation completes successfully for all slides with valid scripts
3. AI presenter mode provides smooth, automatic slide progression with voice
4. User controls respond immediately and predictably
5. Integration preserves all existing slideshow functionality

## Technical Architecture

### State Machine Design

The AI presenter state machine focuses solely on **presentation flow control**, with audio playback as a managed side effect:

```clojure
;; Presenter States (only presentation concerns)
:presenter/inactive    ; Default state - normal slideshow behavior
:presenter/active      ; AI presenter mode active, ready to present
:presenter/presenting  ; Currently presenting a slide (audio + display)
:presenter/paused      ; Presentation paused (audio paused, slide visible)
:presenter/error       ; Error state with recovery options
```

**State Transitions:**
- `inactive → active` (via activate-presenter!)
- `active → presenting` (when slide presentation starts)
- `presenting → paused` (via pause! action)
- `paused → presenting` (via resume! action)
- `presenting → active` (when slide completes, ready for next)
- `any-state → inactive` (via deactivate! - stops all audio)

### Core Namespaces Structure

#### `ai-presenter.db`
**Purpose:** Unified state management (data-oriented)
**Key Functions:**
- `get-state` - Access current unified state
- `update-state!` - Apply state changes with validation
- `subscribe-to-changes` - React to state updates

#### `ai-presenter.core`
**Purpose:** Main coordinator and public API
**Key Functions:**
- `activate-presenter!` - Start AI presentation mode
- `deactivate-presenter!` - Stop and cleanup
- `prepare-slide-audio!` - Generate audio from slide-name and script text
- `present-slide!` - Present specific slide with audio

#### `ai-presenter.state-machine`
**Purpose:** Pure presentation flow logic (no audio concerns)
**Key Functions:**
- `transition` - Pure state transition (input: state + event → new state)
- `can-transition?` - Validate transition legality
- `get-available-actions` - Actions available in current state

#### `ai-presenter.audio`
**Purpose:** Audio lifecycle management (single responsibility)
**Key Functions:**
- `generate-audio!` - Create MP3 from provided script text
- `play-audio!` - Start audio playback (returns audio-id for control)
- `pause-audio!` - Pause specific audio by id
- `resume-audio!` - Resume specific audio by id
- `stop-audio!` - Stop and cleanup specific audio
- `stop-all-audio!` - Emergency stop all audio (chaos prevention!)

#### `ai-presenter.ui`
**Purpose:** User interface controls
**Key Functions:**
- `show-status-button!` - Display presenter controls
- `hide-status-button!` - Remove UI elements
- `show-control-menu!` - Present user options
- `update-status-text!` - Real-time status updates

### Data Structures

#### Unified State Atom (Flat Design)
```clojure
(def !app-state
  (atom {;; Next-slide state (existing)
         :next/active? false
         :next/active-slide 0
         :next/config-path nil

         ;; AI Presenter state (new)
         :presenter/status :presenter/inactive
         :presenter/current-audio-id nil
         :presenter/auto-advance? true
         :presenter/error-message nil

         ;; UI state
         :ui/status-button nil
         :ui/control-menu nil}))
```

#### Audio Registry (Separate Atom for Audio Tracking)
```clojure
(def !audio-registry
  (atom {;; Track active audio to prevent chaos
         :active-audio-id nil
         :audio-sessions {}  ; id → {:file-path, :status, :element}
         :available-files #{}}))
```

#### Audio Configuration
```clojure
(def audio-config
  {:voice "nova"           ; Clear, professional voice
   :model "tts-1"          ; Standard quality for speed
   :response-format "mp3"  ; VS Code compatible
   :dest-dir "slides/voice/"
   :suffix-type "none"})   ; Predictable filenames
```

### Integration Points

#### With `next-slide` Script
- **State Coordination:** Presenter state subscribes to slide changes
- **Navigation Override:** In presenter mode, auto-advance replaces manual navigation
- **Fallback Behavior:** Seamless return to manual mode when deactivated

#### With VS Code APIs
- **Audio Playback:** Use `vscode.env.openExternal` for audio file URIs
- **Status Bar:** Leverage existing status bar patterns from `showtime` script
- **File System:** `vscode.workspace.fs` for audio file operations

### Implementation Phases (TDD Approach)

#### Phase 0: Spikes

1. [x] Figure out how to use the `ai-text-to-speech` module with Joyride.

Results:

```clojure
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
```

2. [ ] Figure out how to control audio play back from Joyride, play/pause/resume/stop

#### Phase 1: Foundation + Tests (Pure Functions First)
1. **State Machine Logic (Pure Functions)**
   ```clojure
   ;; 100% testable without mocks
   (defn transition [current-state event]
     ;; Pure function: state + event → new state
   )

   (defn valid-transitions [state]
     ;; Pure function: state → #{valid-events}
   )
   ```
   **Tests:** State transition truth tables, invalid transition handling

2. **Audio Registry Management (Pure Functions)**
   ```clojure
   (defn register-audio [registry audio-id file-path]
     ;; Pure function: registry + audio-data → new registry
   )

   (defn stop-all-audio-sessions [registry]
     ;; Pure function: registry → cleanup commands + new registry
   )
   ```
   **Tests:** Audio session lifecycle, collision detection, cleanup logic

3. **Database Layer (Controlled Side Effects)**
   ```clojure
   (defn update-state! [update-fn]
     ;; Controlled mutation with validation
   )
   ```
   **Tests:** State updates, validation, rollback on errors

#### Phase 2: Audio Engine + Tests (Single Responsibility)
1. **Audio Generation (Testable with File System)**
   ```clojure
   (defn generate-audio! [slide-name script-text]
     ;; Returns: {:success true :file-path "..."} | {:success false :error "..."}
   )
   ```
   **Tests:** File creation, naming conventions, error handling, OpenAI API failures

2. **Audio Playback Control (Hardware Abstraction)**
   ```clojure
   (defn play-audio! [file-path]
     ;; Returns audio-id for control
   )

   (defn pause-audio! [audio-id]
   (defn resume-audio! [audio-id]
   (defn stop-audio! [audio-id]
   ```
   **Tests:** Playback lifecycle, pause/resume cycles, stop behavior, multiple audio prevention

#### Phase 3: Integration + Tests (Orchestration)
1. **Presenter Coordinator**
   ```clojure
   (defn present-slide! [slide-index]
     ;; Orchestrates: state-machine + audio + next-slide integration
   )
   ```
   **Tests:** End-to-end presentation flow, error recovery, state consistency

2. **Next-Slide Integration**
   ```clojure
   (defn auto-advance-after-audio! [audio-id]
     ;; Hooks audio completion → slide advancement
   )
   ```
   **Tests:** Timing, manual override, mode switching

#### Phase 4: UI + Tests (User Interface)
1. **Status Bar Controls**
   **Tests:** UI state consistency, user action handling

2. **Menu Actions**
   **Tests:** Command dispatch, state validation

### File Structure
```
.joyride/src/
├── ai_presenter/
│   ├── db.cljs             ; Unified state management
│   ├── core.cljs           ; Main API and coordination
│   ├── state_machine.cljs  ; Pure presentation flow logic
│   ├── audio.cljs          ; Audio lifecycle management
│   └── ui.cljs            ; VS Code UI controls
├── test/
│   └── ai_presenter/       ; Unit tests for each namespace
│       ├── state_machine_test.cljs
│       ├── audio_test.cljs
│       ├── db_test.cljs
│       └── core_test.cljs
└── next_slide.cljs         ; Existing (enhanced for integration)

slides/voice/               ; Generated audio files
├── hello.mp3
├── topics.mp3
└── ...
```

### Testing Strategy

**Pure Functions (No Mocks Needed):**
- State machine transitions
- Audio registry operations
- Data transformations
- Validation logic

**Controlled Side Effects (Minimal Mocking):**
- File system operations (use temp directories)
- State atom updates (test actual atoms)

**Integration Points (Focused Testing):**
- VS Code API interactions (mock only the API boundary)
- Audio playback (mock audio element, test control logic)

This approach ensures **high confidence in core logic** while keeping tests maintainable and fast.
