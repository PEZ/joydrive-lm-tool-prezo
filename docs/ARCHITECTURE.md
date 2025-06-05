# Project Architecture Documentation

## Overview
**AI-Powered Joyride Presentation System** - A complete presentation system built entirely in Joyride/ClojureScript where AI generates and narrates slide content with voice synthesis.

## 🏗️ Architecture Layers

### 1. Core Technologies
- **Joyride** - ClojureScript runtime for VS Code extension API
- **VS Code Extension API** - UI and file system integration
- **OpenAI TTS API** - High-quality voice synthesis
- **Web Audio API** - Audio playback through webview

### 2. System Components

#### Presentation Engine (`next-slide.cljs`)
- **Purpose**: Core slide navigation and presentation control
- **Key Features**:
  - Keyboard-driven navigation (arrow keys, PageUp/Down)
  - EDN-based slide configuration
  - Zen mode integration
  - State management for active slides

#### AI Presenter Core (`ai_presenter/core.cljs`)
- **Purpose**: Pure state management for AI presentation features
- **State Structure**:
  ```clojure
  {:ai-presenter/status :inactive|:active|:presenting|:paused|:error
   :ai-presenter/slides vector
   :ai-presenter/slide-index number
   :ai-presenter/current-slide string
   :ai-presenter/current-audio string
   :ai-presenter/audio-cache map}
  ```

#### Audio Generation (`ai_presenter/audio_generation.cljs`)
- **Purpose**: Text-to-speech integration with OpenAI
- **Features**:
  - High-quality HD voice synthesis (model: tts-1-hd, voice: nova)
  - File management and cleanup
  - Environment validation
  - Error handling and recovery

#### Audio Playback (`ai_presenter/audio_playback.cljs`)
- **Purpose**: Web Audio API integration through webview
- **Architecture**:
  - Functional core with pure functions
  - Imperative shell for side effects
  - Promise-based async operations
  - User gesture requirement handling

## 🔄 Data Flow

### Slide Navigation Flow
```
User Input → next-slide state → VS Code markdown preview → Display
```

### Audio Generation Flow
```
Slide Content → AI Narration Script → OpenAI TTS → MP3 File → Audio Cache
```

### Audio Playback Flow
```
MP3 File → Webview → Web Audio API → User Gesture Check → Playback
```

## 📁 File Structure
```
├── .joyride/src/
│   ├── next_slide.cljs           # Core presentation engine
│   └── ai_presenter/
│       ├── core.cljs            # State management
│       ├── audio_generation.cljs # TTS integration
│       └── audio_playback.cljs   # Audio playback
├── .joyride/resources/
│   └── audio-service.html       # Webview for audio
├── slides/                      # Markdown slides
│   ├── *.md                    # Individual slides
│   ├── *-notes.md              # Presentation notes
│   └── voice/                  # Generated audio files
├── prompts/system/             # AI system prompts
├── docs/                       # Documentation
└── slides.edn                 # Slide configuration
```

## 🎯 Key Design Decisions

### 1. Functional Core, Imperative Shell
- Pure functions for state management and data transformation
- Side effects isolated in specific functions
- Promises for async operations

### 2. VS Code Integration
- Native markdown preview for slides
- Joyride for ClojureScript runtime
- Extension API for file system operations

### 3. AI Workflow Support
- Multiple system prompts for different roles
- Configurable AI behavior through prompt switching
- Integration with GitHub Copilot

### 4. Audio Architecture
- Webview isolation for Web Audio API
- User gesture requirement compliance
- File-based audio caching
- Relative path resolution

## 🔧 Extension Points

### Custom Voices
Modify `audio_generation.cljs`:
```clojure
(ai-speech #js {:voice "alloy" ; or "echo", "fable", "onyx", "nova", "shimmer"
                :model "tts-1-hd"})
```

### Custom Navigation
Add keyboard shortcuts in `next_slide.cljs`:
```clojure
;; Custom keybinding example
{
  "key": "ctrl+alt+j n",
  "command": "joyride.runCode",
  "args": "(next-slide/next! true)"
}
```

### AI System Prompts
Add new prompt files in `prompts/system/` and update the mood selector.

## 🚀 Performance Considerations

### Audio Generation
- Files cached in `slides/voice/`
- Temp files cleaned up automatically
- HD model for quality (slower but better)

### Memory Management
- Webview state preserved between slides
- Atom-based state management
- File handles properly closed

## 🔒 Security Model

### Environment Variables
- OpenAI API key from environment only
- No hardcoded credentials
- Validation before API calls

### File System Access
- Workspace-relative paths only
- VS Code API file system integration
- Proper error handling for file operations
