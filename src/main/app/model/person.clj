(ns app.model.person
  (:require
    [app.model.mock-database :as db]
    [datascript.core :as d]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]))

(defmutation alter-mood [{:keys [connection]} {:person/keys [id happy?]}]
  {::pc/sym `alter-mood}
  (d/transact! connection [[:db/add id :person/happy? happy?]])
  nil)

(defn all-people
  "Returns a sequence of UUIDs for all of the active accounts in the system"
  [db]
  (d/q '[:find [?v ...]
         :where
         ;;[?e :account/active? true]
         [?e :person/id ?v]]
    db))

(defresolver all-people-resolver [{:keys [db]} input]
  {::pc/output [{:all-people [:person/id]}]}
  {:all-people (mapv
                 (fn [id] {:person/id id})
                 (all-people db))})

(defn get-person [db id subquery]
  (d/pull db subquery [:person/id id]))

(defresolver person-resolver [{:keys [db] :as env} {:person/keys [id]}]
  {::pc/input  #{:person/id}
   ::pc/output [:person/email
                :person/happy?
                {:person/address [:address/id]}
                {:person/children [:person/id]}]}
  (get-person db id
    [:person/id :person/email :person/happy?
     {:person/address [:address/id]}
     {:person/children [:person/id]}]))

(def resolvers [alter-mood all-people-resolver person-resolver])

(comment
  (d/q '[:find (pull ?e [* {:person/children [:person/id]}])
         :where
         [?e :person/id]] @db/conn))
