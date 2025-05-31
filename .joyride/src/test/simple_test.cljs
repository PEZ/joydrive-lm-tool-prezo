(ns test.simple-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [promesa.core :as p]
   [test.macros :refer [deftest-async]]))


(deftest-async promise-test
  (testing "Async operations work"
    (p/let [result (p/resolved "async-success")]
      (is (= "async-success" result) "Should handle promises"))))

(deftest simple-math-test
  (testing "Basic arithmetic operations"
    (is (= 4 (+ 2 2)) "Addition should work")
    (is (= 6 (* 2 3)) "Multiplication should work")
    (is (not= 5 (+ 2 2)) "Should detect wrong answers")))

(deftest data-structure-test
  (testing "Data manipulation"
    (let [data {:name "Test" :count 42}]
      (is (= "Test" (:name data)) "Should extract name")
      (is (= 42 (:count data)) "Should extract count")
      (is (= 43 (inc (:count data))) "Should increment count"))))

;; TODO: Add async tests once we figure out the macro issues
;; (deftest-async promise-test
;;   (testing "Async operations work"
;;     (p/let [result (p/resolved "async-success")]
;;       (is (= "async-success" result) "Should handle promises"))))
