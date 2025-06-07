(ns ai-chat.human-intelligence
  (:require
   ["vscode" :as vscode]
   [promesa.core :as p]))

(defn quick-pick-config [{:ui/keys [question question-context items-with-other]}]
  {:title question
   :placeholder question-context
   :items (clj->js items-with-other)
   :ignoreFocusOut true
   :canSelectMany false})

(defn apply-quick-pick-config! [quick-pick config]
  (set! (.-title quick-pick) (:title config))
  (set! (.-placeholder quick-pick) (:placeholder config))
  (set! (.-items quick-pick) (:items config))
  (set! (.-ignoreFocusOut quick-pick) (:ignoreFocusOut config))
  (set! (.-canSelectMany quick-pick) (:canSelectMany config))
  quick-pick)

(defn input-box-config [{:ui/keys [question question-context]}]
  {:title question
   :placeholder question-context
   :ignoreFocusOut true})

(defn apply-input-box-config! [input-box config]
  (set! (.-title input-box) (:title config))
  (set! (.-placeholder input-box) (:placeholder config))
  (set! (.-ignoreFocusOut input-box) (:ignoreFocusOut config))
  input-box)

(defn prepare-items [{:ui/keys [items]}]
  (let [original-items-map (into {} (map (fn [item]
                                           [(if (string? item) item (.-label item)) item])
                                         items))
        items-with-other (concat
                          (map (fn [item]
                                 (if (string? item)
                                   #js {:label item}
                                   item))
                               items)
                          [#js {:label "Other" :description "Enter custom value"}])]
    {:ui/original-items-map original-items-map
     :ui/items-with-other items-with-other}))

(defn handle-input-box-accept [state input-value]
  {:new-state (assoc state :input-box nil)
   :actions [#(.dispose (:input-box state))]
   :result input-value})

(defn handle-input-box-hide [state]
  (when (:input-box state)
    {:new-state (assoc state :input-box nil)
     :result ::cancelled}))

(defn setup-input-box! [{:ui/keys [question question-context resolve]
                         :state/keys [!state]}]
  (let [input-box (vscode/window.createInputBox)
        config (input-box-config {:ui/question question
                                  :ui/question-context question-context})]
    (swap! !state assoc :input-box input-box)
    (apply-input-box-config! input-box config)

    (.onDidAccept input-box
                  (fn []
                    (let [input-value (.-value input-box)
                          state @!state
                          {:keys [new-state actions result]} (handle-input-box-accept state input-value)]
                      (reset! !state new-state)
                      (doseq [action actions]
                        (action))
                      (resolve result))))

    (.onDidHide input-box
                (fn []
                  (let [state @!state
                        result (handle-input-box-hide state)]
                    (when result
                      (reset! !state (:new-state result))
                      (resolve (:result result))))))

    (.show input-box)))

(defn handle-quick-pick-selection [state selection original-items-map]
  (if (= (.-label selection) "Other")
    {:new-state (assoc state :selection-made true)
     :actions [#(.dispose (:quick-pick state))
               #(when-let [timeout-id (:timeout-id state)]
                  (js/clearTimeout timeout-id))]
     :show-input-box true}
    (let [selected-label (.-label selection)
          original-item (get original-items-map selected-label)]
      {:new-state (assoc state :selection-made true)
       :actions [#(.dispose (:quick-pick state))
                 #(when-let [timeout-id (:timeout-id state)]
                    (js/clearTimeout timeout-id))]
       :result (or original-item selected-label)})))

(defn handle-quick-pick-accept! [{:ui/keys [resolve question question-context original-items-map]
                                  :state/keys [!state]}]
  (let [state @!state
        selection (first (.-selectedItems (:quick-pick state)))
        {:keys [new-state actions show-input-box result]} (handle-quick-pick-selection state selection original-items-map)]

    (reset! !state new-state)

    (doseq [action actions]
      (action))

    (cond
      show-input-box (setup-input-box! {:ui/question question
                                        :ui/question-context question-context
                                        :ui/resolve resolve
                                        :state/!state !state})
      result (resolve result))))

(defn handle-quick-pick-hide [state]
  (when-not (:selection-made state)
    {:actions [#(when-let [timeout-id (:timeout-id state)]
                  (js/clearTimeout timeout-id))]
     :result (if (:timed-out state)
               ::timeout
               ::cancelled)}))

(defn handle-quick-pick-hide! [{:ui/keys [resolve]
                                :state/keys [!state]}]
  (let [state @!state
        result (handle-quick-pick-hide state)]
    (when result
      (doseq [action (:actions result)]
        (action))
      (resolve (:result result)))))

(defn setup-timeout! [{:ui/keys [timeout-s quick-pick resolve]
                       :state/keys [!state]}]
  (js/setTimeout
   (fn []
     (let [current-state @!state]
       (when-not (:selection-made current-state)
         (swap! !state assoc :timed-out true)
         (when-let [input-box (:input-box current-state)]
           (.dispose input-box))
         (.dispose quick-pick)
         (resolve ::timeout))))
   (* 1000 timeout-s)))

(defn ask!+
  "Creates a persistent quick pick menu with the provided items and an 'Other' option.
   When 'Other' is selected, it shows an input box. Returns a promise that resolves
   to the selected item (the item exactly as provided) or the text entered in the input box.
   `question` is used for the title of the quick-pick
   `question-context` is used for placeholder
   Times out after `timeout-s` seconds if no selection is made.
   The timeout is disabled if the user selects 'Other'.
   Returns ::timeout if timed out, ::cancelled if user closed without selecting."
  [question question-context items timeout-s]
  (p/create
   (fn [resolve _reject]
     (let [quick-pick (vscode/window.createQuickPick)
           _ (def quick-pick quick-pick)
           {:ui/keys [original-items-map items-with-other]} (prepare-items {:ui/items items})
           !state (atom {:selection-made false
                         :changed-active-count 0
                         :timed-out false
                         :input-box nil
                         :timeout-id nil
                         :quick-pick quick-pick})
           quick-pick-cfg (quick-pick-config {:ui/question question
                                              :ui/question-context question-context
                                              :ui/items-with-other items-with-other})]

       (apply-quick-pick-config! quick-pick quick-pick-cfg)

       ;; Set up event handlers before showing the quick pick
       (.onDidAccept quick-pick
                     (fn []
                       (handle-quick-pick-accept!
                        {:ui/resolve resolve
                         :ui/question question
                         :ui/question-context question-context
                         :ui/original-items-map original-items-map
                         :state/!state !state})))

       (.onDidHide quick-pick
                   (fn []
                     (handle-quick-pick-hide!
                      {:ui/resolve resolve
                       :state/!state !state})))

       (.onDidChangeActive quick-pick
                           (fn [_items]
                             (let [state @!state]
                               (when-let [timeout-id (:timeout-id state)]
                                 (when-not (zero? (:changed-active-count state))
                                   (swap! !state dissoc :timeout-id)
                                   (js/clearTimeout timeout-id)))
                               (swap! !state update :changed-active-count inc) )))

       (.show quick-pick)

       (swap! !state assoc :timeout-id
              (setup-timeout! {:ui/timeout-s timeout-s
                               :ui/quick-pick quick-pick
                               :ui/resolve resolve
                               :state/!state !state}))))))

(comment
  (p/let [answer (ask!+ "hello" "because" ["foo"
                                           "bar"
                                           #js {:label "baz"
                                                :description "elaborate on baz"}] 3)]
    (def answer answer))
  :rcf)