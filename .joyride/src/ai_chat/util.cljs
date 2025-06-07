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

(defn get-available-models+
  "Get all available Copilot models as a map with model info."
  []
  (p/let [models (vscode/lm.selectChatModels #js {:vendor "copilot"})]
    (->> models
         (map (fn [model]
                (let [tooled-model (if (= "claude-opus-4" (.-id model))
                                     (-> model
                                         js->clj
                                         (merge {:capabilities
                                                 {:supportsImageToText
                                                  true,
                                                  :supportsToolCalling
                                                  true}})
                                         clj->js)
                                     model)]
                  {:id (.-id tooled-model)
                   :name (.-name tooled-model)
                   :vendor (.-vendor tooled-model)
                   :family (.-family tooled-model)
                   :version (.-version tooled-model)
                   :max-input-tokens (.-maxInputTokens tooled-model)
                   :model-obj tooled-model})))
         (map (juxt :id identity))
         (into {}))))

(defn get-model-by-id!+
  "Get a specific model by ID, with error handling."
  [model-id]
  (p/let [models-map (get-available-models+)]
    (when-let [model-info (get models-map model-id)]
      (:model-obj model-info))))

(defn extract-tool-result-content
  "Extract readable content from VS Code LM tool results"
  [raw-result]
  (letfn [(extract-text-from-node [node]
            (cond
              ;; If it's a text node with text property
              (and (.-text node) (string? (.-text node)))
              (.-text node)

              ;; If it has children, recursively extract from all children
              (and (.-children node) (array? (.-children node)))
              (->> (.-children node)
                   (map extract-text-from-node)
                   (apply str))

              ;; If it's already a string
              (string? node)
              node

              ;; Default fallback
              :else
              ""))

          (process-content-item [item]
            (cond
              ;; If it has a value property with a node
              (and (.-value item) (.-node (.-value item)))
              (extract-text-from-node (.-node (.-value item)))

              ;; If it has a node property directly
              (.-node item)
              (extract-text-from-node (.-node item))

              ;; If it has a value property and it's a string
              (and (.-value item) (string? (.-value item)))
              (.-value item)

              ;; If it's already a string
              (string? item)
              item

              ;; Default fallback
              :else
              (str item)))]

    (if (.-content raw-result)
      (->> (.-content raw-result)
           (map process-content-item)
           (apply str))
      (str raw-result))))

(defn execute-tool-calls!+
  "Execute tool calls using the official VS Code Language Model API - Fixed version"
  [tool-calls]
  (if (seq tool-calls)
    (do
      (println "🔧 Executing" (count tool-calls) "tool call(s)...")
      (p/all
       (to-array
        (map (fn [tool-call]
               (let [tool-name (.-name tool-call)
                     call-id (.-callId tool-call)
                     input (.-input tool-call)]
                 (println "🎯 Invoking tool:" tool-name)
                 (println "📝 Input:" (pr-str input))
                 (-> (vscode/lm.invokeTool tool-name #js {:input input})
                     (.then (fn [raw-result]
                              (let [result (extract-tool-result-content raw-result)]
                                (println "✅ Tool execution result for" tool-name ":")
                                (println result)
                                {:call-id call-id
                                 :tool-name tool-name
                                 :result result})))
                     (.catch (fn [error]
                               (println "❌ Tool execution error for" tool-name ":" error)
                               {:call-id call-id
                                :tool-name tool-name
                                :error (str error)})))))
             tool-calls))))
    (js/Promise.resolve [])))

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

(defn filter-available-tools
  "Filter out tools that are not reliably available for direct invocation.
   Uses selective filtering to keep read-only vscode_editing tools while removing write tools."
  [tools]
  (let [unsafe-tool-names #{"copilot_createFile" "copilot_insertEdit" "copilot_createDirectory"
                            "copilot_editNotebook" "copilot_runInTerminal" "copilot_installExtension"
                            "copilot_runVscodeCommand" "copilot_createNewWorkspace" "copilot_createAndRunTask"
                            "copilot_createNewJupyterNotebook"}]
    (->> tools
         (remove (fn [tool]
                   (contains? unsafe-tool-names (.-name tool)))))))

(defn get-available-tools
  "Get tools that are available for the agentic system to use.
   Filters out problematic write tools while keeping useful read-only tools."
  []
  (->> vscode/lm.tools
       filter-available-tools))

(defn enable-specific-tools
  "Enable only specific tools by name"
  [tool-names]
  (let [available-tools (get-available-tools)
        filtered-tools (filter #(contains? (set tool-names) (.-name %)) available-tools)]
    {:tools (into-array filtered-tools)
     :toolMode vscode/LanguageModelChatToolMode.Auto}))

(defn enable-joyride-tools
  "Get only the Joyride evaluation tool"
  []
  (enable-specific-tools ["joyride_evaluate_code"]))