(ns ai-presenter.opening-sequence
  "Opening sequence for the presentation.
   Adapt to your taste. =)"
  (:require
   ["vscode" :as vscode]
   [ai-presenter.audio-playback :as audio-playback]
   [promesa.core :as p]))

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

(defn run-opening-sequence!+ []
  (p/let [_ (show-start-button!+)
          _ (println "🎭 Sequence started!")
          _ (p/delay 4000)
          _ (println "✅ Hello audio started")
          _ (audio-playback/load-and-play-audio!+ "slides/opening-sequence/hello-peter.mp3")
          _ (p/delay 4000)
          _ (println "⏰ Delay complete, playing takeover file")
          _ (audio-playback/load-and-play-audio!+ "slides/opening-sequence/presenter-takeover.mp3")]
    :sequence-complete))

(comment
  (run-opening-sequence!+)
  :rcf)

