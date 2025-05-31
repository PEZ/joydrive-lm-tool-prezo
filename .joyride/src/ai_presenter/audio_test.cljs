(ns ai-presenter.audio-test
  (:require [ai-presenter.audio :as audio]
            [cljs.test :refer [deftest testing is]]
            [test.macros :refer [deftest-async]]
            [promesa.core :as p]))

;; =============================================================================
;; Tests for AI Text-to-Speech Audio Generation
;; =============================================================================

(deftest available-voices-test
  (testing "should return list of supported OpenAI voices"
    (let [voices (audio/available-voices)]
      (is (vector? voices))
      (is (some #{"alloy" "echo" "fable" "onyx" "nova" "shimmer"} voices))
      (is (every? string? voices)))))

(deftest voice-config-validation-test
  (testing "should validate voice configuration"
    (is (audio/valid-voice-config? {:voice "nova" :model "tts-1" :format "mp3"}))
    (is (not (audio/valid-voice-config? {:voice "invalid" :model "tts-1" :format "mp3"})))
    (is (not (audio/valid-voice-config? {:voice "nova" :model "invalid" :format "mp3"})))
    (is (not (audio/valid-voice-config? {:voice "nova" :model "tts-1" :format "invalid"})))))

(deftest text-validation-test
  (testing "should validate text input"
    (is (audio/valid-text? "Hello world"))
    (is (audio/valid-text? (apply str (repeat 4000 "a"))))
    (is (not (audio/valid-text? "")))
    (is (not (audio/valid-text? nil)))
    (is (not (audio/valid-text? (apply str (repeat 5000 "a")))))))

(deftest-async generate-audio-validation-test
  (testing "should reject invalid inputs"
    (p/do
      (p/let [result (p/catch (audio/generate-audio+ "" {:voice "nova"})
                              (fn [error] {:error (.-message error)}))]
        (is (contains? result :error))
        (is (re-find #"empty.*text" (get result :error))))

      (p/let [result (p/catch (audio/generate-audio+ "Hello" {:voice "invalid"})
                              (fn [error] {:error (.-message error)}))]
        (is (contains? result :error))
        (is (re-find #"Invalid voice" (get result :error)))))))

(deftest-async generate-audio-success-test
  (testing "should generate audio file with valid inputs"
    ;; This test will be skipped if no API key is available
    (if-not (audio/api-key-available?)
      (is true "Skipping test - no OpenAI API key available")
      (p/let [config {:voice "nova" :model "tts-1" :format "mp3"}
              result (audio/generate-audio+ "Hello world for testing" config)]
        (is (string? result))
        (is (re-find #"\.mp3$" result))))))

(deftest default-voice-config-test
  (testing "should provide sensible defaults"
    (let [config (audio/default-voice-config)]
      (is (= "nova" (:voice config)))
      (is (= "tts-1" (:model config)))
      (is (= "mp3" (:format config))))))

(comment
  ;; Run tests in REPL via: (cljs.test/run-tests 'ai-presenter.audio-test)
  :rcf)
