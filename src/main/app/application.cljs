(ns app.application
  (:require
    [re-frame.db :refer [app-db]]
    [com.fulcrologic.fulcro.networking.http-remote :as net]
    [com.fulcrologic.fulcro.application :as app]))

(defn fulcro-reframe-app
  "Create a Fulcro app that uses Reframe's database ratom and rendering."
  [fulcro-options]
  (assoc (app/fulcro-app (merge fulcro-options {:optimized-render! identity
                                                :render-root!      identity
                                                :hydrate-root!     identity}))
    ::app/state-atom app-db))

(defonce SPA (fulcro-reframe-app {:remotes {:remote (net/fulcro-http-remote {:url "/api"})}}))
