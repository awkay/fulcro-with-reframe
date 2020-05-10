(ns app.model.person
  (:require
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]))

(defmutation alter-mood [{:person/keys [id happy?]}]
  (action [{:keys [state]}]
    (swap! state assoc-in [:person/id id :person/happy?] happy?))
  (remote [_] true))
