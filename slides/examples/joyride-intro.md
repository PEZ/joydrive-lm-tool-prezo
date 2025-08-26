<div class="slide">

# Joyride: Make it _your_ editor

<div class="row">
<div class="column col-7">

<div style="display: flex; align-items: center; gap: 30px; margin-bottom: 40px;">
<img src="../images/joyride-logo.png" alt="Joyride Logo" style="max-height: 150px;" />
<i class="fa fa-plus" style="font-size: 2rem; opacity: 0.6;"></i>
<img src="../images/vscode.png" alt="VS Code Logo" style="max-height: 150px;" />
</div>

- **User-Space Scripting** - Extend without plugins
- **Live Development** - Modify while you use it
- **ClojureScript Power** - REPL-driven workflow
- **Full VS Code API** - Access to everything

</div>

<div class="column col-5">
<div style="background-color: rgba(0,0,0,0.05); padding: 20px; border-radius: 10px; margin-top: 20px;">
<pre style="font-size: 0.9rem;">

```clojure
(ns user
  (:require [joyride.core :as j]
            [promesa.core :as p]))

;; Live-hack VS Code!
(p/let [editor (j/current-editor)]
  (j/show-message
    (str "Editing "
         (.-fileName editor))))
```
<div class="center" style="margin-top: 30px;">
<i class="fa fa-magic" style="font-size: 3rem; opacity: 0.7;"></i>
</div>
</div>
</div>

</div>
