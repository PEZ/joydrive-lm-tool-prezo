(ns ai-presenter.core
  "Pure functions for AI presenter state management.
   
   State structure (integrated with next-slide):
   {:next/active? boolean
    :next/active-slide number
    :next/config-path vector
    ;; AI Presenter state
    :ai-presenter/status keyword           ; :inactive, :active, :presenting, :paused, :error
    :ai-presenter/slides vector            ; List of slide file paths
    :ai-presenter/slide-index number       ; Current slide index
    :ai-presenter/current-slide string     ; Current slide file path
    :ai-presenter/current-audio string     ; Current audio file path
    :ai-presenter/audio-cache map          ; slide-name -> audio-file-path
   }")

(defn initial-state
  "Create the initial AI presenter state."
  []
  {:ai-presenter/status :ai-presenter/inactive
   :ai-presenter/slides []
   :ai-presenter/slide-index 0
   :ai-presenter/current-slide nil
   :ai-presenter/current-audio nil
   :ai-presenter/audio-cache {}})

(defn can-activate?
  "Check if the presenter can be activated from the current state."
  [state]
  (#{:ai-presenter/inactive} (:ai-presenter/status state)))

(defn activate-presenter
  "Activate the presenter with a list of slides."
  [state slides]
  (if (can-activate? state)
    (assoc state
           :ai-presenter/status :ai-presenter/active
           :ai-presenter/slides slides
           :ai-presenter/slide-index 0
           :ai-presenter/current-slide (when (seq slides) (first slides)))
    state))

(defn can-start-presenting?
  "Check if we can start presenting from the current state."
  [state]
  (#{:ai-presenter/active} (:ai-presenter/status state)))

(defn start-presenting
  "Transition from active to presenting state."
  [state]
  (if (can-start-presenting? state)
    (assoc state :ai-presenter/status :ai-presenter/presenting)
    state))

(defn can-pause?
  "Check if the presenter can be paused."
  [state]
  (#{:ai-presenter/presenting} (:ai-presenter/status state)))

(defn pause-presenter
  "Pause the presenter."
  [state]
  (if (can-pause? state)
    (assoc state :ai-presenter/status :ai-presenter/paused)
    state))

(defn can-resume?
  "Check if the presenter can be resumed."
  [state]
  (#{:ai-presenter/paused} (:ai-presenter/status state)))

(defn resume-presenter
  "Resume the presenter."
  [state]
  (if (can-resume? state)
    (assoc state :ai-presenter/status :ai-presenter/presenting)
    state))

(defn next-slide
  "Move to the next slide, bounded by slide list length."
  [state]
  (let [slides (:ai-presenter/slides state)
        current-index (:ai-presenter/slide-index state)]
    (if (empty? slides)
      state
      (let [max-index (dec (count slides))
            new-index (min (inc current-index) max-index)]
        (assoc state
               :ai-presenter/slide-index new-index
               :ai-presenter/current-slide (nth slides new-index))))))

(defn previous-slide
  "Move to the previous slide, bounded by 0."
  [state]
  (let [slides (:ai-presenter/slides state)
        current-index (:ai-presenter/slide-index state)]
    (if (empty? slides)
      state
      (let [new-index (max (dec current-index) 0)]
        (assoc state
               :ai-presenter/slide-index new-index
               :ai-presenter/current-slide (nth slides new-index))))))

(defn deactivate-presenter
  "Deactivate the presenter and clear state."
  [state]
  (assoc state
         :ai-presenter/status :ai-presenter/inactive
         :ai-presenter/slides []
         :ai-presenter/slide-index 0
         :ai-presenter/current-slide nil
         :ai-presenter/current-audio nil))
