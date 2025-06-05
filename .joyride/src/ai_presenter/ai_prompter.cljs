(ns ai-presenter.ai-prompter
  "General-purpose AI prompting utilities for VS Code Language Model API.
   Handles model selection, message creation, and system instruction integration."
  (:require
   ["vscode" :as vscode]
   [ai-mood-selector :as mood]
   [promesa.core :as p]))

(defn create-chat-message
  "Create a VS Code Language Model chat message as a plain JS object.
   Args: {:role :system|:user|:assistant, :content string, :name string (optional)}"
  [{:keys [role content name]}]
  (let [role-str (case role
                   :system "system"
                   :user "user"
                   :assistant "assistant"
                   (str role))] ; Allow passing string role directly
    (if name
      #js {:role role-str :content content :name name}
      #js {:role role-str :content content})))

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

(defn get-current-system-instructions+
  "Read the current system instructions from the active mood."
  []
  (let [current-mood (mood/get-current-mood)]
    (if current-mood
      (p/let [instructions-uri (vscode/Uri.joinPath
                                (mood/ws-root)
                                ".github"
                                "copilot-instructions.md")
              file-data (vscode/workspace.fs.readFile instructions-uri)
              content (-> (js/Buffer.from file-data) (.toString "utf-8"))]
        content)
      (p/resolved nil))))

(defn build-message-chain
  "Build a message chain with optional system instructions.
   Args: {:system-prompt string (optional), :messages [{:role :user|:assistant, :content string}]}"
  [{:keys [system-prompt messages]}]
  (let [system-msg (when system-prompt
                     (create-chat-message {:role :system :content system-prompt}))
        user-msgs (map create-chat-message messages)]
    (cond->> user-msgs
      system-msg (cons system-msg))))

(defn send-prompt-request!+
  "Send a prompt request with optional system instructions and tool use.
   Args: {:model-id string, :system-prompt string (optional), :messages vector, :options map (optional)}"
  [{:keys [model-id system-prompt messages options]}]
  (def model-id model-id)
  (def system-prompt system-prompt)
  (def messages messages)
  (def options options)
  (p/let [model (get-model-by-id!+ model-id)
          ;; Use provided system prompt or fall back to current mood instructions
          _ (def model model)
          final-system-prompt (or system-prompt (get-current-system-instructions+))
          _ (def final-system-prompt final-system-prompt)
          message-chain (build-message-chain {:system-prompt final-system-prompt
                                              :messages messages})
          _ (def message-chain message-chain)
          js-messages (clj->js message-chain)
          _ (def js-messages js-messages)
          ;; Enable tool use by default if not explicitly disabled
          final-options (merge {:tools vscode/lm.tools
                                :toolMode vscode/LanguageModelChatToolMode.Auto}
                               options)
          response (.sendRequest model js-messages (clj->js final-options))]
    (def response response)
    response))


(defn collect-response-text!+
  "Collect all text from a streaming response."
  [response]
  (p/let [text-iter (.-text response)
          iter-fn (aget text-iter js/Symbol.asyncIterator)
          iterator (.call iter-fn text-iter)]
    (letfn [(collect-chunks [acc]
              (p/let [result (.next iterator)]
                (if (.-done result)
                  acc
                  (collect-chunks (str acc (.-value result))))))]
      (collect-chunks ""))))

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

(defn prompt-and-collect!+
  "Send a prompt and collect the full response text.
   Enhanced to show when tools are used behind the scenes."
  [prompt-args]
  (p/let [response (send-prompt-request!+ prompt-args)
          result (collect-response-with-tools!+ response)]
    ;; Show tool usage in console for debugging
    (when (seq (:tool-calls result))
      (js/console.log "🔧 Tools used:" (map #(.-name %) (:tool-calls result))))
    {:response (:response result)
     :text (:text result)
     :tools-used (map #(.-name %) (:tool-calls result))}))

;; Convenience functions for common patterns
(defn ask-with-system!+
  "Ask a question with explicit system instructions."
  [model-id system-prompt user-question]
  (p/let [answer (prompt-and-collect!+ {:model-id model-id
                                        :system-prompt system-prompt
                                        :messages [{:role :user :content user-question}]})]
    answer))

(defn continue-conversation!+
  "Continue a conversation with message history."
  [model-id conversation-history new-message]
  (let [messages (conj conversation-history {:role :user :content new-message})]
    (prompt-and-collect!+ {:model-id model-id
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

(defn enable-specific-tools
  "Enable only specific tools by name"
  [tool-names]
  (let [available-tools (.-tools vscode/lm)
        filtered-tools (filter #(contains? (set tool-names) (.-name %)) available-tools)]
    {:tools filtered-tools
     :toolMode (.-Auto vscode/LanguageModelChatToolMode)}))

(defn force-tool-use
  "Force the model to use exactly one tool (Required mode)"
  [tool-name]
  (let [available-tools (.-tools vscode/lm)
        target-tool (first (filter #(= (.-name %) tool-name) available-tools))]
    (if target-tool
      {:tools [target-tool]
       :toolMode (.-Required vscode/LanguageModelChatToolMode)}
      (throw (js/Error. (str "Tool not found: " tool-name))))))

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
