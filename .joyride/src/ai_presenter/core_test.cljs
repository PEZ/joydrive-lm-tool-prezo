(ns ai-presenter.core-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [ai-presenter.core :as core]))

(deftest initial-state-test
  (testing "Initial presenter state"
    (let [state (core/initial-state)]
      (is (= :ai-presenter/inactive (:ai-presenter/status state))
          "Should start in inactive status")
      (is (nil? (:ai-presenter/current-slide state))
          "Should have no current slide initially")
      (is (nil? (:ai-presenter/current-audio state))
          "Should have no current audio initially")
      (is (= {} (:ai-presenter/audio-cache state))
          "Should have empty audio cache initially"))))

(deftest can-activate-test
  (testing "State transitions: activation validation"
    (is (core/can-activate? (core/initial-state))
        "Should be able to activate from inactive state")
    
    (is (not (core/can-activate? {:ai-presenter/status :ai-presenter/active}))
        "Should not be able to activate when already active")
    
    (is (not (core/can-activate? {:ai-presenter/status :ai-presenter/presenting}))
        "Should not be able to activate when presenting")))

(deftest activate-presenter-test
  (testing "Activate presenter with slides"
    (let [slides ["slide1.md" "slide2.md" "slide3.md"]
          initial-state (core/initial-state)
          activated-state (core/activate-presenter initial-state slides)]
      
      (is (= :ai-presenter/active (:ai-presenter/status activated-state))
          "Should be in active status after activation")
      
      (is (= slides (:ai-presenter/slides activated-state))
          "Should store the provided slides")
      
      (is (= 0 (:ai-presenter/slide-index activated-state))
          "Should start at slide index 0")
      
      (is (= (first slides) (:ai-presenter/current-slide activated-state))
          "Should set current slide to first slide"))))

(deftest can-start-presenting-test
  (testing "Can start presenting validation"
    (let [active-state (-> (core/initial-state)
                           (core/activate-presenter ["slide1.md"]))]
      (is (core/can-start-presenting? active-state)
          "Should be able to start presenting from active state")
      
      (is (not (core/can-start-presenting? (core/initial-state)))
          "Should not be able to start presenting from inactive state")
      
      (is (not (core/can-start-presenting? 
                 (assoc active-state :ai-presenter/status :ai-presenter/presenting)))
          "Should not be able to start presenting when already presenting"))))

(deftest start-presenting-test
  (testing "Start presenting transition"
    (let [active-state (-> (core/initial-state)
                           (core/activate-presenter ["slide1.md" "slide2.md"]))
          presenting-state (core/start-presenting active-state)]
      
      (is (= :ai-presenter/presenting (:ai-presenter/status presenting-state))
          "Should be in presenting status")
      
      (is (= "slide1.md" (:ai-presenter/current-slide presenting-state))
          "Should maintain current slide"))))

(deftest can-pause-resume-test
  (testing "Pause and resume validation"
    (let [presenting-state {:ai-presenter/status :ai-presenter/presenting}
          paused-state {:ai-presenter/status :ai-presenter/paused}]
      
      (is (core/can-pause? presenting-state)
          "Should be able to pause when presenting")
      
      (is (not (core/can-pause? paused-state))
          "Should not be able to pause when already paused")
      
      (is (core/can-resume? paused-state)
          "Should be able to resume when paused")
      
      (is (not (core/can-resume? presenting-state))
          "Should not be able to resume when presenting"))))

(deftest pause-resume-test
  (testing "Pause and resume transitions"
    (let [presenting-state {:ai-presenter/status :ai-presenter/presenting
                            :ai-presenter/current-slide "slide1.md"}
          paused-state (core/pause-presenter presenting-state)
          resumed-state (core/resume-presenter paused-state)]
      
      (is (= :ai-presenter/paused (:ai-presenter/status paused-state))
          "Should be in paused status after pause")
      
      (is (= :ai-presenter/presenting (:ai-presenter/status resumed-state))
          "Should be in presenting status after resume")
      
      (is (= "slide1.md" (:ai-presenter/current-slide resumed-state))
          "Should maintain slide state through pause/resume"))))

(deftest next-previous-slide-test
  (testing "Slide navigation"
    (let [state (-> (core/initial-state)
                    (core/activate-presenter ["slide1.md" "slide2.md" "slide3.md"]))
          next-state (core/next-slide state)
          prev-state (core/previous-slide next-state)]
      
      (is (= 1 (:ai-presenter/slide-index next-state))
          "Should increment slide index")
      
      (is (= "slide2.md" (:ai-presenter/current-slide next-state))
          "Should update current slide")
      
      (is (= 0 (:ai-presenter/slide-index prev-state))
          "Should decrement slide index")
      
      (is (= "slide1.md" (:ai-presenter/current-slide prev-state))
          "Should update current slide backwards"))))

(deftest slide-navigation-bounds-test
  (testing "Slide navigation boundary conditions"
    (let [state (-> (core/initial-state)
                    (core/activate-presenter ["slide1.md" "slide2.md"]))
          ; Try to go before first slide
          before-first (core/previous-slide state)
          ; Go to last slide and try to go beyond
          at-last (core/next-slide state)
          beyond-last (core/next-slide at-last)]
      
      (is (= 0 (:ai-presenter/slide-index before-first))
          "Should not go below index 0")
      
      (is (= "slide1.md" (:ai-presenter/current-slide before-first))
          "Should stay at first slide")
      
      (is (= 1 (:ai-presenter/slide-index beyond-last))
          "Should not go beyond last slide")
      
      (is (= "slide2.md" (:ai-presenter/current-slide beyond-last))
          "Should stay at last slide"))))

(deftest deactivate-presenter-test
  (testing "Deactivate presenter"
    (let [active-state (-> (core/initial-state)
                           (core/activate-presenter ["slide1.md"]))
          deactivated-state (core/deactivate-presenter active-state)]
      
      (is (= :ai-presenter/inactive (:ai-presenter/status deactivated-state))
          "Should return to inactive status")
      
      (is (nil? (:ai-presenter/current-slide deactivated-state))
          "Should clear current slide")
      
      (is (empty? (:ai-presenter/slides deactivated-state))
          "Should clear slides list"))))

;; Integration tests for complete workflows

(deftest complete-presentation-workflow-test
  (testing "Complete presentation workflow"
    (let [slides ["intro.md" "main.md" "conclusion.md"]
          ; Start inactive
          state (core/initial-state)
          ; Activate with slides
          activated (core/activate-presenter state slides)
          ; Start presenting
          presenting (core/start-presenting activated)
          ; Navigate slides
          on-slide-2 (core/next-slide presenting)
          on-slide-3 (core/next-slide on-slide-2)
          ; Pause
          paused (core/pause-presenter on-slide-3)
          ; Resume  
          resumed (core/resume-presenter paused)
          ; Deactivate
          final (core/deactivate-presenter resumed)]
      
      (is (= "intro.md" (:ai-presenter/current-slide activated))
          "Should start on first slide after activation")
      
      (is (= :ai-presenter/presenting (:ai-presenter/status presenting))
          "Should be presenting after start")
      
      (is (= "conclusion.md" (:ai-presenter/current-slide on-slide-3))
          "Should navigate to last slide")
      
      (is (= :ai-presenter/paused (:ai-presenter/status paused))
          "Should be paused")
      
      (is (= "conclusion.md" (:ai-presenter/current-slide paused))
          "Should maintain slide during pause")
      
      (is (= :ai-presenter/presenting (:ai-presenter/status resumed))
          "Should resume to presenting")
      
      (is (= :ai-presenter/inactive (:ai-presenter/status final))
          "Should return to inactive after deactivation"))))

(deftest error-state-handling-test
  (testing "Error state transitions"
    (let [state {:ai-presenter/status :ai-presenter/error
                 :ai-presenter/slides ["slide1.md"]}]
      
      (is (not (core/can-activate? state))
          "Cannot activate from error state")
      
      (is (not (core/can-start-presenting? state))
          "Cannot start presenting from error state")
      
      (is (not (core/can-pause? state))
          "Cannot pause from error state")
      
      (is (not (core/can-resume? state))
          "Cannot resume from error state"))))

(deftest empty-slides-handling-test
  (testing "Empty slides list handling"
    (let [state (core/activate-presenter (core/initial-state) [])]
      
      (is (= [] (:ai-presenter/slides state))
          "Should handle empty slides list")
      
      (is (nil? (:ai-presenter/current-slide state))
          "Should have no current slide with empty list")
      
      ; Navigation should be safe with empty slides
      (let [next-state (core/next-slide state)
            prev-state (core/previous-slide state)]
        
        (is (= 0 (:ai-presenter/slide-index next-state))
            "Should stay at index 0 with empty slides")
        
        (is (= 0 (:ai-presenter/slide-index prev-state))
            "Should stay at index 0 with empty slides")))))

(deftest state-immutability-test
  (testing "State functions are pure and immutable"
    (let [original-state (-> (core/initial-state)
                             (core/activate-presenter ["slide1.md" "slide2.md"]))
          modified-state (-> original-state
                             (core/start-presenting)
                             (core/next-slide)
                             (core/pause-presenter))]
      
      ; Original state should be unchanged
      (is (= :ai-presenter/active (:ai-presenter/status original-state))
          "Original state should remain unchanged")
      
      (is (= 0 (:ai-presenter/slide-index original-state))
          "Original slide index should remain unchanged")
      
      ; Modified state should have changes
      (is (= :ai-presenter/paused (:ai-presenter/status modified-state))
          "Modified state should reflect changes")
      
      (is (= 1 (:ai-presenter/slide-index modified-state))
          "Modified state should have updated slide index"))))
