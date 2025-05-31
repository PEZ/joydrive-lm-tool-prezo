(ns ai-presenter.audio-playback
  "Core playback module.
   * Sets up the webview, in view column One
   * with the html from the file `audio-service.html`
   * "
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

(def !audio-webview (atom nil))

(defn dispose-audio-webview! []
  (when @!audio-webview
    (.dispose @!audio-webview)
    (reset! !audio-webview nil)))

(defn create-audio-webview! []
  (dispose-audio-webview!)
  (reset! !audio-webview
          (vscode/window.createWebviewPanel
           "audio-service-webview"
           "Audio Service"
           (.-One vscode/ViewColumn)
           (clj->js {:enableScripts true
                     :retainContextWhenHidden true}))))

(defn load-html-from-file!
  "Load HTML content from external file to avoid string parsing issues"
  []
  (when @!audio-webview
    (let [html-path (path/join resource-dir "audio-service.html")
          html-content (fs/readFileSync html-path "utf8")]
      (def html-content html-content)
      (set! (-> @!audio-webview .-webview .-html) html-content))))

(defn init-audio-service!
  "Improved version using external HTML file"
  []
  (create-audio-webview!)
  (load-html-from-file!)
  (.onDidReceiveMessage
   (.-webview @!audio-webview)
   (fn [message]
     (println "audio-service-webview message:"  message)
     message))
  @!audio-webview)

(defn send-audio-command! [command & args]
  (when @!audio-webview
    (.postMessage
     (.-webview @!audio-webview)
     (clj->js (apply merge {:command (name command)} args)))))

(defn load-audio! [audio-path & {:keys [id]}]
  (send-audio-command! :load {:audioPath audio-path :id (or id "default")}))

(defn play-audio! [& {:keys [id]}]
  (send-audio-command! :play {:id (or id "default")}))

(defn pause-audio! [& {:keys [id]}]
  (send-audio-command! :pause {:id (or id "default")}))

(defn stop-audio! [& {:keys [id]}]
  (send-audio-command! :stop {:id (or id "default")}))

(defn set-volume! [volume & {:keys [id]}]
  (send-audio-command! :volume {:volume volume :id (or id "default")}))

(comment
  (init-audio-service!)

  (p/let [load+ (load-audio! "/Users/pez/Projects/Meetup/joydrive-lm-tool-prezo/slides/voice/test-playback.mp3")]
    (def load+ load+))

  (p/let [play+ (play-audio!)]
    (def play+ play+))

  (p/let [pause+ (pause-audio!)]
    (def pause+ pause+))

  (p/let [set-volume+ (set-volume! 0.5)]
    (def set-volume+ set-volume+))

  (p/let [stop+ (stop-audio!)]
    (def stop+ stop+))

  (dispose-audio-webview!)

  :rcf)