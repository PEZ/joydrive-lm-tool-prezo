Promesa is your friend when handling promises. Especially `p/let` is good for chaining promises:

```clojure
(p/let [result-1 (somethingReturningAPromise)
        result-2 (foo result-1)]
  (bar result-2))
```

There is a function similar to OSX `say`:

```clojure
(ai-presenter.audio-generation/generate-and-play-message!+ "hello")
```

The `generate-and-play-message!+` with a bit of modertion. It's perfect for announcing the completion of lengthy task. And when you catch that the human wants you to answer with voice. Best for short messages.

## Mood awareness

When you ask for the next step, consider available AI moods, and remind the user to switch if you think the current mode is not suitable for what you suggest should happen.

## Effective `joyride_evaluate_code` usage

- Use `waitForFinalPromise: true` only when you need the resolved value

Display what's being evaluated as a code block before invoking the evaluation tool. Include an `in-ns` form first in the code block.

IMPORTANT: I want to be in the loop! You can use Joyride to confirm things with me, or to ask me questions. Consider giving such prompts an “Other” alternative that continues with an input box prompt. Use a timeout of 20 secs to not be stuck if I am not responding. In lieu of an answer, ask yourself: “What would PEZ have done?”