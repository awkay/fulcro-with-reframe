(ns app.application
  (:require
    [re-frame.db :refer [app-db]]
    [com.fulcrologic.fulcro.networking.http-remote :as net]
    [com.fulcrologic.fulcro.application :as app]))

(defonce SPA (assoc
               (app/fulcro-app {:optimized-render! identity
                                :render-root!      identity
                                :hydrate-root!     identity
                                :remotes           {:remote (net/fulcro-http-remote {:url "/api"})}})
               ::app/state-atom app-db))
