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
  "Send a prompt request with optional system instructions.
   Args: {:model-id string, :system-prompt string (optional), :messages vector, :options map (optional)}"
  [{:keys [model-id system-prompt messages options]}]
  (p/let [model (get-model-by-id!+ model-id)
          ;; Use provided system prompt or fall back to current mood instructions
          final-system-prompt (or system-prompt (get-current-system-instructions+))
          message-chain (build-message-chain {:system-prompt final-system-prompt
                                              :messages messages})
          js-messages (clj->js message-chain)
          response (. model sendRequest js-messages (clj->js (or options {})))]
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

(defn prompt-and-collect!+
  "Send a prompt and collect the full response text.
   Convenience function that combines send-prompt-request!+ and collect-response-text!+"
  [prompt-args]
  (p/let [response (send-prompt-request!+ prompt-args)
          text (collect-response-text!+ response)]
    {:response response
     :text text}))

;; Convenience functions for common patterns
(defn ask-with-system!+
  "Ask a question with explicit system instructions."
  [model-id system-prompt user-question]
  (prompt-and-collect!+ {:model-id model-id
                         :system-prompt system-prompt
                         :messages [{:role :user :content user-question}]}))

(defn ask-with-current-mood!+
  "Ask a question using the current AI mood as system instructions."
  [model-id user-question]
  (prompt-and-collect!+ {:model-id model-id
                         :messages [{:role :user :content user-question}]}))

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