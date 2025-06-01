(ns ai-presenter.audio-playback
  "Core playback module."
  (:require ["vscode" :as vscode]
            ["path" :as path]
            [promesa.core :as p]
            [joyride.core :as joy]
            ["fs" :as fs]))

(def resource-dir (path/join
                   (-> vscode/workspace.workspaceFolders
                       first
                       .-uri
                       .-fsPath)
                   ".joyride"
                   "resources"))

(defonce !state (atom {:webview nil
                       :status-resolvers {}
                       :load-resolvers {}
                       :last-known-status nil}))

;; =============================================================================
;; PURE FUNCTIONS - Functional Core
;; =============================================================================

(defn can-play?
  "Pure function: Check if audio system is ready to play"
  [status]
  (and (:userGestureComplete status)
       (:audioLoaded status)))

(defn add-status-resolver
  "Pure function: Add a status resolver to state"
  [state id resolver]
  (assoc-in state [:status-resolvers id] resolver))

(defn add-load-resolver
  "Pure function: Add a load resolver to state"
  [state id resolver]
  (assoc-in state [:load-resolvers id] resolver))

(defn remove-resolver
  "Pure function: Remove a resolver from state"
  [state resolver-type id]
  (update state resolver-type dissoc id))

(defn update-last-status
  "Pure function: Update the cached status"
  [state new-status]
  (assoc state :last-known-status new-status))

(defn get-play-readiness
  "Pure function: Analyze play readiness and return info"
  [status]
  (cond
    (not (:userGestureComplete status))
    {:ready? false
     :reason :no-user-gesture
     :message "Please click Enable Audio first"}

    (not (:audioLoaded status))
    {:ready? false
     :reason :audio-not-loaded
     :message "Audio not yet loaded/ready"}

    :else
    {:ready? true
     :reason :ready
     :message "Ready to play"}))

;; =============================================================================
;; IMPERATIVE SHELL - Side Effects
;; =============================================================================

(defn dispose-audio-webview! []
  (when-let [webview (:webview @!state)]
    (.dispose webview)
    (swap! !state assoc :webview nil)))

(defn create-audio-webview! []
  (dispose-audio-webview!)
  (let [webview (vscode/window.createWebviewPanel
                 "audio-service-webview"
                 "Audio Service"
                 (.-One vscode/ViewColumn)
                 (clj->js {:enableScripts true
                           :retainContextWhenHidden true}))]
    (swap! !state assoc :webview webview)
    webview))

(defn load-html-from-file!
  "Load HTML content from external file to avoid string parsing issues"
  []
  (when-let [webview (:webview @!state)]
    (let [html-path (path/join resource-dir "audio-service.html")
          html-content (fs/readFileSync html-path "utf8")]
      (set! (-> webview .-webview .-html) html-content))))

(defn init-audio-service!
  "Improved version using external HTML file"
  []
  (create-audio-webview!)
  (load-html-from-file!)
  (.onDidReceiveMessage
   (.-webview (:webview @!state))
   (fn [message]
     (println "audio-service-webview message:" message)
     ;; Handle status responses
     (when (= (.-type message) "statusResponse")
       (let [status (js->clj (.-status message) :keywordize-keys true)
             current-state @!state]
         ;; Update cached status using pure function
         (swap! !state update-last-status status)
         ;; Resolve pending status request
         (when-let [resolver (get-in current-state [:status-resolvers "current"])]
           (resolver status)
           (swap! !state remove-resolver :status-resolvers "current"))))
     ;; Handle audio ready notifications
     (when (= (.-type message) "audioReady")
       (println "🎵 Audio ready notification received!")
       (let [load-data (js->clj message :keywordize-keys true)
             current-state @!state
             audio-id (or (:id load-data) "default")]
         ;; Try to find resolver by ID, or any resolver if ID not found
         (if-let [resolver-map (get-in current-state [:load-resolvers audio-id])]
           (do
             ((:resolve resolver-map) load-data)
             (swap! !state remove-resolver :load-resolvers audio-id))
           ;; If no resolver found for the specific ID, try to resolve any pending resolver
           (when-let [[found-id resolver-map] (first (:load-resolvers current-state))]
             (println "🔧 No resolver for audio-id" audio-id ", using" found-id)
             ((:resolve resolver-map) load-data)
             (swap! !state remove-resolver :load-resolvers found-id)))))
     ;; Handle audio load error notifications
     (when (= (.-type message) "audioLoadError")
       (println "❌ Audio load error notification received!")
       (let [error-data (js->clj message :keywordize-keys true)
             current-state @!state
             audio-id (or (:id error-data) "default")]
         ;; Try to find any resolver (since webview might not send the correct ID)
         (if-let [resolver-map (get-in current-state [:load-resolvers audio-id])]
           (do
             ;; Reject the promise with error details
             ((:reject resolver-map) (js/Error. (:error error-data)))
             (swap! !state remove-resolver :load-resolvers audio-id))
           ;; If no resolver found for the specific ID, try to reject any pending resolver
           (when-let [[found-id resolver-map] (first (:load-resolvers current-state))]
             (println "🔧 No resolver for audio-id" audio-id ", using" found-id)
             ((:reject resolver-map) (js/Error. (:error error-data)))
             (swap! !state remove-resolver :load-resolvers found-id)))))
     message))
  (:webview @!state))

(defn send-audio-command! [command & args]
  (when-let [webview (:webview @!state)]
    (.postMessage
     (.-webview webview)
     (clj->js (apply merge {:command (name command)} args)))))

(defn load-audio! [local-file-path & {:keys [id]}]
  (def local-file-path local-file-path)
  (let [webview (:webview @!state)
        _ (def webview webview)
        audio-uri (.asWebviewUri (.-webview webview) (vscode/Uri.file local-file-path))]
    (def audio-uri audio-uri)
    (send-audio-command! :load {:audioPath (str audio-uri)
                                :id (or id "default")})))

(defn play-audio-smart!+
  "Smart play that checks readiness first and returns comprehensive info"
  [& {:keys [id]}]
  (p/let [status (get-audio-status!+)
          readiness (get-play-readiness status)]
    (if (:ready? readiness)
      (do
        (send-audio-command! :play {:id (or id "default")})
        ;; Get updated status after play command
        (p/let [new-status (get-audio-status!+)]
          {:success true
           :action :played
           :readiness readiness
           :status-before status
           :status-after new-status}))
      {:success false
       :action :blocked
       :readiness readiness
       :status status})))

(defn play-audio! [& {:keys [id]}]
  (send-audio-command! :play {:id (or id "default")}))

(defn pause-audio! [& {:keys [id]}]
  (send-audio-command! :pause {:id (or id "default")}))

(defn stop-audio! [& {:keys [id]}]
  (send-audio-command! :stop {:id (or id "default")}))

(defn set-volume! [volume & {:keys [id]}]
  (send-audio-command! :volume {:volume volume :id (or id "default")}))

(defn load-and-play-audio!+ [file-path]
  (p/let [load-result (load-audio-promise!+ file-path)
          play-result (play-audio!)]
    {:load-result load-result
     :play-result play-result
     :success true}))

(defn get-audio-status!+ []
  (p/create
   (fn [resolve reject]
     ;; Store the resolver using pure function
     (swap! !state add-status-resolver "current" resolve)
     ;; Request status from webview
     (send-audio-command! :status)
     ;; Timeout after 5 seconds
     (js/setTimeout #(do
                       (swap! !state remove-resolver :status-resolvers "current")
                       (reject "Status request timeout")) 5000))))

;; Enhanced load and play that waits for proper loading
(defn load-and-play-audio-properly!+ [file-path]
  (p/let [_ (load-audio! file-path)
          ;; Wait a bit for loading to start
          _ (p/delay 100)
          ;; Check status repeatedly until loaded
          status (loop [attempts 0]
                   (p/let [current-status (get-audio-status!+)]
                     (if (or (:audioLoaded current-status)
                             (> attempts 20)) ;; 2 second timeout
                       current-status
                       (do
                         (p/delay 100)
                         (recur (inc attempts))))))]
    (if (:audioLoaded status)
      (do
        (println "Audio loaded successfully, attempting play...")
        (p/let [play-result (play-audio!)]
          {:load-status status
           :play-result play-result
           :success true}))
      {:load-status status
       :play-result nil
       :success false
       :error "Audio failed to load in time"})))

(defn load-audio-promise!+
  "Returns a promise that resolves when audio is loaded and ready to play, or rejects with detailed error info"
  [local-file-path & {:keys [id timeout-ms] :or {timeout-ms 10000}}]
  (let [audio-id (or id "default")]
    (p/create
     (fn [resolve reject]
       ;; Store both resolve and reject functions
       (swap! !state add-load-resolver audio-id {:resolve resolve :reject reject})
       ;; Send load command using existing function
       (let [webview (:webview @!state)
             audio-uri (.asWebviewUri (.-webview webview) (vscode/Uri.file local-file-path))]
         (send-audio-command! :load {:audioPath (str audio-uri)
                                     :id audio-id}))
       ;; Enhanced timeout with status check
       (js/setTimeout
        #(p/let [final-status (get-audio-status!+)]
           (swap! !state remove-resolver :load-resolvers audio-id)
           (if (and (:audioDataReady final-status)
                    (not (:userGestureComplete final-status)))
             ;; Audio data loaded but waiting for user gesture
             (reject (js/Error.
                      (str "Audio loaded but requires user gesture. Duration: "
                           (:audioDuration final-status) "s. Please click 'Enable Audio'.")))
             ;; True timeout or other issue
             (reject (js/Error.
                      (str "Audio load timeout for: " local-file-path
                           ". Status: " (:playbackState final-status)
                           (when (:lastError final-status)
                             (str ". Error: " (:lastError final-status))))))))
        timeout-ms)))))

;; Simple load-and-play using the promise-based load
(defn load-and-play-audio-simple!+ [file-path]
  (p/let [load-result (load-audio-promise!+ file-path)
          play-result (play-audio!)]
    {:load-result load-result
     :play-result play-result
     :success true}))

(comment
  (p/let [load+ (load-audio! "/Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/slides/voice/test-playback.mp3")]
    (def load+ load+))

  (p/let [play+ (play-audio!)]
    (def play+ play+))

  (p/let [load-and-play+ (load-and-play-audio!+ "/Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/slides/voice/test-playback.mp3")]
    (def load-and-play+ load-and-play+))


  (p/let [pause+ (pause-audio!)]
    (def pause+ pause+))

  (p/let [set-volume+ (set-volume! 0.1)]
    (def set-volume+ set-volume+))

  (p/let [stop+ (stop-audio!)]
    (def stop+ stop+))

  :rcf)