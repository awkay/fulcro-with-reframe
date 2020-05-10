(ns app.client
  (:require
    [reagent.dom :as rdom]
    [re-frame.core :as rf]
    [app.application :refer [SPA]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.fulcro.components :as comp]
    [app.model.session :as session]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.inspect.inspect-client :as inspect]))

(defmutation deactivate [{:account/keys [id]}]
  (action [{:keys [state]}]
    (swap! state assoc-in [:account/id id :account/active?] false))
  (remote [_] true))

(defsc Account [this {:keys [:account/id :account/email :account/active?] :as props}]
  {:query [:account/id :account/email :account/active?]
   :ident :account/id})

(defsc Child [this {:keys [:child/id :child/name] :as props}]
  {:query [:child/id :child/name]
   :ident :child/id})

(defsc Root [this {:keys [:child] :as props}]
  {:query         [{:child (comp/get-query Child)}]
   :initial-state {}})

(rf/reg-event-db :increment (fn [db event] (assoc db :number (inc (:number db)))))

(defn run-query [db [event-name {:keys [root component]}]]
  (log/spy :info [root component])
  (fdn/db->tree [{root (comp/get-query component db)}] db db))

(defn get-account [db [event-name {:keys [account/id]}]]
  (fdn/db->tree (comp/get-query Account db) (get-in db [:account/id id]) db))

(rf/reg-sub :q run-query)
(rf/reg-sub :get-account get-account)

(defn ui-page
  [{:account/keys [id email active?]}]
  [:div
   (str "Account" id ", " email ", " active?)
   [:button {:onClick #(comp/transact! SPA [(deactivate {:account/id id})])}
    "Deactivate"]])

(defn ui-app
  []
  (let [account @(rf/subscribe [:get-account {:account/id 1}])]
    [:div
     [:button {:onClick #(df/load! SPA :all-accounts Account)} "Load"]
     [ui-page account]]))

(defn ^:dev/after-load render
  "Render the toplevel component for this app."
  []
  (rdom/render [ui-app] (.getElementById js/document "app")))

(defn ^:export refresh []
  (log/info "Hot code Remount")
  (render))

(defn ^:export init []
  (log/info "Application starting.")
  ;(inspect/app-started! SPA)
  ;;(app/set-root! SPA root/Root {:initialize-state? true})
  (log/info "Starting session machine.")
  (app/set-root! SPA Root {:initialize-state? true})
  (inspect/app-started! SPA)
  (render))

(comment
  (df/load! SPA :all-accounts Account)
  (inspect/app-started! SPA)
  (app/mounted? SPA)
  (app/set-root! SPA root/Root {:initialize-state? true})
  (uism/begin! SPA session/session-machine ::session/session
    {:actor/login-form      root/Login
     :actor/current-session root/Session})

  (reset! (::app/state-atom SPA) {})

  (merge/merge-component! my-app Settings {:account/time-zone "America/Los_Angeles"
                                           :account/real-name "Joe Schmoe"})
  (dr/initialize! SPA)
  (app/current-state SPA)
  (dr/change-route SPA ["settings"])
  (app/mount! SPA root/Root "app")
  (comp/get-query root/Root {})
  (comp/get-query root/Root (app/current-state SPA))

  (-> SPA ::app/runtime-atom deref ::app/indexes)
  (comp/class->any SPA root/Root)
  (let [s (app/current-state SPA)]
    (fdn/db->tree [{[:component/id :login] [:ui/open? :ui/error :account/email
                                            {[:root/current-session '_] (comp/get-query root/Session)}
                                            [::uism/asm-id ::session/session]]}] {} s)))



