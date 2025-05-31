(ns ai-presenter.integration-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [ai-presenter.core :as core]
   [ai-presenter.integration :as integration]))

(deftest merge-states-test
  (testing "Merging AI presenter state with next-slide state"
    (let [next-slide-state {:next/active? true
                            :next/active-slide 2
                            :next/config-path ["slides.edn"]}
          ai-presenter-state (-> (core/initial-state)
                                 (core/activate-presenter ["slide1.md" "slide2.md" "slide3.md"])
                                 (core/start-presenting))
          merged-state (integration/merge-states next-slide-state ai-presenter-state)]
      
      (is (= true (:next/active? merged-state))
          "Should preserve next-slide state")
      
      (is (= 2 (:next/active-slide merged-state))
          "Should preserve next-slide active slide")
      
      (is (= :ai-presenter/presenting (:ai-presenter/status merged-state))
          "Should preserve AI presenter status")
      
      (is (= "slide1.md" (:ai-presenter/current-slide merged-state))
          "Should preserve AI presenter current slide"))))

(deftest sync-slide-indices-test
  (testing "Synchronizing slide indices between systems"
    (let [slides ["intro.md" "main.md" "conclusion.md"]
          ai-state (-> (core/initial-state)
                       (core/activate-presenter slides)
                       (core/next-slide)
                       (core/next-slide)) ; Now at index 2, slide "conclusion.md"
          next-slide-state {:next/active? true
                            :next/active-slide 1} ; At index 1
          
          ; Sync AI presenter to next-slide
          synced-to-next (integration/sync-ai-to-next-slide ai-state next-slide-state)
          
          ; Sync next-slide to AI presenter  
          synced-to-ai (integration/sync-next-slide-to-ai next-slide-state ai-state)]
      
      (is (= 1 (:ai-presenter/slide-index synced-to-next))
          "Should sync AI presenter index to next-slide index")
      
      (is (= "main.md" (:ai-presenter/current-slide synced-to-next))
          "Should update AI presenter current slide")
      
      (is (= 2 (:next/active-slide synced-to-ai))
          "Should sync next-slide index to AI presenter index"))))

(deftest unified-state-operations-test
  (testing "Unified state operations that affect both systems"
    (let [initial-unified (integration/create-unified-state ["slide1.md" "slide2.md"])
          
          ; Activate both systems
          activated (integration/activate-unified initial-unified)
          
          ; Navigate forward
          next-state (integration/next-slide-unified activated)
          
          ; Start AI presenting
          presenting (integration/start-presenting-unified next-state)]
      
      (is (= true (:next/active? activated))
          "Should activate next-slide system")
      
      (is (= :ai-presenter/active (:ai-presenter/status activated))
          "Should activate AI presenter system")
      
      (is (= 1 (:ai-presenter/slide-index next-state))
          "Should update AI presenter slide index")
      
      (is (= 1 (:next/active-slide next-state))
          "Should update next-slide index")
      
      (is (= :ai-presenter/presenting (:ai-presenter/status presenting))
          "Should start AI presenting")
      
      ; Verify both systems are in sync
      (is (= (:ai-presenter/slide-index presenting) (:next/active-slide presenting))
          "Both systems should have same slide index"))))

(deftest error-state-synchronization-test
  (testing "Error state handling in unified operations"
    (let [unified-state (integration/create-unified-state [])
          
          ; Try to start presenting with no slides
          error-state (integration/start-presenting-unified unified-state)]
      
      (is (= :ai-presenter/error (:ai-presenter/status error-state))
          "Should set error status with no slides")
      
      (is (contains? error-state :ai-presenter/error-message)
          "Should include error message"))))

(deftest state-cleanup-test
  (testing "Cleanup when deactivating unified systems"
    (let [active-state (-> (integration/create-unified-state ["slide1.md"])
                           (integration/activate-unified)
                           (integration/start-presenting-unified))
          cleaned-state (integration/deactivate-unified active-state)]
      
      (is (= false (:next/active? cleaned-state))
          "Should deactivate next-slide system")
      
      (is (= :ai-presenter/inactive (:ai-presenter/status cleaned-state))
          "Should deactivate AI presenter system")
      
      (is (nil? (:ai-presenter/current-slide cleaned-state))
          "Should clear current slide")
      
      (is (= 0 (:next/active-slide cleaned-state))
          "Should reset next-slide index"))))
