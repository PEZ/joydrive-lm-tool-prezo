(ns ai-presenter.integration
  "Integration functions for AI presenter with next-slide system.
   
   Provides unified operations that keep both systems in sync."
  (:require [ai-presenter.core :as core]))

(defn merge-states
  "Merge next-slide state with AI presenter state into unified state."
  [next-slide-state ai-presenter-state]
  (merge next-slide-state ai-presenter-state))

(defn sync-ai-to-next-slide
  "Sync AI presenter slide index to match next-slide system."
  [ai-state next-slide-state]
  (let [next-index (:next/active-slide next-slide-state)
        slides (:ai-presenter/slides ai-state)]
    (if (and (seq slides) (< next-index (count slides)))
      (assoc ai-state
             :ai-presenter/slide-index next-index
             :ai-presenter/current-slide (nth slides next-index))
      ai-state)))

(defn sync-next-slide-to-ai
  "Sync next-slide index to match AI presenter system."
  [next-slide-state ai-state]
  (let [ai-index (:ai-presenter/slide-index ai-state)]
    (assoc next-slide-state :next/active-slide ai-index)))

(defn create-unified-state
  "Create initial unified state with both systems."
  [slides]
  (merge
    {:next/active? false
     :next/active-slide 0
     :next/config-path ["slides.edn"]}
    (core/initial-state)
    {:ai-presenter/slides slides}))

(defn activate-unified
  "Activate both next-slide and AI presenter systems."
  [unified-state]
  (let [slides (:ai-presenter/slides unified-state)]
    (-> unified-state
        (assoc :next/active? true)
        (core/activate-presenter slides))))

(defn next-slide-unified
  "Move both systems to next slide in sync."
  [unified-state]
  (let [updated-ai (core/next-slide unified-state)
        ai-index (:ai-presenter/slide-index updated-ai)]
    (assoc updated-ai :next/active-slide ai-index)))

(defn previous-slide-unified
  "Move both systems to previous slide in sync."
  [unified-state]
  (let [updated-ai (core/previous-slide unified-state)
        ai-index (:ai-presenter/slide-index updated-ai)]
    (assoc updated-ai :next/active-slide ai-index)))

(defn start-presenting-unified
  "Start AI presenting with error handling."
  [unified-state]
  (let [slides (:ai-presenter/slides unified-state)]
    (if (empty? slides)
      (assoc unified-state
             :ai-presenter/status :ai-presenter/error
             :ai-presenter/error-message "Cannot start presenting: no slides available")
      (core/start-presenting unified-state))))

(defn pause-presenting-unified
  "Pause AI presenting."
  [unified-state]
  (core/pause-presenter unified-state))

(defn resume-presenting-unified
  "Resume AI presenting."
  [unified-state]
  (core/resume-presenter unified-state))

(defn deactivate-unified
  "Deactivate both systems and clean up state."
  [unified-state]
  (-> unified-state
      (assoc :next/active? false
             :next/active-slide 0)
      (core/deactivate-presenter)))
