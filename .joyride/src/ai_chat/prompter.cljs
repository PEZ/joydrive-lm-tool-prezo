(ns ai-chat.prompter
  "General-purpose AI prompting utilities for VS Code Language Model API.
   Handles model selection, message creation, and system instruction integration."
  (:require
   ["vscode" :as vscode]
   [ai-chat.util :as util]
   [promesa.core :as p]))

(defn prompt-with-tool-execution!+
  "Send a prompt, execute any tool calls, and return the complete conversation"
  [prompt-args]
  (p/let [tools-args (util/enable-joyride-tools)
          response (util/send-prompt-request!+ (assoc prompt-args
                                                 :options tools-args))
          result (util/collect-response-with-tools!+ response)
          tool-calls (:tool-calls result)]

    ;; If there are tool calls, execute them
    (if (seq tool-calls)
      (do
        (println "🔧 Found" (count tool-calls) "tool call(s) to execute")
        (p/let [tool-results (util/execute-tool-calls!+ tool-calls)]
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

;; Convenience functions for tool use control
(defn disable-tools-options
  "Options to disable tool use - for when we want pure Joyride solutions"
  []
  {:tools []})

(comment

  :rcf)
