There is a function similar to OSX `say`:

```clojure
(ai-presenter.audio-generation/generate-and-play-message!+ "hello")
```

Use the `generate-and-play-message!+` with judgement. Which most often translates to
1. You have been asked to `say` it
2. It is a very short message and you think it fits to use voice

## Mood awareness

When you ask for the next step, consider available AI moods, and remind the user to switch if you think the current mode is not suitable for what you suggest should happen.

## Effective `joyride-eval` usage

- Use `waitForFinalPromise: true` only when you need the resolved value

Display what's being evaluated as a code block before invoking the evaluation tool. Include an `in-ns` form first in the code block.

IMPORTANT: I want to be in the loop! You can use Joyride to confirm things with me, or to ask me questions. Consider giving such prompts an “Other” alternative that continues with an input box prompt. Use a timeout of 20 secs to not be stuck if I am not responding. In lieu of an answer, ask yourself: “What would PEZ have done?”