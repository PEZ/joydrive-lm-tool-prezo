(ns test.ai-chat.agentic-prompter-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [ai-chat.agentic-prompter :as agentic]))

(def test-history-empty [])
(def test-history-basic
  [{:role :assistant :content "Hello" :tool-calls [] :turn 1}])
(def test-tool-results
  [{:call-id "abc123" :tool-name "joyride_evaluate_code" :result ["File count: 5"]}])

(deftest add-assistant-response-test
  (testing "Adding assistant response to conversation history"
    (let [result (agentic/add-assistant-response test-history-empty "Hello world" [] 1)]
      (is (= 1 (count result)) "Should add one entry to empty history")
      (is (= :assistant (:role (first result))) "Should have assistant role")
      (is (= "Hello world" (:content (first result))) "Should preserve content")
      (is (= 1 (:turn (first result))) "Should set turn number"))

    (let [result (agentic/add-assistant-response test-history-basic "Second" [{:name "tool"}] 2)]
      (is (= 2 (count result)) "Should append to existing history")
      (is (= 2 (:turn (last result))) "Should set correct turn number")
      (is (seq (:tool-calls (last result))) "Should preserve tool calls"))

    (testing "Edge cases"
      (let [result (agentic/add-assistant-response [] nil [] 0)]
        (is (nil? (:content (first result))) "Should handle nil content")
        (is (= 0 (:turn (first result))) "Should handle zero turn")))))

(deftest add-tool-results-test
  (testing "Adding tool results to conversation history"
    (let [result (agentic/add-tool-results test-history-basic test-tool-results 1)]
      (is (= 2 (count result)) "Should add tool results entry")
      (is (= :tool-results (:role (last result))) "Should have tool-results role")
      (is (= test-tool-results (:results (last result))) "Should preserve results")
      (is (= test-tool-results (:processed-results (last result))) "Should set processed-results"))

    (testing "Empty tool results"
      (let [result (agentic/add-tool-results [] [] 1)]
        (is (= 1 (count result)) "Should add entry even with empty results")
        (is (= [] (:results (first result))) "Should handle empty results")))))

(deftest determine-conversation-outcome-test
  (testing "Conversation continuation decision logic"
    (testing "Task completion detection"
      (let [result (agentic/determine-conversation-outcome "Task is complete!" [] 3 10)]
        (is (false? (:continue? result)) "Should stop when task complete")
        (is (= :task-complete (:reason result)) "Should identify completion reason")))

    (testing "Tool execution continuation"
      (let [result (agentic/determine-conversation-outcome "Working..." [{:name "tool"}] 3 10)]
        (is (true? (:continue? result)) "Should continue when tools executing")
        (is (= :tools-executing (:reason result)) "Should identify tool execution")))

    (testing "Max turns reached"
      (let [result (agentic/determine-conversation-outcome "Thinking..." [] 10 10)]
        (is (false? (:continue? result)) "Should stop at max turns")
        (is (= :max-turns-reached (:reason result)) "Should identify max turns reason")))

    (testing "Agent continuing"
      (let [result (agentic/determine-conversation-outcome "Now I will proceed" [] 3 10)]
        (is (true? (:continue? result)) "Should continue when agent indicates continuation")
        (is (= :agent-continuing (:reason result)) "Should identify agent continuation")))

    (testing "Agent finished"
      (let [result (agentic/determine-conversation-outcome "Done here." [] 3 10)]
        (is (false? (:continue? result)) "Should stop when agent indicates finish")
        (is (= :agent-finished (:reason result)) "Should identify agent finished")))

    (testing "Edge cases"
      (let [result (agentic/determine-conversation-outcome nil [] 1 10)]
        (is (false? (:continue? result)) "Should handle nil text")
        (is (= :agent-finished (:reason result)) "Should default to agent finished"))

      (let [result (agentic/determine-conversation-outcome "Working" [] 0 0)]
        (is (false? (:continue? result)) "Should handle zero max turns")
        (is (= :max-turns-reached (:reason result)) "Should prioritize max turns")))))

(deftest format-completion-result-test
  (testing "Formatting final conversation results"
    (let [test-history [{:role :assistant :content "Hello"}]
          test-response {:text "Final" :tool-calls []}
          result (agentic/format-completion-result test-history :task-complete test-response)]
      (is (= test-history (:history result)) "Should preserve history")
      (is (= :task-complete (:reason result)) "Should preserve reason")
      (is (= test-response (:final-response result)) "Should preserve final response")
      (is (= 3 (count (keys result))) "Should have exactly 3 keys"))

    (testing "Edge cases"
      (let [result (agentic/format-completion-result [] :unknown-reason nil)]
        (is (= [] (:history result)) "Should handle empty history")
        (is (= :unknown-reason (:reason result)) "Should handle unknown reason")
        (is (nil? (:final-response result)) "Should handle nil response")))))