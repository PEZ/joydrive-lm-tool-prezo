(ns ai-chat.ui
  (:require
   ["vscode" :as vscode]
   [ai-chat.util :as util]
   [clojure.string :as string]
   [promesa.core :as p]))

(defn pick-model!+ []
  (p/let [models-map (util/get-available-models+)
          items (map (fn [[id model-info]]
                       #js {:label (str (:name model-info) " (" id ")")
                            :description (str "Max tokens: " (:max-input-tokens model-info))
                            :id id})
                     models-map)
          selected-item (vscode/window.showQuickPick
                         (clj->js items)
                         #js {:placeHolder "Select a language model"
                              :canPickMany false})]
    (if selected-item
      (let [model-id (.-id selected-item)]
        (println "Selected model:" model-id)
        model-id)
      (throw (js/Error. "No model selected")))))

(comment
  (require '[ai-chat.prompter :as prompter])
  (-> (pick-model!+)
      (.then (fn [model-id]
               (prompter/ask-with-system!+ "some instructions" "hello, greet the audience, please =) " model-id)))
      (.then (fn [response]
               (def response response)
               (println "Model response:" response)
               (vscode/showInformationMessage
                (str "🤖 " response))
               response)))
  :rcf)

(defn tools-picker+
  "Display a picker with all available tools and return a vector of selected tool IDs.
   The preselected-ids parameter is a collection of tool IDs to preselect."
  [preselected-ids]
  (let [all-tools (js->clj (util/get-available-tools)
                           :keywordize-keys true)
        preselected-set (set preselected-ids)
        items (mapv (fn [{tool-name :name
                          :keys [description]}]
                      #js {:label tool-name
                           :description (-> description
                                            (string/split #"\n")
                                            first)
                           :picked (contains? preselected-set tool-name)
                           :id tool-name})
                    all-tools)]
    (p/let [selected (.showQuickPick vscode/window
                                     (into-array items)
                                     #js {:canPickMany true
                                          :placeHolder "Select tools to enable"})]
      (when selected
        (if (array? selected)
          (mapv #(.-id %) selected)
          [(.-id selected)])))))

(comment
  (p/let [tool-ids (tools-picker+ #{"joyride_evaluate_code"})]
    (def tool-ids tool-ids)
    (println (pr-str tool-ids)))
  :rcf)

