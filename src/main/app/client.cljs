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

;; We can use Fulcro's defsc as a simple `defquery` mechanism
(defsc Address [_ _]
  {:query [:address/id :address/street :address/city]
   :ident :address/id})

(defsc Person [_ _]
  {:query (fn [] [:person/id {:person/address (comp/get-query Address)} :person/email :person/happy? {:person/children '...}])
   :ident :person/id})

(defsc Root
  "Fulcro needs a root in order to start..we just make a placeholder, it won't render anything. We can use it, however,
   to initialize the database with startup data (if we wanted)."
  [_ _]
  {:query         ['*]
   :initial-state {}})

;; Now we can just define some Reframe stuff that leverages Fulcro to denormalize data back into a tree
(defn run-query
  "Run a generalized query against the current state database, where there is a `root` key, which points to one or more
   things of `component` type."
  [db [_ {:keys [root component]}]]
  (get
    (fdn/db->tree [{root (comp/get-query component db)}] db db)
    root))

(defn run-eql "Run a raw EQL query" [db [_ eql]] (fdn/db->tree eql db db))
(defn run-eql-entity "Run a raw EQL query starting at an entity" [db [_ ident eql]] (fdn/db->tree eql (get-in db ident) db))

(defn get-person [db [event-name {:keys [person/id]}]]
  (fdn/db->tree (comp/get-query Person db) (get-in db [:person/id id]) db))

(rf/reg-sub :q run-query)
(rf/reg-sub :eql run-eql)
(rf/reg-sub :eql-entity run-eql-entity)
(rf/reg-sub :get-person get-person)

(defn ui-address [{:address/keys [id street city]}]
  [:div {:key id}
   [:p (str street ", " city)]])

(defn ui-basic-person [{:person/keys [email happy?]}]
  [:span (str email ", " (if happy? "Happy!" "sad :(  "))])

(defn ui-person-tree
  [{:person/keys [id email happy? children address] :as person}]
  [:div {:key (str id)}
   [ui-basic-person person]
   [:button {:onClick #(comp/transact! SPA [(person/alter-mood {:person/id id :person/happy? (not happy?)})])} "Alter mood"]
   (when address
     [ui-address address])
   (when (seq children)
     [:ul
      (map (fn [{:person/keys [id] :as c}]
             [:li {:key (str id)}
              [ui-person-tree c]]) children)])])

(defn ui-person
  [{:person/keys [id email happy?] :as person}]
  (when id
    [:div {:key (str id)}
     [ui-basic-person person]
     [:button {:onClick #(comp/transact! SPA [(person/alter-mood {:person/id id :person/happy? (not happy?)})])} "Alter mood"]]))

(defn ui-app
  []
  (let [person @(rf/subscribe [:eql-entity [:person/id 3] [:person/id :person/email :person/happy?]])
        ;person @(rf/subscribe [:get-person {:person/id 3}])
        ;people (:all-people @(rf/subscribe [:eql [{:all-people [:person/id :person/email :person/happy?]}]]))
        people @(rf/subscribe [:q {:root :all-people :component Person}])]
    [:div
     [:button {:onClick #(df/load! SPA [:person/id 3] Person)} "Load a person"]
     [:h3 "A single person"]
     [ui-person person]
     [:h3 "All people"]
     [:div
      [:button {:onClick #(df/load! SPA :all-people Person)} "Load people"]
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

(comment
  (df/load! SPA :all-people Person)
  )
