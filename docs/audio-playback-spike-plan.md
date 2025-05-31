# Audio Playback Control Spike Plan

## 🎯 Spike Scope: Audio Playback Control System

**Goal:** Figure out how to control audio playback (play/pause/resume/stop) from Joyride, and build a robust system for managing audio sessions.

## Background

This spike builds on our successful [Audio Generation MVP](./audio-generation-mvp-plan.md). We now have perfect audio file generation working, and need to explore how to control playback within VS Code through Joyride.

The [main plan](./AI-PRESENTER-PLAN.md#phase-0-spikes) identified this as Phase 0 Spike #2: "Figure out how to control audio play back from Joyride, play/pause/resume/stop"

## Research Questions

### Primary Questions (Must Answer)
1. **VS Code/Electron/Node.js Audio Control Options:**
   - Are there VS Code APIs for programmatic audio playback control?
   - What Node.js built-in modules support audio with play/pause/resume/stop?
   - What npm modules provide full programmatic audio control in Electron/Node environment?
   - Can we leverage Electron's main process audio capabilities?

2. **Programmatic Control Requirements:**
   - Can we get programmatic handles for play/pause/resume/stop operations?
   - How do we detect audio completion events programmatically?
   - Can we prevent multiple audio sessions programmatically (not relying on user)?
   - Can we control playback position, volume, speed programmatically?

3. **Integration with Joyride/ClojureScript:**
   - How do chosen audio solutions integrate with Joyride's ClojureScript environment?
   - What's the interop story for audio control from ClojureScript?
   - Can we maintain functional, data-oriented design patterns?

### Secondary Questions (Fallback Options)
4. **Browser Tech Fallback:**
   - If native options fail, how do HTML5 Audio elements work in VS Code's webview?
   - Can we get programmatic control through webview messaging?
   - What are the limitations of browser-based audio in this context?

## Spike Implementation Plan

### Step 1: Native Audio Control Investigation
- **Priority 1**: Research VS Code extension APIs for audio control
- **Priority 2**: Test Node.js built-in audio capabilities (`child_process` with system audio tools?)
- **Priority 3**: Explore npm modules: `node-wav-player`, `speaker`, `audio-play`, etc.
- **Priority 4**: Investigate Electron main process audio control options
### Step 2: Programmatic Control Testing
- Test each viable option with actual play/pause/resume/stop operations
- Measure control responsiveness and reliability
- Test audio completion event detection
- Validate session management capabilities

### Step 3: Joyride Integration Assessment
- Test ClojureScript interop with chosen audio solutions
- Prototype data-oriented API design
- Test error handling and cleanup scenarios
- Document integration patterns and constraints

### Step 4: Fallback Option Evaluation
- **Only if native options fail**: Test HTML5 Audio in VS Code webview
- Compare programmatic control capabilities
- Document trade-offs and limitations

## Success Criteria for Spike

✅ **Native Audio Control**: Identify working VS Code/Node.js/npm solution with full programmatic control
✅ **Complete Control Set**: Verified play/pause/resume/stop operations work reliably
✅ **Event Detection**: Can programmatically detect audio completion and errors
✅ **Session Management**: Can prevent multiple audio chaos programmatically
✅ **Joyride Integration**: ClojureScript interop works smoothly with chosen solution
✅ **Performance Validation**: Controls are responsive enough for presenter mode
✅ **Technical Decision**: Clear recommendation on best approach with justification

## Spike Approach

### REPL-Driven Investigation
- Use existing generated audio files for testing
- Build understanding step by step in the REPL
- Document findings with inline defs for easy result examination
- Test edge cases and error conditions

### Data-Oriented Discovery
- Focus on what data structures we need for audio session tracking
- Identify pure vs. side-effect functions needed
- Design for functional composition and testability

## Implementation File

```
.joyride/src/ai_presenter/
└── audio_playback_spike.cljs  # Exploration and findings
```

## Out of Scope for Spike

- Full presenter mode integration (comes after spike)
- UI controls and status indicators (presenter mode concern)
- Batch audio processing (generation MVP handles this)
- Audio generation (already complete)

## Expected Outcomes

1. **Primary Recommendation**: Best native audio control solution for VS Code/Electron/Node.js environment
2. **API Design**: Proposed function signatures for programmatic audio control system
3. **Technical Constraints**: Documented limitations and required workarounds
4. **Integration Pattern**: How to connect with Joyride/ClojureScript effectively
5. **Performance Profile**: Control responsiveness and reliability characteristics
6. **Fallback Strategy**: Browser-based option if native approaches fail

---

## 🎉 SPIKE RESULTS: BREAKTHROUGH ACHIEVED!

**Date Completed:** May 31, 2025  
**Result:** ✅ COMPLETE SUCCESS - Full programmatic audio control achieved!

### 🏆 Key Findings

**WINNING SOLUTION: VS Code Webview with HTML5 Audio + Resource Scheme**

After extensive investigation, the breakthrough solution combines:
- **VS Code Webview** with `retainContextWhenHidden: true` for persistent background operation
- **HTML5 Audio Element** for reliable cross-platform audio playback
- **VS Code Resource Scheme** (`.asWebviewUri()`) for secure local file access
- **Message Passing API** for bidirectional Joyride ↔ Webview communication

### 🔬 Investigation Journey & Key Insights

1. **Native Node.js Audio Libraries FAILED**
   - `sounds-control` npm package: Requires browser environment (window object)
   - Node.js built-ins: No direct audio playback capabilities
   - Electron main process: Complex, over-engineered for our needs

2. **Browser Environment BREAKTHROUGH**
   - VS Code webviews provide full browser environment within VS Code
   - HTML5 audio elements work perfectly in webview context
   - User gesture requirement satisfied with one-time button click

3. **Local File Access SOLVED**
   - VS Code webview resource scheme enables secure local file access
   - `localResourceRoots` + `.asWebviewUri()` transforms local paths to webview-accessible URLs
   - No file serving or HTTP endpoints needed

4. **Background Operation SUCCESS**
   - `retainContextWhenHidden: true` keeps webview alive when hidden
   - Audio continues playing during slide navigation
   - Full programmatic control maintained while webview is backgrounded

5. **Message Passing PERFECTED**
   - Bidirectional communication via `webview.postMessage()` and `window.addEventListener('message')`
   - ClojureScript data → JavaScript conversion with `clj->js`
   - Real-time command execution (play/pause/stop/load)

### 📋 Reproduction Recipe

**Prerequisites:**
- Joyride extension active in VS Code
- Audio file available (e.g., `slides/voice/test-playback.mp3`)

**Step-by-Step POC:**

1. **Install sounds-control (for learning, though not used in final solution):**
   ```bash
   npm install sounds-control
   ```

2. **Create spike file:**
   ```
   .joyride/src/ai_presenter/spikes/audio_playback_sounds_control_spike.cljs
   ```

3. **Core webview creation with local resource access:**
   ```clojure
   (defn create-webview-with-local-resource-access! []
     (let [panel (vscode/window.createWebviewPanel
                  "audioControl" "Audio Control (Local Resources)"
                  vscode/ViewColumn.One
                  (clj->js {:enableScripts true
                           :retainContextWhenHidden true
                           :localResourceRoots [(vscode/Uri.file "/path/to/project")]}))
           webview (.-webview panel)
           audio-uri (.asWebviewUri webview (vscode/Uri.file "/path/to/audio.mp3"))]
       ;; HTML with audio element and message passing...
       ))
   ```

4. **Key HTML5 audio setup:**
   ```html
   <audio id='audioPlayer' preload='metadata'>
     <source src='WEBVIEW_RESOURCE_URI' type='audio/mpeg'>
   </audio>
   ```

5. **Programmatic control functions:**
   ```clojure
   (defn send-audio-command! [command-type]
     (.postMessage (:webview @!audio-webview) (clj->js {:type command-type})))
   
   (defn play-audio! [] (send-audio-command! "play-audio"))
   (defn pause-audio! [] (send-audio-command! "pause-audio"))
   (defn stop-audio! [] (send-audio-command! "stop-audio"))
   ```

6. **Testing sequence:**
   - Create webview: `(create-webview-with-local-resource-access!)`
   - User clicks "Enable Audio" button (satisfies browser security)
   - Test background control: Show slide, then `(play-audio!)` `(pause-audio!)`
   - ✅ Audio plays/pauses while webview is hidden!

### 🎯 Technical Validation

**ALL SUCCESS CRITERIA MET:**

✅ **Complete Control Set**: play/pause/resume/stop operations work reliably  
✅ **Local File Access**: VS Code resource scheme enables secure local audio loading  
✅ **Background Operation**: Audio control works while webview is hidden during presentations  
✅ **Event Detection**: Audio completion, error, and state events captured  
✅ **Session Management**: Single audio instance, no chaos from multiple sessions  
✅ **Joyride Integration**: Smooth ClojureScript interop via message passing  
✅ **Performance**: Instantaneous control response, no perceptible latency  
✅ **Browser Security Compliance**: One-time user gesture enables all future programmatic control  

### 🚀 Production-Ready Architecture

**Data Flow:**
```
Joyride/ClojureScript → Message Passing → Hidden Webview → HTML5 Audio → Local Files
```

**Key Components:**
- `!audio-webview` atom for webview state management
- Message passing functions for command dispatch
- HTML5 audio element with comprehensive event handling
- VS Code resource scheme for secure file access

**Integration Points:**
- Can integrate with existing `next-slide` presentation system
- Ready for AI-generated audio file playback
- Supports multiple audio files per presentation
- Background operation during slide navigation

### 🎊 Bottom Line

**WE CRACKED IT!** Full programmatic audio control in VS Code/Joyride is not only possible, but elegantly achievable using VS Code's own webview system. This solution is:

- **Production-ready** - Robust, reliable, and performant
- **Security-compliant** - Satisfies browser audio restrictions  
- **Integration-friendly** - Clean API for presentation system
- **Scalable** - Supports complex audio scenarios

**NO BLOCKERS REMAIN** for building the AI presenter system with synchronized audio! 🎉

## Next Steps After Spike

1. ✅ **SPIKE COMPLETE** - All technical unknowns resolved
2. 🚀 **Build Production Audio Service** - Clean API based on spike findings  
3. 🎵 **Integrate with Presentation System** - Connect with `next-slide` navigation
4. 🎤 **Enable AI-Generated Audio** - Connect with audio generation pipeline
5. 📢 **Full AI Presenter Mode** - Complete synchronized presentation experience
