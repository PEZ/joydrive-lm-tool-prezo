# Audio Playback User Gesture Bug Report

**Date:** June 4, 2025

## Problem Summary

The Joyride audio playback system in VS Code was failing to play audio automatically, even after the user had completed the required gesture to enable audio. The system would report success, but the browser would block playback with the error: `Audio play() failed: play() can only be initiated by a user gesture.`

### Root Cause

The problem was a classic web audio API timing issue:
- The user gesture (click) was captured, but the actual `audio.play()` call was made asynchronously after awaiting a promise (waiting for audio to load).
- Browser security requires that `audio.play()` be called **directly within the same JavaScript execution context** as the user gesture, not after an async/promise boundary.
- As a result, playback was blocked even though the UI and status reported success.

## Solution

Two key changes were made to `.joyride/resources/audio-service.html`:

1. **Auto-play on Audio Ready:**
   - In the `canplay` event handler, if the user gesture was already completed, playback is triggered immediately when the audio is ready.
2. **Auto-play on User Gesture:**
   - In the `enableAudio()` function, if the audio is already loaded when the user enables audio, playback is triggered immediately.

This ensures that `audio.play()` is always called within the user gesture context, regardless of whether the audio loads before or after the gesture.

## Why This Works

- The fix guarantees that playback is always initiated synchronously with a user gesture, satisfying browser security requirements.
- The system now works reliably whether the user enables audio before or after the audio is loaded.

## Key Takeaway

**User gesture context cannot be preserved across async/promise boundaries for web audio playback.** All playback logic must be triggered directly within the user gesture event handler or immediately when both gesture and audio readiness are true.

---

*Prepared by GitHub Copilot, June 2025*
