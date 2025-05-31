# Audio Generation MVP Plan

## 🎯 MVP Scope: Perfect Single Audio Generation

**Goal:** Create a robust audio generation system that can produce perfect audio files from provided scripts, one at a time.

## Background

This mini-plan is derived from the comprehensive [AI Presenter Mode Implementation Plan](./AI-PRESENTER-PLAN.md#phase-0-spikes). The audio generation spike (Phase 0) has already confirmed that:

- `ai-text-to-speech` module works successfully with Joyride
- Files are created with auto-generated names (uncontrollable by us)
- File path is returned, allowing us to move files to correct naming convention
- OpenAI API integration is functional

## Core Implementation: Audio Generation

### 1. Leverage the Successful Spike Results
- The spike confirmed `ai-text-to-speech` works with Joyride
- Files are created with auto-generated names (uncontrollable)
- File path is returned, so we can move files to correct naming convention
- Validate `OPENAI_API_KEY` exists before attempting generation

### 2. Build Complete Audio Generation Function
- Take slide name + script text as input
- Generate MP3 using `ai-text-to-speech` in temp location
- Move generated file to `slides/voice/{slide-name}.mp3`
- Handle file naming, directory creation, cleanup
- Robust error handling and validation

### 3. File Management System
- Ensure `slides/voice/` directory exists
- Handle file collisions (overwrite? prompt?)
- Clean up temporary files on success/failure
- Return detailed success/failure status

## Success Criteria

✅ **Environment Validation**: Confirms `OPENAI_API_KEY` exists before attempting generation
✅ **Perfect Audio Quality**: Generated MP3 files have clear, professional voice quality
✅ **Reliable File Management**: Files consistently appear in `slides/voice/` with correct naming (`{slide-name}.mp3`)
✅ **Robust Error Handling**: Clear error messages for API failures, file system issues, missing scripts
✅ **No Leftover Artifacts**: Temporary files are properly cleaned up on both success and failure
✅ **Validation**: Won't attempt generation without valid inputs (slide name, script text, API key)

## Implementation File

```
.joyride/src/ai_presenter/
└── audio_generation.cljs  # Complete audio generation system
```

## Out of Scope (For Later)

- Audio playback control (needs separate spike)
- Integration with slideshow navigation
- UI controls and status indicators
- Real-time presenter mode
- Batch processing of multiple slides
- Automatic script generation from slide content (AI-powered)

## Next Steps

1. Implement the core audio generation function in `audio_generation.cljs`
2. Test with sample scripts to validate success criteria
3. Spike audio playback control system
4. Plan integration with existing slideshow system
