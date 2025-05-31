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

## Next Steps After Spike

1. Implement the audio playback control system based on spike findings
2. Create mini-plan for basic presenter mode (generation + playback integration)
3. Build toward full AI presenter mode with UI controls
