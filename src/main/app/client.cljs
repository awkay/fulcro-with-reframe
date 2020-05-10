(ns app.client
  (:require
    [reagent.dom :as rdom]
    [re-frame.core :as rf]
    [app.application :refer [SPA]]
    [app.model.person :as person]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.components :as comp]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.inspect.inspect-client :as inspect]))

(defsc Person [_ _]
  {:query (fn [] '[:person/id :person/email :person/happy? {:person/children ...}])
   :ident :person/id})

(defsc Root [_ _]
  {:query         ['*]
   :initial-state {}})

(rf/reg-event-db :increment (fn [db event] (assoc db :number (inc (:number db)))))

(defn run-query [db [_ {:keys [root component]}]]
  (get
    (fdn/db->tree [{root (comp/get-query component db)}] db db)
    root))

(defn get-person [db [event-name {:keys [person/id]}]]
  (fdn/db->tree (comp/get-query Person db) (get-in db [:person/id id]) db))

(rf/reg-sub :q run-query)
(rf/reg-sub :get-person get-person)

(defn ui-person-tree
  [{:person/keys [id email happy? children]}]
  [:div {:key (str id)}
   (str "Person" id ", " email ", " happy?)
   [:button {:onClick #(comp/transact! SPA [(person/alter-mood {:person/id id :person/happy? (not happy?)})])} "Alter mood"]
   (when (seq children)
     [:ul
      (map (fn [{:person/keys [id] :as c}]
             [:li {:key (str id)}
              [ui-person-tree c]]) children)])])

(defn ui-app
  []
  (let [person @(rf/subscribe [:get-person {:person/id 3}])
        people @(rf/subscribe [:q {:root      :all-people
                                   :component Person}])]
    [:div
     [:h3 "A single person"]
     [ui-person-tree person]
     [:h3 "All people"]
     [:div
      [:button {:onClick #(df/load! SPA :all-people Person)} "Load"]
      (map (fn [person] [:div {:key (:person/id person)}
                         [ui-person-tree person]]) people)]]))

(defn render
  "Render the toplevel component for this app."
  []
  (rdom/render [ui-app] (.getElementById js/document "app")))

(defn ^:export refresh [] (render))

(defn ^:export init []
  (app/set-root! SPA Root {:initialize-state? true})
  (inspect/app-started! SPA)
  (render))

