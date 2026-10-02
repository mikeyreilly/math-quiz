(ns quaxt.arithmetic-challenge-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [quaxt.arithmetic-challenge :as ac]))

(deftest toggle-test
  (is (= #{} (ac/toggle #{"+"} "+")))
  (is (= #{"+" "-"} (ac/toggle #{"+"} "-"))))

(deftest right?-test
  (testing "correct answers"
    (is (ac/right? {:op "+" :x 2 :y 3} 5))
    (is (ac/right? {:op "-" :x 7 :y 3} 4))
    (is (ac/right? {:op "*" :x 3 :y 4} 12))
    (is (ac/right? {:op "/" :x 12 :y 4} 3)))
  (testing "wrong answers"
    (is (not (ac/right? {:op "+" :x 2 :y 3} 6)))))

(deftest question-to-string-test
  (is (= "3 \u00D7 4 = 12"
         (ac/question-to-string {:op "*" :x 3 :y 4} "12"))))

(deftest results-total-test
  (is (= [1 150]
         (ac/results-total
          [{:question {:op "+" :x 1 :y 1} :user-answer 2 :time 100}
           {:question {:op "+" :x 1 :y 1} :user-answer 3 :time 50}]))))
