# AI-Powered Joyride Presentation System

A complete presentation system built entirely in Joyride (ClojureScript for VS Code) where AI generates and narrates slide content.

**Taking this for a spin is as easy as opening this project in VS Code with CoPilot enabled**, and installing the Joyride extension.

The audio generation features need an OpenAI API key. You and CoPilot can hack it to use something else for audio generation, because ...

.. This project is **100% Joyride** - demonstrating the power of **Interactive Programming** by building a complete AI presentation system using ClojureScript that runs directly within VS Code. The AI can read slide content, craft engaging scripts, generate high-quality audio narration, and orchestrate full presentations - all through Joyride's seamless integration with VS Code's extension API.

## ✨ What This System Does

- 📖 **Smart Script Generation** - AI reads markdown slides and crafts engaging presentation scripts
- 🎵 **Audio Synthesis** - Converts scripts to professional-quality speech using OpenAI TTS
- 🎯 **Intelligent Targeting** - Scripts specifically crafted for VS Code users with Interactive Programming hooks
- ⏱️ **Timing Management** - Estimates audio duration and waits for completion before advancing
- 🖥️ **VS Code Integration** - Complete webview-based audio playback with browser security compliance
- 🔄 **Live Development** - Build and test the system interactively while using it

## 🚀 Quick Start

### Core Requirements
**Only 3 things needed:**
1. **[Joyride](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.joyride)** - ClojureScript scripting for VS Code (this project is 100% Joyride)
2. **[GitHub Copilot](https://marketplace.visualstudio.com/items?itemName=GitHub.copilot)** - For AI interaction with the system
3. (for text-to-speech generation) **OpenAI API Key**:
   ```bash
   export OPENAI_API_KEY="your-openai-api-key"
   ```

### Running the System

Ask CoPilot to demo it for you and show you how to operate it. 😀

## 🎯 Core Features

### 📝 **Intelligent Script Generation**
The AI reads slide content and presentation notes to create engaging scripts that:
- Target VS Code users specifically
- Build excitement about Interactive Programming
- Use conversational, enthusiastic tone
- Include practical examples and hooks

### 🎙️ **Professional Audio Generation**
- High-quality text-to-speech using OpenAI's API
- Automatic file management in `slides/voice/`
- Duration estimation for proper timing
- Support for multiple presentation styles

### 🎮 **Interactive Presentation Control**
- Slide navigation with keyboard shortcuts (`j`/`k`, `→`/`←`)
- Webview-based audio playback with security compliance
- User gesture handling for browser audio policies
- Real-time feedback and status updates

### 🤖 **AI-Driven Workflow**
- AI can read slides, generate scripts, and orchestrate presentations
- REPL-driven development for live system modification
- Interactive Programming paradigm demonstration
- Seamless integration with VS Code's extension ecosystem

## 📁 Project Structure

```
├── slides/                    # Markdown presentation slides
│   ├── *.md                  # Individual slides with HTML/CSS
│   ├── *-notes.md            # Presentation guidance for each slide
│   └── voice/                # Generated audio files
├── .joyride/src/
│   ├── next_slide.cljs       # Slide navigation system
│   └── ai_presenter/         # AI presentation system
│       ├── audio_generation.cljs  # TTS integration
│       ├── audio_playback.cljs    # Webview audio player
│       ├── core.cljs             # State management
│       └── presentation.cljs     # High-level workflows
├── .joyride/resources/
│   └── audio-service.html    # Audio playback webview
└── docs/
    └── PROJECT_SUMMARY.md    # Detailed technical documentation
```

## 🎬 Example Usage

### Basic Presentation
```clojure
;; Generate audio for a slide
(ai-presenter.audio-generation/generate-slide-audio!+
  "hello"
  "Welcome to the exciting world of Interactive Programming!")

;; Present with full workflow
(ai-presenter.presentation/present-slide!+
  0 "hello" "Your engaging script here...")
```

### AI-Assisted Presentation
Ask Copilot (with LM Tools enabled):
> "Present the slide about the LM tools for Joyride, please. I'm especially interested in vide coding"

The AI will:
1. Navigate to the slide
2. Read slide content and notes
3. Generate a narration script, incorporating your focus
4. Create audio narration
5. Play the audio
6. Wait for your next instruction

## 🧠 Interactive Programming Paradigm

This project showcases **Interactive Programming** - the ability to modify and extend your development environment while using it:

- **Live System Modification** - Change presentation behavior without restarting
- **REPL-Driven Development** - Test and iterate on features immediately
- **AI Code Execution** - AI can directly interact with VS Code through Joyride
- **Real-time Feedback** - See results instantly as you develop

## 🔧 Development Workflow

1. **REPL-First Development** - All functions can be tested interactively
2. **Pure Functions** - Core logic separated from side effects for easy testing
3. **Promise-Based Async** - Clean async workflows with `promesa.core`
4. **State Management** - Functional state transitions with data-oriented design

## 🚀 Extension Ideas

- **Multiple Voices** - Different narrators for different slide types
- **Background Music** - Ambient audio during presentations
- **Interactive Elements** - Audience participation through VS Code commands
- **Recording Capabilities** - Save complete presentations for later playback
- **Multi-modal Content** - Generate visuals alongside audio

## 📖 Learning Resources

- [Joyride Documentation](https://github.com/BetterThanTomorrow/joyride)
- [Interactive Programming with Clojure](https://clojure.org/guides/repl/introduction)
- [VS Code Extension API](https://code.visualstudio.com/api)

## 🤝 Contributing

This project demonstrates Interactive Programming principles built entirely in **Joyride ClojureScript**.

### For Effective Development
For the most productive experience, both human and AI should be connected to the Joyride REPL:

**Human setup:**
- Install [Calva](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva) for excellent Clojure development
- Command: `Calva: Start Joyride REPL and Connect`

**AI setup:**
- Install [Backseat Driver](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva-backseat-driver) for LM Tool integration
- Configure Backseat Driver to expose the REPL tool in VS Code Settings
- This enables AI to execute Joyride code directly

### Development Workflow
1. Start the Joyride REPL (with Calva for best experience)
2. Enable AI access to the REPL (via Backseat Driver)
3. Experiment with functions interactively
4. Build upon the existing system
5. Share your enhancements

**Both intelligences working together in the same REPL = Interactive Programming magic!** ✨

**Happy Interactive Programming!** 🎉

---

*Built with 💜 by the Clojure community, powered by the magic of Interactive Programming*
