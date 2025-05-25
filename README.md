# Can we make CoPilot do Interactive programming?

Slides for my presentation at [Scicloj AI meetup May 24 2025](https://clojureverse.org/t/scicloj-ai-meetup-7-can-we-turn-copilot-into-an-interactive-programmer)

The recording:
* https://www.youtube.com/watch?v=kmW804dNqgA
A shorter demo of when CoPilot extends VS Code with features:
* https://www.youtube.com/watch?v=W7CR-r8XRgE

To try repeating that part of the demo yourself:

0. Install [Joyride](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.joyride) and [Backseat Driver](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva-backseat-driver) in VS Code
1. Open this project in VS Code
   * You should see a message that `next-slide` is activated
1. Put CoPilot in Agent mode
   * You should see that it has all Backseat Driver tools except the REPL (Evaluate Clojure Code)
1. Configure Backseat Driver to expose the REPL tool (from VS Code Settings)
   * You should see that CoPilot now has access to this tool
1. Start the Joyride REPL (there's a command for this: **Calva: Start Joyride REPL and Connect**)

Then you can ask CoPilot to show you the first (or any) slide, and can take it from there. Have fun!
