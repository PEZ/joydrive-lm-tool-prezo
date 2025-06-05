(ns ai-presenter.opening-sequence
  "AI-assisted opening sequence for the presentation.
   Adapt to your taste. =)"
  (:require
   ["vscode" :as vscode]
   [ai-presenter.audio-playback :as playback]
   [clojure.string :as str]
   [promesa.core :as p]))

(def ^:private config
  {:model-id "claude-sonnet-4"
   :audio-files {:hello "slides/opening-sequence/hello-peter.mp3"
                 :takeover "slides/opening-sequence/presenter-takeover.mp3"}
   :prompts {:hello "You are GitHub Copilot helping with a presentation. Please play the prerecorded audio file 'hello-peter.mp3' (which contains the greeting 'Hello! – Peter!')"
             :takeover "Now play the second prerecorded audio file 'presenter-takeover.mp3' (which contains the message 'I want to be the presenter! Give me the keys! I can drive! Please? Pretty, please?')"}})

(defn get-iterator
  "Extract an async iterator from a VS Code Language Model response."
  [response]
  (let [text-iter (.-text response)
        iter-fn (aget text-iter js/Symbol.asyncIterator)
        iterator (.call iter-fn text-iter)]
    (println "Iterator created:" iterator)
    iterator))

(defn collect-all-chunks
  "Recursively collect all chunks from an async iterator until done."
  [iterator]
  (p/let [result (.next iterator)]
    (if (.-done result)
      []
      (p/let [remaining (collect-all-chunks iterator)]
        (cons (.-value result) remaining)))))

(defn get-model-by-id!+ [model-id]
  (-> (vscode/lm.selectChatModels #js {:vendor "copilot"})
      (.then (fn [models]
               (let [model (->> models
                                (filter #(= (.-id %) model-id))
                                first)]
                 (if model
                   (do
                     (println (str "✅ Selected model: " (.-name model) " (" model-id ")"))
                     model)
                   (throw (js/Error. (str "❌ Model not found: " model-id)))))))))

(defn- model->quick-pick-item [model]
  #js {:label (str (.-name model) " (" (.-id model) ")")
       :description (str "Max tokens: " (.-maxInputTokens model))
       :id (.-id model)})

(defn pick-model!+ []
  (p/let [models (vscode/lm.selectChatModels #js {:vendor "copilot"})
          items (map model->quick-pick-item models)
          selected-item (vscode/window.showQuickPick
                         (clj->js items)
                         #js {:placeHolder "Select a language model"
                              :canPickMany false})]
    (if selected-item
      (let [model-id (.-id selected-item)]
        (println "Selected model:" model-id)
        model-id)
      (throw (js/Error. "No model selected")))))

(comment
  (-> (pick-model!+)
      (.then (fn [model-id]
               (ask-copilot-with-model!+ "hello, greet the audience, please =) " model-id)))
      (.then (fn [response]
               (def response response)
               (println "Model response:" response)
               (vscode/showInformationMessage
                (str "🤖 " response))
               response)))
  :rcf)

(defn- response->text!+ [response]
  (p/let [iterator (get-iterator response)
          chunks (collect-all-chunks iterator)]
    (str/join "" chunks)))

(defn ask-copilot-with-model!+ [model-id message]
  (p/let [model (get-model-by-id!+ model-id)
          response (.sendRequest model #js [#js {:role "user" :content message}])
          text (response->text!+ response)]
    text))

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

