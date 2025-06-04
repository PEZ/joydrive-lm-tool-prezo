(ns ai-presenter.opening-sequence
  "AI-assisted opening sequence for the presentation.

   This namespace provides functionality to create an interactive opening
   where GitHub Copilot 'interrupts' the human presenter and offers to
   collaborate on the presentation.
   Adapt to your taste. =)"
  (:require
   ["vscode" :as vscode]
   [ai-presenter.audio-playback :as playback]
   [clojure.string :as str]
   [promesa.core :as p]))

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
  (let [chunks (atom [])]
    (letfn [(read-next []
              (-> (.next iterator)
                  (.then (fn [result]
                           (if (.-done result)
                             @chunks
                             (do
                               (swap! chunks conj (.-value result))
                               (read-next)))))))]
      (read-next))))

;; Function to list all available models with their details
(defn list-models!+ []
  (-> (vscode/lm.selectChatModels #js {:vendor "copilot"})
      (.then (fn [models]
               (println "\n🤖 Available Language Models:")
               (doseq [model models]
                 (println (str "• " (.-id model) " - " (.-name model)
                                      " (max tokens: " (.-maxInputTokens model) ")")))
               models))))

(comment
  (p/let [ms (list-models!+)]
    (def ms ms))
  :rcf)

;; Function to get a specific model by ID
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

;; Interactive model picker using VS Code quick pick
(defn pick-model!+ []
  (-> (vscode/lm.selectChatModels #js {:vendor "copilot"})
      (.then (fn [models]
               (let [vscode (js/require "vscode")
                     items (map (fn [model]
                                  #js {:label (str (.-name model) " (" (.-id model) ")")
                                       :description (str "Max tokens: " (.-maxInputTokens model))
                                       :id (.-id model)})
                                models)]
                 (-> (.showQuickPick (.-window vscode)
                                     (clj->js items)
                                     #js {:placeHolder "Select a language model"
                                          :canPickMany false})
                     (.then (fn [selected-item]
                              (if selected-item
                                (let [model-id (.-id selected-item)]
                                  (println "Selected model:" model-id)
                                  model-id)
                                (throw (js/Error. "No model selected")))))))))))

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

(defn ask-copilot-with-model!+ [model-id message]
  (-> (get-model-by-id!+ model-id)
      (.then (fn [model]
               (.sendRequest model #js [#js {:role "user" :content message}])))
      (.then (fn [response]
               (let [iterator (get-iterator response)]
                 (collect-all-chunks iterator))))
      (.then (fn [chunks]
               (clojure.string/join "" chunks)))))

(comment
  (p/let [ask-response (ask-copilot-with-model!+ "claude-sonnet-4" "test")]
    (def ask-response ask-response)
    )
  :rcf)

;; 4. Create functions to play specific audio files
(defn play-hello-peter!+ []
  (-> (playback/load-and-play-audio!+ "slides/opening-sequence/hello-peter.mp3")
      (.then (fn [result]
               (println "Played hello-peter audio")
               result))))

(defn play-presenter-takeover!+ []
  (-> (playback/load-and-play-audio!+ "slides/opening-sequence/presenter-takeover.mp3")
      (.then (fn [result]
               (println "Played presenter-takeover audio")
               result))))

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
  (-> (show-start-button!+)
      (.then (fn [_]
               (println "🎭 Sequence started!")
               ;; Step 2: Ask Copilot to play the first audio file
               (ask-copilot-with-model!+ "claude-sonnet-4" "You are GitHub Copilot helping with a presentation. Please play the prerecorded audio file 'hello-peter.mp3' (which contains the greeting 'Hello! – Peter!')")))
      (.then (fn [copilot-response]
               (println "🤖 Copilot says:" copilot-response)
               ;; Actually play the first audio file
               (play-hello-peter!+)))
      (.then (fn [_]
               (println "✅ First audio played")
               ;; Step 3: Wait for a moment
               (p/delay 1000)))
      (.then (fn [_]
               (println "⏰ Delay complete, moving to second prompt")
               ;; Step 4: Ask Copilot to play the second audio file
               (ask-copilot-with-model!+ "claude-sonnet-4" "Now play the second prerecorded audio file 'presenter-takeover.mp3' (which contains the message 'I want to be the presenter! Give me the keys! I can drive! Please? Pretty, please?')")))
      (.then (fn [copilot-response]
               (println "🤖 Copilot says:" copilot-response)
               (play-presenter-takeover!+)))
      (.then (fn [_]
               (println "✅ Second audio started")
               :sequence-complete))))

(comment
  (run-opening-sequence!+)
  :rcf)

