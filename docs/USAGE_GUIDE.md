# Usage Guide

## Getting Started

### Prerequisites
1. **VS Code** with the following extensions:
   - [Joyride](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.joyride)
   - [GitHub Copilot](https://marketplace.visualstudio.com/items?itemName=GitHub.copilot)
   - [Calva](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva) (recommended)

2. **OpenAI API Key** (for audio generation):
   ```bash
   export OPENAI_API_KEY="your-api-key-here"
   ```

### Quick Start
1. Open this project in VS Code
2. Look for the 🎭 button in the status bar
3. Select **presenter** mood
4. Ask Copilot: "Demo the presentation system for me"

## Core Workflows

### 1. Basic Slide Navigation

#### Manual Navigation
```clojure
;; Activate the presentation system
(next-slide/activate!)

;; Navigate forward
(next-slide/next!)

;; Navigate backward  
(next-slide/next! false)

;; Go to specific slide
(next-slide/show-slide-by-name!+ "hello.md")

;; Restart from beginning
(next-slide/restart!)
```

#### Keyboard Shortcuts
- **Right Arrow**: Next slide
- **Left Arrow**: Previous slide
- **Page Down**: Next slide
- **Page Up**: Previous slide
- **F5**: Toggle Zen mode
- **Ctrl+Alt+Cmd+Left**: Restart presentation

### 2. Audio-Enhanced Presentations

#### Initialize Audio System
```clojure
;; Required before audio playback
(ai-presenter.audio-playback/init-audio-service!)

;; This creates a webview for audio playback
;; You'll see an "Audio Service" tab appear
```

#### Generate Audio for Slides
```clojure
;; Generate audio from text
(ai-presenter.audio-generation/generate-slide-audio!+
 "slide-name"
 "Your narration script here...")

;; The audio file will be saved to slides/voice/slide-name.mp3
```

#### Play Audio
```clojure
;; Load and play audio file
(ai-presenter.audio-playback/load-and-play-audio!+
 "slides/voice/hello.mp3")

;; For first-time use, you'll need to click "Enable Audio" 
;; in the webview to satisfy browser security requirements
```

### 3. AI-Assisted Presentation Creation

#### System Prompt Modes
Use the 🎭 status bar button to switch between modes:

- **slide-author**: Create and edit slide content
- **story-author**: Develop narrative structure
- **slide-narration-author**: Write engaging narration scripts
- **audio-generator**: Generate voice audio
- **presenter**: Present with voice narration
- **joyride-hacker**: Interactive development

#### Example AI Interactions

**Slide Creation:**
> "Create a slide about Joyride's key features with speaker notes"

**Narration Generation:**
> "Write an engaging 2-minute narration for the 'what-is-joyride.md' slide"

**Audio Generation:**
> "Generate HD audio for the intro slide using the narration script"

**Presenting:**
> "Present the slide about experiences, focusing on the interactive development aspect"

## Advanced Usage

### 1. Custom Slide Configuration

Edit `slides.edn`:
```clojure
{:slides ["slides/intro.md"
          "slides/features.md"
          "slides/demo.md"
          "slides/conclusion.md"]}
```

### 2. Custom Voice Settings

Modify voice parameters in audio generation:
```clojure
;; Available voices: "alloy", "echo", "fable", "onyx", "nova", "shimmer"
;; Available models: "tts-1" (faster), "tts-1-hd" (higher quality)

(ai-presenter.audio-generation/generate-slide-audio!+
 "slide-name"
 "Your script"
 {:voice "echo"
  :model "tts-1"})
```

### 3. Interactive Development

#### Connect to Joyride REPL
1. Install Calva extension
2. Run command: `Calva: Start Joyride REPL and Connect`
3. Now you can evaluate code interactively

#### Live Coding Examples
```clojure
;; Check current state
@next-slide/!state

;; Get current slide info
(p/let [current-slide (next-slide/get-current-slide-name+)]
  (println "Current slide:" current-slide))

;; Check audio system status
(p/let [status (ai-presenter.audio-playback/get-audio-status!+)]
  (println "Audio status:" status))
```

## Troubleshooting

### Common Issues

#### "OPENAI_API_KEY not found"
**Solution:** Ensure environment variable is set:
```bash
# Check if key is set
echo $OPENAI_API_KEY

# Set key (replace with your key)
export OPENAI_API_KEY="sk-..."

# Restart VS Code after setting
```

#### Audio doesn't play
**Checklist:**
1. Is the Audio Service webview open?
2. Have you clicked "Enable Audio" in the webview?
3. Does the audio file exist in `slides/voice/`?
4. Check browser console in webview for errors

```clojure
;; Debug audio status
(p/let [status (ai-presenter.audio-playback/get-audio-status!+)]
  (cljs.pprint/pprint status))
```

#### Slide not found
```clojure
;; List available slides
(p/let [slides (next-slide/slides-list+)]
  (println "Available slides:" slides))

;; Check slides.edn configuration
```

#### Webview issues
```clojure
;; Reinitialize audio service
(ai-presenter.audio-playback/dispose-audio-webview!)
(ai-presenter.audio-playback/init-audio-service!)
```

### Performance Tips

1. **Audio Generation**: Use `tts-1` model for faster generation during development
2. **Caching**: Generated audio files are cached in `slides/voice/`
3. **Memory**: Close unused webviews to free memory
4. **REPL**: Keep Joyride REPL connected for faster development

## Best Practices

### 1. Slide Organization
```
slides/
├── intro.md              # Main slide content
├── intro-notes.md        # Presenter notes
└── voice/
    └── intro.mp3         # Generated audio
```

### 2. Narration Scripts
- Keep narrations conversational and engaging
- Include pauses: "Let's pause here... and think about this"
- Use transitions: "Now, moving on to our next point..."
- Target 1-2 minutes per slide

### 3. Development Workflow
1. Create slide content (markdown)
2. Add presenter notes
3. Generate narration script
4. Generate audio
5. Test presentation flow
6. Iterate based on feedback

### 4. AI Prompt Engineering
- Be specific about context and audience
- Include tone and style preferences
- Reference slide content and notes
- Ask for specific duration targets

## Integration with Other Tools

### VS Code Extensions
- **Markdown All in One**: Enhanced markdown editing
- **Better Comments**: Organize code comments
- **GitLens**: Git integration
- **Thunder Client**: API testing (for OpenAI)

### External APIs
- **OpenAI**: Currently used for TTS
- **ElevenLabs**: Alternative TTS (requires code modification)
- **Azure Cognitive Services**: Another TTS option

### Export Options
- Audio files (MP3) can be used in other presentation tools
- Markdown slides compatible with most markdown processors
- Slide content can be exported to various formats

## Contributing

### Development Setup
1. Fork the repository
2. Open in VS Code with Joyride installed
3. Connect to Joyride REPL
4. Make changes and test interactively
5. Submit pull request

### Code Style
- Follow Clojure naming conventions
- Use pure functions where possible
- Handle errors gracefully
- Add docstrings to public functions
- Include REPL comment blocks for testing

Happy presenting! 🎉
