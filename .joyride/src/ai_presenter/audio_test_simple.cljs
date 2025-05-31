(ns ai-presenter.audio-test-simple
  (:require [ai-presenter.audio :as audio]
            [cljs.test :refer [deftest testing is]]
            [promesa.core :as p]))

;; =============================================================================
;; Simple Audio Module Tests (using Seatbelt)
;; =============================================================================

(deftest available-voices-test
  (testing "should return list of supported OpenAI voices"
    (let [voices (audio/available-voices)]
      (is (vector? voices))
      (is (some #{"alloy"} voices))
      (is (some #{"nova"} voices))
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

(deftest default-voice-config-test
  (testing "should provide sensible defaults"
    (let [config (audio/default-voice-config)]
      (is (= "nova" (:voice config)))
      (is (= "tts-1" (:model config)))
      (is (= "mp3" (:format config))))))

(deftest generate-audio-validation-test
  (testing "should reject invalid inputs"
    ;; Test empty text
    (p/let [result (p/catch (audio/generate-audio+ "" {:voice "nova"})
                            (fn [error] {:error (.-message error)}))]
      (is (map? result))
      (is (contains? result :error))
      (is (re-find #"empty text" (get result :error))))
    
    ;; Test invalid voice
    (p/let [result (p/catch (audio/generate-audio+ "Hello" {:voice "invalid"})
                            (fn [error] {:error (.-message error)}))]
      (is (contains? result :error))
      (is (re-find #"Invalid voice" (get result :error))))))

(comment
  ;; Run tests via: (cljs.test/run-tests 'ai-presenter.audio-test-simple)
  ;; Or use Seatbelt test runner
  :rcf)
