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
     (re-find #"(?i)(task.*(complete|done|finished)|goal.*(achieved|reached|accomplished)|mission.*(complete|success)|successfully (completed|finished))" ai-text))))

(defn should-continue-agentic?
  "Determine if the agentic conversation should continue"
  [ai-text tool-calls turn-count max-turns]
  (cond
    ;; Stop if we've reached max turns
    (>= turn-count max-turns) false

    ;; Stop if AI indicates completion
    (agent-indicates-completion? ai-text) false

    ;; Continue if there are tool calls (agent is actively working)
    (seq tool-calls) true

    ;; Continue if AI explicitly says it wants to continue
    (and ai-text
         (re-find #"(?i)(next.*(step|action)|now.*(i.will|let.me)|continuing|proceeding)" ai-text)) true

    ;; Otherwise, stop
    :else false))

(defn agentic-conversation!+
  "Create an autonomous AI conversation that drives itself toward a goal"
  [{:keys [model-id goal max-turns progress-callback]
    :or {max-turns 10 progress-callback (fn [step] (println "Progress:" step))}}]

  (p/let [tools-args (util/enable-joyride-tools)
          conversation-history []]

    (letfn [(continue-agentic-conversation [history turn-count last-response]
              (progress-callback (str "Turn " turn-count "/" max-turns))

              (if (>= turn-count max-turns)
                {:history history :reason :max-turns-reached :final-response last-response}

                (p/let [;; Build messages - include processed tool results in conversation
                        messages (build-agentic-messages history goal turn-count)

                        ;; Send request
                        response (util/send-prompt-request!+
                                  {:model-id model-id
                                   :system-prompt agentic-system-prompt
                                   :messages messages
                                   :options tools-args})

                        ;; Collect response
                        result (util/collect-response-with-tools!+ response)
                        ai-text (:text result)
                        tool-calls (:tool-calls result)

                        ;; Log AI's thinking
                        _ (when ai-text
                            (println "\n🤖 AI Agent says:")
                            (println ai-text))

                        ;; Add AI response to history
                        updated-history (conj history
                                              {:role :assistant
                                               :content ai-text
                                               :tool-calls tool-calls
                                               :turn turn-count})

                        ;; Execute tools if present and process results properly
                        final-history (if (seq tool-calls)
                                        (do
                                          (println "\n🔧 AI Agent executing" (count tool-calls) "tool(s)")
                                          (p/let [tool-results (util/execute-tool-calls!+ tool-calls)]
                                            (println "✅ Tools executed, processed results:" tool-results)
                                            (conj updated-history
                                                  {:role :tool-results
                                                   :results tool-results
                                                   :processed-results tool-results
                                                   :turn turn-count})))
                                        updated-history)

                        ;; Decide whether to continue
                        should-continue? (should-continue-agentic? ai-text tool-calls turn-count max-turns)]

                  (if should-continue?
                    (do
                      (println "\n↻ AI Agent continuing to next step...")
                      (continue-agentic-conversation final-history (inc turn-count) result))
                    (let [reason (cond
                                   (>= turn-count max-turns) :max-turns-reached
                                   (agent-indicates-completion? ai-text) :task-complete
                                   :else :agent-finished)]
                      (println "\n🎯 Agentic conversation ended:" (name reason))
                      {:history final-history :reason reason :final-response result})))))]

      ;; Start the agentic conversation
      (println "🚀 Starting agentic conversation with goal:" goal)
      (continue-agentic-conversation conversation-history 1 nil))))

(defn advanced-agentic-conversation!+
  "Advanced agentic AI that can drive complex multi-step tasks"
  [{:keys [model-id goal max-turns show-in-ui?]
    :or {max-turns 8 show-in-ui? false}}]

  (letfn [(show-progress [message]
            (println message)
            (when show-in-ui?
              (vscode/window.showInformationMessage message)))]

    (p/let [result (agentic-conversation!+
                    {:model-id model-id
                     :goal goal
                     :max-turns max-turns
                     :progress-callback show-progress})]

      ;; Show final summary with proper turn counting
      (let [;; Count actual turns by counting assistant messages
            actual-turns (count (filter #(= (:role %) :assistant) (:history result)))
            summary (str "🎯 Agentic task "
                         (case (:reason result)
                           :task-complete "COMPLETED successfully!"
                           :max-turns-reached "reached max turns"
                           :agent-finished "finished"
                           "ended unexpectedly")
                         " (" actual-turns " turns, " (count (:history result)) " conversation steps)")]
        (show-progress summary))

      result)))

(defn start-agentic-agent!+
  "Start an autonomous AI agent with a goal - simple API"
  [goal]
  (advanced-agentic-conversation!+
   {:model-id "gpt-4o-mini"  ; Fast model for experimentation
    :goal goal
    :max-turns 6
    :show-in-ui? true}))

(comment
  ;; Simple usage
  (start-agentic-agent!+ "Count all .cljs files and show the result")
  (start-agentic-agent!+ "Show an information message that says 'Hello from the adaptive AI agent!' using VS Code APIs")
  ;; Advanced usage
  (advanced-agentic-conversation!+
   {:model-id "claude-sonnet-4"
    :goal "Analyze this project structure and create documentation"
    :max-turns 10
    :show-in-ui? true})

  (advanced-agentic-conversation!+
   {:model-id "claude-sonnet-4"
    :goal "Analyze the `ai-chat.agentic-prompter` namespace and its dependencies and create documentation in `docs/agent-dispatch/`."
    :max-turns 12
    :show-in-ui? true})

  ;; Full control
  (agentic-conversation!+
   {:model-id "claude-sonnet-4"
    :goal "Generate the fibonacci sequence without writing a function, but instead by starting with evaluating `[0 1]` and then each step read the result and evaluate `[second-number sum-of-first-and-second-number]`. In the last step evaluate just `second-number`."
    :max-turns 12
    :progress-callback (fn [step]
                         (println "🔄" step)
                         (vscode/window.showInformationMessage step))})
  :rcf)