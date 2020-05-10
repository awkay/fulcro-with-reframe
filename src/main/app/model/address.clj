(ns app.model.address
  (:require
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [datascript.core :as d]))

(defresolver address-resolver [{:keys [db] :as env} {:address/keys [id]}]
  {::pc/input  #{:address/id}
   ::pc/output [:address/id :address/street :address/city]}
  (log/spy :info (d/pull db [:address/id :address/street :address/city] [:address/id id])))

(def resolvers [address-resolver])
