(ns ai-presenter.ai-prompter
  "General-purpose AI prompting utilities for VS Code Language Model API.
   Handles model selection, message creation, and system instruction integration."
  (:require
   ["vscode" :as vscode]
   [ai-mood-selector :as mood]
   [promesa.core :as p]))

(defn create-chat-message
  "Create a VS Code Language Model chat message as a plain JS object."
  [{:keys [role content]}]
  (let [role-str (case role
                   :system "system"
                   :user "user"
                   :assistant "assistant")]
    #js {:role role-str :content content}))

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
                      (p/let [result (vscode/lm.invokeTool tool-name #js {:input input})]
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

(defn prompt-with-tool-execution!+
  "Send a prompt, execute any tool calls, and return the complete conversation"
  [prompt-args]
  (p/let [tools-args (enable-joyride-tools)
          response (send-prompt-request!+ (assoc prompt-args
                                                 :options tools-args))
          result (collect-response-with-tools!+ response)
          tool-calls (:tool-calls result)]

    ;; If there are tool calls, execute them
    (if (seq tool-calls)
      (do
        (println "🔧 Found" (count tool-calls) "tool call(s) to execute")
        (p/let [tool-results (execute-tool-calls!+ tool-calls)]
          (println "🎉 Tools executed successfully!")
          ;; For now, return the result with tool execution info
          (assoc result
                 :tools-used (map #(.-name %) tool-calls)
                 :tool-results tool-results)))

      ;; No tool calls, return as-is
      (assoc result :tools-used []))))

(defn ask-with-system!+
  "Ask a question with explicit system instructions."
  [model-id system-prompt user-question]
  (p/let [answer (prompt-with-tool-execution!+
                  {:model-id model-id
                   :system-prompt system-prompt
                   :messages [{:role :user
                               :content user-question}]})]
    answer))

(defn continue-conversation!+
  "Continue a conversation with message history."
  [model-id conversation-history new-message]
  (let [messages (conj conversation-history {:role :user :content new-message})]
    (prompt-with-tool-execution!+ {:model-id model-id
                                   :messages messages})))

(defn pick-model!+ []
  (p/let [models-map (get-available-models+)
          items (map (fn [[id model-info]]
                       #js {:label (str (:name model-info) " (" id ")")
                            :description (str "Max tokens: " (:max-input-tokens model-info))
                            :id id})
                     models-map)
          selected-item (vscode/window.showQuickPick
                         (clj->js items)
                         #js {:placeHolder "Select a language model"
                              :canPickMany false})]
    (if selected-item
      (let [model-id (.-id selected-item)]
        (println "Selected model:" model-id)
        model-id)
      (throw (js/Error. "No model selected")))))

;; Convenience functions for tool use control
(defn disable-tools-options
  "Options to disable tool use - for when we want pure Joyride solutions"
  []
  {:tools []})

(comment
  (-> (pick-model!+)
      (.then (fn [model-id]
               (ask-with-system!+ "some instructions" "hello, greet the audience, please =) " model-id)))
      (.then (fn [response]
               (def response response)
               (println "Model response:" response)
               (vscode/showInformationMessage
                (str "🤖 " response))
               response)))
  :rcf)
