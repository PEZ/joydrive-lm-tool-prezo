# Audio Playback Control Spike Plan

## 🎯 Spike Scope: Audio Playback Control System

**Goal:** Figure out how to control audio playback (play/pause/resume/stop) from Joyride, and build a robust system for managing audio sessions.

## Background

This spike builds on our successful [Audio Generation MVP](./audio-generation-mvp-plan.md). We now have perfect audio file generation working, and need to explore how to control playback within VS Code through Joyride.

The [main plan](./AI-PRESENTER-PLAN.md#phase-0-spikes) identified this as Phase 0 Spike #2: "Figure out how to control audio play back from Joyride, play/pause/resume/stop"

## Research Questions

### Primary Questions (Must Answer)
1. **How does VS Code/Joyride handle audio playback?**
   - Can we use `vscode.env.openExternal` with file URIs?
   - Do we need HTML5 Audio elements?
   - What about native system audio controls?

2. **How do we get playback control handles?**
   - Can we get references to pause/resume/stop the audio?
   - How do we detect when audio finishes playing?
   - How do we prevent multiple audio files playing simultaneously?

3. **What's the user experience like?**
   - Does audio play in system default player or in VS Code?
   - Can we control volume, speed, position?
   - How responsive are the controls?

### Secondary Questions (Nice to Know)
4. **Integration points:**
   - How do we hook into audio completion events?
   - Can we get playback progress/timing information?
   - What happens if user manually closes audio player?

## Spike Implementation Plan

### Step 1: Basic Playback Exploration
- Test different audio playback approaches with existing generated files
- Document what works, what doesn't, and UX implications
- Test with MP3 files in `slides/voice/` directory

### Step 2: Control Interface Discovery  
- Experiment with pause/resume/stop mechanisms
- Test audio session management (prevent chaos of multiple audio)
- Document control limitations and capabilities

### Step 3: Event Handling Investigation
- Research audio completion detection
- Test cleanup and error scenarios
- Document integration patterns for auto-advance functionality

### Step 4: Design Recommendations
- Propose data structures for audio session management
- Recommend control patterns based on findings
- Identify constraints and workarounds needed

## Success Criteria for Spike

✅ **Basic Playback**: Can reliably play MP3 files from `slides/voice/` directory  
✅ **Control Discovery**: Understand available control mechanisms (play/pause/stop)  
✅ **Session Management**: Can prevent multiple audio files playing simultaneously  
✅ **Completion Detection**: Can detect when audio finishes (for auto-advance)  
✅ **Error Handling**: Understand failure modes and cleanup requirements  
✅ **UX Documentation**: Clear understanding of user experience implications  
✅ **Integration Pattern**: Recommended approach for presenter mode integration  

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

1. **Technical Recommendations**: Best approach for audio playback in Joyride/VS Code
2. **API Design**: Proposed function signatures for audio control system
3. **Data Models**: Structures needed for session management and state tracking
4. **Integration Guidelines**: How to connect with slideshow navigation
5. **Constraint Documentation**: Known limitations and workarounds

## Next Steps After Spike

1. Implement the audio playback control system based on spike findings
2. Create mini-plan for basic presenter mode (generation + playback integration)
3. Build toward full AI presenter mode with UI controls
