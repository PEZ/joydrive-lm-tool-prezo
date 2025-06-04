# AI-Powered Joyride Presentation System

A complete presentation system + custom AI workflow, built entirely in Joyride, where AI generates and narrates slide content.

**Taking this for a spin is as easy as opening this project in VS Code with CoPilot enabled**, and installing the Joyride extension.

The audio generation features need an OpenAI API key. You and CoPilot can hack it to use something else for audio generation, because ...

... This project is **100% Joyride** - The AI reads slide content, crafts engaging slide narration, generates high-quality audio from the narration scripts, and orchestrate full presentations - all through Joyride's integration with VS Code's extension API.

It is also WIP: I am going to add much, much better guidance of how to use the example.

## 🚀 Quick Start

### Core Requirements
**Only 3 things needed:**
1. **[GitHub Copilot](https://marketplace.visualstudio.com/items?itemName=GitHub.copilot)** - For AI interaction with the system
2. **[Joyride](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.joyride)** - ClojureScript scripting for VS Code (this project is 100% Joyride)
3. (for text-to-speech generation) **OpenAI API Key in your env**:
   ```bash
   export OPENAI_API_KEY="your-openai-api-key"
   ```

### Running the System

See that button in the stats bar with “🎭”? It's a system prompt/mood selector. Select the **presenter** mood. Then ask CoPilot to demo it for you and show you how to operate it. ✨ It may take some time the first time because it will need to genarate audio from the text script.

## 🎯 Core Features

### AI Workflow support

CoPilot Instructions moods supporting both VS Code hacking and presentation authoring/presenting.

Easily switch between different system prompts depending on where in the workflow you are. Available system prompts (in `prompts/system/`):

- **architect** - For system design and architectural decisions
- **joyride-hacker** - For interactive programming and VS Code hacking with Joyride
- **slide-author** - For creating and editing slide content
- **story-author** - For crafting the full narrative story
- **slide-narration-author** - For writing engaging slide narrations, based on slide content, notes and the narrative story
- **audio-generator** - Generates high quality voice audio from slide scripts
- **presenter** - For presenting slides with voice narration, can also author narration and genarate audio

#### Slideshow creation workflow/pipeline

Creating slides is a composition of human direction, AI agentic processing and Joyride scripts. The main pipeline looks something like so

```
Human has a story to share
     ↓
🤝 Human + slide-author → Draft slides & notes
     ↓ (human feedback & iteration)
📖 story-author → Retrofits with storytelling expertise
     ↓ (human feedback & story refinement)
🎙️ slide-narration-author → Crafts narration scripts
     ↓ (human feedback & narration polish)
🔊 audio-generator → Generates high-quality voice audio
     ↓ (human feedback & audio refinement)
✨ Final Presentation
```

It is the human who is responsible for the main orchestration, maintaining high flexibility. Orchestration is also part of each agent mood. At individual task level within orchestration is sometimes encoded as promise chains in Joyride scripts.

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
;; 1. Hide the chat during presentation
(vscode/commands.executeCommand "workbench.action.closeAuxiliaryBar")

;; 2. Show a specific slide
(next-slide/show-slide-by-name!+ "hello.md")

;; 3. Generate and play audio for the slide
(p/let [gen-result (ai-presenter.audio-generation/generate-slide-audio!+
                    "hello" ; slide name (without .md)
                    "Your engaging presentation script here...")]
  (ai-presenter.audio-playback/load-and-play-audio!+ "slides/voice/hello.mp3"))

;; 4. Answer questions during presentation
(ai-presenter.audio-generation/generate-and-play-message!+
 "That's a great question! Here's my answer...")

;; 5. Navigate to next slide when ready
(next-slide/next! true)
```

### AI-Assisted Presentation

Ask Copilot (with LM Tools enabled):
> "Present the slide about the LM tools for Joyride, please. I'm especially interested in vibe coding"

The AI will:
1. Navigate to the slide
2. Read slide content and notes
3. Generate a narration script, incorporating your focus
4. Create audio narration
5. Play the audio
6. Wait for your next instruction

(Or, the AI should do the authoring and generation steps. But most often it just plays whatever audio is already generated for the slide.)

## 🚀 Extension Ideas

- **Multiple Voices** - Different narrators for different slide types
- **Interactive Elements** - Audience participation through VS Code commands

## 📖 Learning Resources

- [Joyride Documentation](https://github.com/BetterThanTomorrow/joyride)
- [Interactive Programming with Clojure](https://clojure.org/guides/repl/introduction)
- [VS Code Extension API](https://code.visualstudio.com/api)

## 🤝 Contributing

This project demonstrates Interactive Programming principles built entirely in **Joyride ClojureScript**.

### For Effective Development
For the most productive experience, both human and AI should be connected to the Joyride REPL:

**Human setup:**
- Install [Calva](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva) a Clojure extension
- Command: `Calva: Start Joyride REPL and Connect`

**AI setup:**

- With Joyride installed, there is no AI setup.

### Development Workflow
1. Joyride automatically gives
1. Start the Joyride REPL
2. Enable AI access to the REPL (via Backseat Driver)
3. Experiment with functions interactively
4. Build upon the existing system
5. Share your enhancements

**Both intelligences working together in the same REPL = Interactive Programming magic!** ✨

**Happy Interactive Programming!** 🎉

---

*Built with 💜 by the Clojure community, powered by the magic of Interactive Programming*
