# ADDENDUM: Advanced Interactive Debugging

## Narration Script

Here's a bonus power move for the true Interactive Programming enthusiasts: inline def debugging.

This is the technique that separates the interactive debugging masters from the casual users. Instead of just printing values, you capture them with inline defs, preserving their existing names and making them available for exploration.

Watch this magic: you instrument a function by adding `(def items items)` to capture the input, and `(def result result)` to capture the output. After the function runs, you can interactively explore these captured values in the REPL - seeing exactly what happened at each step.

The key insight? Only instrument the code you send to the REPL, not the file itself. This keeps your source clean while giving you superpowers during debugging.

And here's the advanced technique: targeted inline defs with conditionals. Capture only the specific data you care about, working around the "last iteration problem" by using conditions to grab exactly the transaction or data point that's causing issues.

This is Interactive Programming at its finest - turning debugging from guesswork into precise, data-driven exploration.

\newpage