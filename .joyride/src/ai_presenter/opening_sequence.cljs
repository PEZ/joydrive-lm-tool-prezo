(ns ai-presenter.opening-sequence
  "AI-assisted opening sequence for the presentation.
   Adapt to your taste. =)"
  (:require
   ["vscode" :as vscode]
   [ai-mood-selector :as mood]
   [ai-chat.prompter :as prompter]
   [promesa.core :as p]))

;; We don't actually need to use ask the AI to play the files
;; It's more of a POC for that this can be done

(def ^:private config
  {:model-id "gpt-4o-mini" ; fast and reliable for this task
   :system-prompt "Use the `joyride_evaluate_code` tool in the `user` namespace and code such as `(ai-presenter.audio-playback/load-and-play-audio!+ PATH-TO-AUDIO-FILE)` (replace `PATH-TO-AUDIO-FILE` with the path you are provided from the user)."
   :prompts {:hello "You are a GitHub Copilot agent helping with a presentation. Please use the Joyride evaluation tool to play the prerecorded audio file 'slides/opening-sequence/hello-peter.mp3'"
             :takeover "You are a GitHub Copilot agent helping with a presentation. Please use the Joyride evaluation tool to play the second prerecorded audio file 'slides/opening-sequence/presenter-takeover.mp3'"}})

(defn ask-copilot-with-model!+ [model-id system-prompt message]
  (p/let [result (prompter/ask-with-system!+
                  model-id system-prompt message)]
    result))

(comment
  (p/let [prompt (mood/get-system-prompt-for-mood+ "presenter")
          ask-response (ask-copilot-with-model!+
                        "claude-sonnet-4"
                        prompt
                        "please show the first slide and then the next, as two separate tool calls")]
    (def ask-response ask-response))

  :rcf)

(defn ask-to-play-audio!+
  "Use the real VS Code Language Model API tool execution"
  [prompt-key]
  (p/let [prompt (get-in config [:prompts prompt-key])
          system-prompt (:system-prompt config)
          result (prompter/ask-with-system!+
                  (:model-id config)
                  system-prompt
                  prompt)
          _ (println "🤖 Copilot says:" (:text result))
          _ (when-let [tools-used (:tools-used result)]
              (println "🔧 Tools used:" (pr-str tools-used)))
          _ (when-let [tool-results (:tool-results result)]
              (println "🎵 Tool execution results:")
              (doseq [tool-result tool-results]
                (println "  -" (:tool-name tool-result) "=>" (:result tool-result))))]
    (println (str "✅ " (name prompt-key) " step completed with REAL tool execution!"))))

(defn show-start-button!+ []
  (p/let [choice (.showInformationMessage (.-window vscode)
                                          "🎭 Ready for the dramatic opening sequence?"
                                          "Start"
                                          "Cancel")]
    (if (= choice "Start")
      :started
      (throw (js/Error. :cancelled)))))

(comment
  (-> (p/let [answer (show-start-button!+)]
        (println answer))
      (p/catch (fn [e]
                 (println (.-message e)))))
  :rcf)

;; TODO: Let CoPilot wing it a little with the audio responses,
;;       generating new audio on the fly

(defn run-opening-sequence!+ []
  (p/let [_ (show-start-button!+)
          _ (println "🎭 Sequence started!")
          _ (p/delay 4000)
          _ (ask-to-play-audio!+ :hello)
          _ (println "✅ First audio played")
          _ (p/delay 4000)
          _ (println "⏰ Delay complete, moving to second prompt")
          _ (ask-to-play-audio!+ :takeover)
          _ (println "✅ Second audio started")]
    :sequence-complete))

(comment
  (run-opening-sequence!+)
  :rcf)

