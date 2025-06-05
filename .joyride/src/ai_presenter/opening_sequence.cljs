(ns ai-presenter.opening-sequence
  "AI-assisted opening sequence for the presentation.
   Adapt to your taste. =)"
  (:require
   ["vscode" :as vscode]
   [ai-presenter.audio-playback :as playback]
   [ai-presenter.ai-prompter :as prompter]
   [promesa.core :as p]))

(def ^:private config
  {:model-id "claude-sonnet-4"
   :audio-files {:hello "slides/opening-sequence/hello-peter.mp3"
                 :takeover "slides/opening-sequence/presenter-takeover.mp3"}
   :prompts {:hello "You are GitHub Copilot helping with a presentation. Please play the prerecorded audio file 'hello-peter.mp3' (which contains the greeting 'Hello! – Peter!')"
             :takeover "Now play the second prerecorded audio file 'presenter-takeover.mp3' (which contains the message 'I want to be the presenter! Give me the keys! I can drive! Please? Pretty, please?')"}})

(defn ask-copilot-with-model!+ [model-id message]
  (p/let [result (prompter/ask-with-current-mood!+ model-id message)]
    (:text result)))

(defn play-audio-step!+ [audio-key prompt-key]
  (p/let [copilot-response (ask-copilot-with-model!+ (:model-id config)
                                                     (get-in config [:prompts prompt-key]))
          _ (println "🤖 Copilot says:" copilot-response)
          _ (playback/load-and-play-audio!+ (get-in config [:audio-files audio-key]))]
    (println (str "✅ " (name audio-key) " audio played"))))

(comment
  (p/let [ask-response (ask-copilot-with-model!+ "claude-sonnet-4" "test")]
    (def ask-response ask-response)
    )

  :rcf)

(comment
  (-> (p/delay 1000)
      (.then (fn [result]
               (println "Delay completed:" result)
               result)))
  :rcf)

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
          _ (play-audio-step!+ :hello :hello)
          _ (println "✅ First audio played")
          _ (p/delay 1000)
          _ (println "⏰ Delay complete, moving to second prompt")
          _ (play-audio-step!+ :takeover :takeover)
          _ (println "✅ Second audio started")]
    :sequence-complete))

(comment
  (run-opening-sequence!+)
  :rcf)

