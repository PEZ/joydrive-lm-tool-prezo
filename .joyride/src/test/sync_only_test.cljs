(ns test.sync-only-test
  (:require
   [cljs.test :refer [deftest is testing]]))

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
