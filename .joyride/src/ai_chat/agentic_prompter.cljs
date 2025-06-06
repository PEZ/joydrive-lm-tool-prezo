(ns ai-chat.agentic-prompter
  "Autonomous AI conversation system with improved error handling and adaptability"
  (:require
   ["vscode" :as vscode]
   [ai-chat.util :as util]
   [promesa.core :as p]))

(def agentic-system-prompt
  "You are an autonomous AI agent with the ability to take initiative and drive conversations toward goals.

AGENTIC BEHAVIOR RULES:
1. When given a goal, break it down into steps and execute them
2. Use available tools proactively to gather information or take actions
3. After each tool use, analyze the results and decide your next action
4. If a tool returns unexpected results or fails, ADAPT your approach - don't repeat the same action
5. Continue working toward the goal without waiting for human input
6. Provide progress updates as you work
7. Ask for clarification only when absolutely necessary
8. Take creative initiative to solve problems

LEARNING FROM FAILURES:
- If tool results are not what you expected, try a different approach
- Don't repeat the exact same tool call if it didn't work the first time
- Explain what you learned and how you're adapting your strategy
- Consider the tool results as feedback to guide your next steps

CONVERSATION FLOW:
- Receive goal from human
- Plan your approach
- Execute tools and actions
- Analyze results and continue OR adapt if results weren't as expected
- Report progress and findings
- Suggest next steps or completion

AVAILABLE TOOLS:
- joyride_evaluate_code: Execute Joyride/ClojureScript code in VS Code

Be proactive, creative, and goal-oriented. Drive the conversation forward!")

(defn build-agentic-messages
  "Build message history for agentic conversation with actionable tool feedback"
  [history goal turn-count]
  (let [initial-message {:role :user
                         :content (str "GOAL: " goal
                                       "\n\nPlease work autonomously toward this goal. "
                                       "Take initiative, use tools as needed, and continue "
                                       "until the goal is achieved. This is turn " turn-count ".")}]
    (if (empty? history)
      [initial-message]
      ;; Convert history to message format with processed tool results
      (concat [initial-message]
              (mapcat (fn [entry]
                        (case (:role entry)
                          :assistant [{:role :assistant :content (:content entry)}]
                          :tool-results
                          (map (fn [result]
                                 {:role :user
                                  :content (str "TOOL RESULT: " result
                                                "\n\nAnalyze this result. If it shows the goal is achieved, conclude. "
                                                "If not, adapt your approach and try something different.")})
                               (:processed-results entry))
                          [])) ; skip other roles
                      history)))))

(defn agent-indicates-completion?
  "Check if AI agent indicates the task is complete"
  [ai-text]
  (when ai-text
    (boolean
     (re-find #"(?i)(task.*(?!\bnot\b).*(complete|done|finished)|goal.*(?!\bnot\b).*(achieved|reached|accomplished)|mission.*(?!\bnot\b).*(complete|success)|successfully (completed|finished))" ai-text))))

(defn add-assistant-response
  "Add AI assistant response to conversation history"
  [history ai-text tool-calls turn-count]
  (conj history
        {:role :assistant
         :content ai-text
         :tool-calls tool-calls
         :turn turn-count}))

(defn add-tool-results
  "Add tool execution results to conversation history"
  [history tool-results turn-count]
  (conj history
        {:role :tool-results
         :results tool-results
         :processed-results tool-results
         :turn turn-count}))

(defn determine-conversation-outcome
  "Determine if conversation should continue and why"
  [ai-text tool-calls turn-count max-turns]
  (cond
    (>= turn-count max-turns)
    {:continue? false :reason :max-turns-reached}

    (agent-indicates-completion? ai-text)
    {:continue? false :reason :task-complete}

    (seq tool-calls)
    {:continue? true :reason :tools-executing}

    (and ai-text (re-find #"(?i)(next.*(step|action)|i'll|i.will|let.me|continu|proceed)" ai-text))
    {:continue? true :reason :agent-continuing}

    :else
    {:continue? false :reason :agent-finished}))

(defn format-completion-result
  "Format the final conversation result"
  [history reason final-response]
  {:history history
   :reason reason
   :final-response final-response})

(defn execute-conversation-turn
  "Execute a single conversation turn - handles request/response cycle"
  [{:keys [model-id goal history turn-count tools-args]}]
  (p/let [messages (build-agentic-messages history goal turn-count)
          response (util/send-prompt-request!+
                    {:model-id model-id
                     :system-prompt agentic-system-prompt
                     :messages messages
                     :options tools-args})
          result (util/collect-response-with-tools!+ response)]
    (assoc result :turn turn-count)))

(defn execute-tools-if-present!+
  "Execute tool calls if present, return updated history"
  [history tool-calls turn-count]
  (if (seq tool-calls)
    (do
      (println "\n🔧 AI Agent executing" (count tool-calls) "tool(s)")
      (p/let [tool-results (util/execute-tool-calls!+ tool-calls)]
        (println "✅ Tools executed, processed results:" tool-results)
        (add-tool-results history tool-results turn-count)))
    history))

(defn continue-conversation-loop
  "Main conversation loop using extracted pure functions"
  [{:keys [model-id goal max-turns progress-callback tools-args]} history turn-count last-response]
  (progress-callback (str "Turn " turn-count "/" max-turns))

  (if (> turn-count max-turns)
    (format-completion-result history :max-turns-reached last-response)

    (p/let [;; Execute the conversation turn
            turn-result (execute-conversation-turn
                         {:model-id model-id
                          :goal goal
                          :history history
                          :turn-count turn-count
                          :tools-args tools-args})

            ai-text (:text turn-result)
            tool-calls (:tool-calls turn-result)

            ;; Log AI's response
            _ (when ai-text
                (println "\n🤖 AI Agent says:")
                (println ai-text))

            ;; Add AI response to history
            history-with-assistant (add-assistant-response history ai-text tool-calls turn-count)

            ;; Execute tools and update history
            final-history (execute-tools-if-present!+ history-with-assistant tool-calls turn-count)

            ;; Determine what to do next
            outcome (determine-conversation-outcome ai-text tool-calls turn-count max-turns)]

      (if (:continue? outcome)
        (do
          (println "\n↻ AI Agent continuing to next step...")
          (continue-conversation-loop
           {:model-id model-id :goal goal :max-turns max-turns
            :progress-callback progress-callback :tools-args tools-args}
           final-history
           (inc turn-count)
           turn-result))
        (do
          (println "\n🎯 Agentic conversation ended:" (name (:reason outcome)))
          (format-completion-result final-history (:reason outcome) turn-result))))))

(defn agentic-conversation!+
  "Create an autonomous AI conversation that drives itself toward a goal"
  [{:keys [model-id goal max-turns progress-callback]
    :or {max-turns 10
         progress-callback (fn [step]
                             (println "Progress:" step))}}]
  (p/let [tools-args (util/enable-joyride-tools)]
    (println "🚀 Starting agentic conversation with goal:" goal)
    (continue-conversation-loop
     {:model-id model-id
      :goal goal
      :max-turns max-turns
      :progress-callback progress-callback
      :tools-args tools-args}
     [] ; empty initial history
     1  ; start at turn 1
     nil)))

(defn autonomous-conversation!+
  "Start an autonomous AI conversation toward a goal with flexible configuration"
  ([goal]
   (autonomous-conversation!+ goal
                              {:model-id "gpt-4o-mini"
                               :max-turns 6
                               :show-progress? true}))

  ([goal {:keys [model-id max-turns show-progress?]
          :or {model-id "gpt-4o-mini"
               max-turns 8
               show-progress? false}}]

   (letfn [(show-progress [message]
             (println message)
             (when show-progress?
               (vscode/window.showInformationMessage message)))]

     (p/let [result (agentic-conversation!+
                     {:model-id model-id
                      :goal goal
                      :max-turns max-turns
                      :progress-callback show-progress})]

       ;; Show final summary with proper turn counting
       (let [actual-turns (count (filter #(= (:role %) :assistant) (:history result)))
             summary (str "🎯 Agentic task "
                          (case (:reason result)
                            :task-complete "COMPLETED successfully!"
                            :max-turns-reached "reached max turns"
                            :agent-finished "finished"
                            "ended unexpectedly")
                          " (" actual-turns " turns, " (count (:history result)) " conversation steps)")]
         (show-progress summary))

       result))))

(comment
  ;; Simple usage
  (autonomous-conversation!+ "Count all .cljs files and show the result")
  (autonomous-conversation!+ "Show an information message that says 'Hello from the adaptive AI agent!' using VS Code APIs")

  ;; Advanced usage
  (autonomous-conversation!+ "Analyze this project structure and create documentation"
                             {:model-id "claude-sonnet-4"
                              :max-turns 10
                              :show-progress? true})

  (autonomous-conversation!+ "Analyze the `ai-chat.agentic-prompter` namespace and its dependencies and create documentation in `docs/agent-dispatch/`."
                             {:model-id "claude-sonnet-4"
                              :max-turns 15
                              :show-progress? true})

  (autonomous-conversation!+ "Generate the eight first numbers in the fibonacci sequence without writing a function, but instead by starting with evaluating `[0 1]` and then each step read the result and evaluate `[second-number sum-of-first-and-second-number]`. In the last step evaluate just `second-number`."
                             {:model-id "gpt-4o-mini"
                              :max-turns 12
                              :progress-callback (fn [step]
                                                   (println "🔄" step)
                                                   (vscode/window.showInformationMessage step))})
  :rcf)