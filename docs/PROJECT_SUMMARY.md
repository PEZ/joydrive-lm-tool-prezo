# AI-Powered Joyride Presentation System - Project Summary

## Overview

This is a complete AI-powered presentation system built entirely with [Joyride](https://github.com/BetterThanTomorrow/joyride) - user space ClojureScript scripting for VS Code. The system enables AI-assisted slide creation, narration generation, text-to-speech conversion, and interactive presentation delivery, all within the VS Code environment.

**Key Innovation**: This demonstrates "Interactive Programming" where human developers and AI (GitHub Copilot) collaborate in the same REPL environment to build and extend functionality live.

## Architecture

### Core Components

1. **Slide Navigation System** (`.joyride/src/next_slide.cljs`)
   - Keyboard-driven slide navigation
   - Configurable slide sequences via `slides.edn`
   - Context-aware VS Code integration
   - Slide notes management (`.joyride/src/next_slide_notes.cljs`)

2. **AI Presentation System** (`.joyride/src/ai_presenter/`)
   - **Audio Generation** (`audio_generation.cljs`) - OpenAI TTS integration
   - **Audio Playback** (`audio_playback.cljs`) - WebView-based audio player with user gesture handling
   - **Opening Sequence** (`opening_sequence.cljs`) - Presentation introduction sequence

3. **AI Mood System** (`.joyride/src/ai_mood_selector.cljs`)
   - Dynamic system prompt switching for different workflow phases
   - Status bar integration for easy mode switching

4. **Showtime Utility** (`.joyride/src/showtime.cljs`)
   - Status bar timer/stopwatch for presentation timing
   - Interactive start/stop controls

4. **Presentation Content**
   - Slides in Markdown format (`slides/*.md`)
   - Detailed presentation notes (`slides/*-notes.md`)
   - Generated audio files (`slides/voice/*.mp3`)

## Key File Paths & Descriptions

### Configuration
- `slides.edn` - Slide sequence configuration
- `deps.edn` - Clojure dependencies and source paths
- `package.json` - Node.js dependencies (ai-text-to-speech)

### Core Joyride Source
- `.joyride/src/next_slide.cljs` - Slide navigation engine
- `.joyride/src/next_slide_notes.cljs` - Slide notes management
- `.joyride/src/showtime.cljs` - Status bar timer/stopwatch
- `.joyride/src/ai_presenter/audio_generation.cljs` - TTS generation with file management
- `.joyride/src/ai_presenter/audio_playback.cljs` - WebView audio player with promise-based control
- `.joyride/src/ai_presenter/opening_sequence.cljs` - Presentation introduction sequence
- `.joyride/src/ai_mood_selector.cljs` - AI system prompt management
- `.joyride/resources/audio-service.html` - Audio playback WebView UI
- `.joyride/temp-audio/` - Temporary storage for generated audio

### AI System Prompts
- `prompts/system/presenter-instructions.md` - AI presenter behavior and Joyride API usage
- `prompts/system/slide-author-instructions.md` - Slide creation guidelines
- `prompts/system/audio-generator-instructions.md` - TTS generation workflow
- `prompts/system/slide-narration-author-instructions.md` - Narration script authoring
- `prompts/system/story-author-instructions.md` - Overall narrative structure

### Project Documentation
- `docs/PROJECT_SUMMARY.md` - This comprehensive project overview
- `docs/images/` - Documentation images and screenshots
- `docs/log/` - Development logs and debugging notes
  - `audio-debugging-log.md` - Audio system troubleshooting
  - `audio-playback-user-gesture-bug.md` - User gesture handling issues
  - `tts-voice-quality-investigation.md` - Voice quality testing

## Dependencies & Versions

### Node.js Dependencies
- `ai-text-to-speech@^1.0.5` - OpenAI TTS integration

### VS Code Extensions Required
- **Joyride** - ClojureScript scripting runtime
- **GitHub Copilot** - AI assistant with LM Tools
- **Calva** (recommended) - Clojure REPL integration

### Environment Variables
- `OPENAI_API_KEY` - Required for text-to-speech generation

## Available APIs & Functions

### Navigation Functions
```clojure
;; Core navigation (from user namespace)
(next-slide/next! true)                           ; Navigate forward
(next-slide/next! false)                          ; Navigate backward
(next-slide/show-slide-by-name!+ "slide-name.md") ; Show specific slide
(next-slide/get-current-slide-name+)              ; Get current slide filename
(next-slide/activate!)                            ; Enable navigation
(next-slide/deactivate!)                          ; Disable navigation
```

### Audio Generation Functions
```clojure
;; TTS generation (from user namespace)
(ai-presenter.audio-generation/generate-slide-audio!+ slide-name script-text)
(ai-presenter.audio-generation/generate-and-play-message!+ "quick message")
```

### Audio Playback Functions
```clojure
;; Audio control (from user namespace)
(ai-presenter.audio-playback/load-and-play-audio!+ "slides/voice/filename.mp3")
(ai-presenter.audio-playback/play-audio!)
(ai-presenter.audio-playback/pause-audio!)
(ai-presenter.audio-playback/stop-audio!)
(ai-presenter.audio-playback/set-volume! 0.5)
(ai-presenter.audio-playback/get-audio-status!+)
```

### Utility Functions
```clojure
;; VS Code integration
(vscode/commands.executeCommand "workbench.action.closeAuxiliaryBar") ; Hide chat
```

## AI Workflow Pipeline

The system supports a complete AI-assisted presentation creation pipeline:

```
Human Story Concept
     ↓
🤝 Human + slide-author → Draft slides & notes
     ↓ (iteration)
📖 story-author → Narrative structure refinement
     ↓ (system design)
🏗️ architect → System architecture planning
     ↓ (implementation)
💻 joyride-hacker → Technical code development
     ↓ (refinement)
🎙️ slide-narration-author → Narration scripts
     ↓ (polish)
🔊 audio-generator → High-quality TTS audio
     ↓ (review)
🔍 reviewer → Code and content review
     ↓ (generation)
✨ presenter (+ human) → Interactive presentation
```

## Implementation Patterns

### Promise-Based Architecture
- Extensive use of Promesa for async operations
- `p/let` for sequential promise chaining
- Error handling with `p/catch`

### Functional Core, Imperative Shell
- Pure functions for data transformation
- Side effects isolated to specific functions marked with `!+`
- Immutable state management with atoms

### WebView Integration Pattern
- HTML/CSS/JS in `.joyride/resources/`
- Bidirectional communication via VS Code WebView API
- Promise-based message handling

### AI Integration Patterns
- System prompt switching via mood selector
- Contextual AI assistance using slide notes
- REPL-driven development with AI pair programming

## Development Workflow

### Setup
1. Install required VS Code extensions (Joyride, Copilot, Calva)
2. Set `OPENAI_API_KEY` environment variable
3. Open project in VS Code - Joyride auto-activates

### REPL-Driven Development
1. Start Joyride REPL: `Calva: Start Joyride REPL and Connect`
2. Evaluate code interactively in `.cljs` files
3. Use `comment` blocks for REPL experiments
4. Test functions immediately with `(comment :rcf)` blocks

### AI-Assisted Development
1. Switch AI mood using status bar button (🎭)
2. Use different moods for different tasks:
   - `presenter` - Live presentation
   - `joyride-hacker` - Technical code development
   - `architect` - System design discussions
   - `slide-author` - Content creation
   - `slide-narration-author` - Narration script creation
   - `story-author` - Narrative development
   - `audio-generator` - TTS generation
   - `reviewer` - Code review

## Technical Architecture Highlights

### State Management
- Central state atom in each namespace (`!state`)
- Immutable updates with `swap!`
- Context preservation across slide transitions

### Error Handling Strategy
- Promise-based error catching
- Graceful degradation for missing audio files
- User-friendly error messages via VS Code notifications

### File System Integration
- Dynamic file creation for audio generation
- Workspace-relative path handling
- Automatic directory creation with error handling

## Usage Examples

### Basic Presentation Flow
```clojure
;; 1. Start presentation
(next-slide/show-slide-by-name!+ "hello.md")

;; 2. Generate audio if needed
(ai-presenter.audio-generation/generate-slide-audio!+
  "hello"
  "Welcome to our presentation...")

;; 3. Play audio
(ai-presenter.audio-playback/load-and-play-audio!+ "slides/voice/hello.mp3")

;; 4. Navigate when ready
(next-slide/next! true)
```

### AI-Assisted Workflow
Simply ask GitHub Copilot in the appropriate mood:
- "Present the first slide"
- "Generate audio for the current slide"
- "Create a new slide about Clojure benefits"

The AI will execute the appropriate Joyride functions and provide feedback.

## Development Philosophy

This project embodies **Interactive Programming** principles:
- Live coding and immediate feedback
- Human-AI collaboration in shared environment
- Incremental development with REPL-driven workflow
- Functional programming with ClojureScript
- VS Code as a malleable development environment

The goal is to demonstrate how developers can reshape their tools collaboratively with AI, creating bespoke solutions that traditional extensions cannot provide.

## Extension Possibilities

### Custom Audio Voices
- Modify `ai-presenter.audio-generation/generate-slide-audio!+`
- Change TTS service from OpenAI to alternatives
- Implement voice selection UI

### Interactive Elements
- Add audience participation via VS Code commands
- Implement real-time polling or Q&A
- Create slide annotation system

### Presentation Analytics
- Track slide timing and navigation patterns
- Add presentation recording capabilities
- Implement audience engagement metrics

### Multi-Modal Content
- Add video integration to slides
- Implement live coding demonstrations
- Create interactive code examples

### Keyboard Shortcuts Enhancement
- Current shortcuts in `next_slide.cljs` header comments
- Add presenter remote control support
- Implement gesture-based navigation

## Content
- `slides/` - Markdown slides with embedded HTML/CSS
- `slides/*-notes.md` - Presentation guidance and narration scripts
- `slides/voice/` - Generated MP3 audio files
- `slides/images/` - Presentation assets
- `slides/narration-script/` - Detailed narration scripts
- `slides/opening-sequence/` - Presentation introduction assets