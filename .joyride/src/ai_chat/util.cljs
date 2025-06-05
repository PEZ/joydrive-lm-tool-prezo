(ns ai-chat.util
  (:require
   ["vscode" :as vscode]
   [promesa.core :as p]))

(defn create-chat-message
  "Create a VS Code Language Model chat message as a plain JS object."
  [{:keys [role content]}]
  (let [role-str (case role
                   :system "system"
                   :user "user"
                   :assistant "assistant")]
    #js {:role role-str :content content}))

(defn enable-specific-tools
  "Enable only specific tools by name"
  [tool-names]
  (let [available-tools vscode/lm.tools
        filtered-tools (filter #(contains? (set tool-names) (.-name %)) available-tools)]
    {:tools (into-array filtered-tools)
     :toolMode vscode/LanguageModelChatToolMode.Auto}))

(defn enable-joyride-tools
  "Get only the Joyride evaluation tool"
  []
  (enable-specific-tools ["joyride_evaluate_code"]))

(defn get-available-models+
  "Get all available Copilot models as a map with model info."
  []
  (p/let [models (vscode/lm.selectChatModels #js {:vendor "copilot"})]
    (->> models
         (map (fn [model]
                {:id (.-id model)
                 :name (.-name model)
                 :vendor (.-vendor model)
                 :family (.-family model)
                 :version (.-version model)
                 :max-input-tokens (.-maxInputTokens model)
                 :model-obj model}))
         (map (juxt :id identity))
         (into {}))))

(defn get-model-by-id!+
  "Get a specific model by ID, with error handling."
  [model-id]
  (p/let [models-map (get-available-models+)]
    (if-let [model-info (get models-map model-id)]
      (:model-obj model-info)
      (throw (js/Error. (str "❌ Model not found: " model-id))))))

(defn execute-tool-calls!+
  "Execute tool calls using the official VS Code Language Model API"
  [tool-calls]
  (when (seq tool-calls)
    (println "🔧 Executing" (count tool-calls) "tool call(s)...")
    (p/let [results
            (p/all
             (map (fn [tool-call]
                    (let [tool-name (.-name tool-call)
                          call-id (.-callId tool-call)
                          input (.-input tool-call)]
                      (println "🎯 Invoking tool:" tool-name)
                      (println "📝 Input:" (pr-str input))
                      (p/let [raw-result (vscode/lm.invokeTool tool-name #js {:input input})
                              _ (def raw-result raw-result)
                              result (mapv (fn [o]
                                             (-> o
                                                 .-value
                                                 js/JSON.parse
                                                 js->clj))
                                           (.-content raw-result))
                              _ (def result result)
                              ]
                        (println "✅ Tool execution result:" result)
                        {:call-id call-id
                         :tool-name tool-name
                         :result result})))
                  tool-calls))]
      (println "🎉 All tools executed!")
      results)))

(defn collect-response-with-tools!+
  "Collect all text and tool calls from a streaming response."
  [response]
  (p/let [stream (.-stream response)
          async-iter-symbol js/Symbol.asyncIterator
          iterator-fn (aget stream async-iter-symbol)
          iterator (.call iterator-fn stream)]
    (letfn [(collect-parts [text-acc tool-calls]
              (p/let [result (.next iterator)]
                (if (.-done result)
                  {:text text-acc :tool-calls tool-calls :response response}
                  (let [part (.-value result)]
                    ;; Check if this is a tool call part
                    (cond
                      ;; Regular text part
                      (and (.-value part) (string? (.-value part)))
                      (collect-parts (str text-acc (.-value part)) tool-calls)

                      ;; Tool call part
                      (and (.-callId part) (.-name part))
                      (collect-parts text-acc (conj tool-calls part))

                      ;; Other parts - continue
                      :else
                      (collect-parts text-acc tool-calls))))))]
      (collect-parts "" []))))

(defn build-message-chain
  "Build a message chain with system instructions."
  [{:keys [system-prompt messages]}]
  (let [system-msg (create-chat-message {:role :system :content system-prompt})
        user-msgs (map create-chat-message messages)]
    (cond->> user-msgs
      system-msg (cons system-msg))))

(defn send-prompt-request!+
  "Send a prompt request with optional system instructions and tool use.
   Args: {:model-id string, :system-prompt string (optional), :messages vector, :options map (optional)}"
  [{:keys [model-id system-prompt messages options]}]
  (p/let [model (get-model-by-id!+ model-id)
          message-chain (build-message-chain {:system-prompt system-prompt
                                              :messages messages})
          js-messages (into-array message-chain)
          response (.sendRequest model js-messages (clj->js options))]
    response))