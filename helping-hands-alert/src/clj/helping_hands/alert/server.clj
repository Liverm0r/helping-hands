(ns helping-hands.alert.server
  (:gen-class)
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [mount.core :as mount]
            [helping-hands.alert.config :as cfg]
            [helping-hands.alert.service :as service]))

(defonce runnable-service (server/create-server service/service))

(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (println "\nCreating your [DEV] server...")
  (cfg/init-config {:cli-args args})
  (mount/start)
  (.addShutdownHook (Runtime/getRuntime) (Thread. mount/stop))
  (-> service/service ;; start with production configuration
      (merge {:env :dev
              ::server/join? false
              ::server/routes #(route/expand-routes (deref #'service/routes))
              ::server/allowed-origins {:creds true :allowed-origins (constantly true)}
              ::server/secure-headers {:content-security-policy-settings {:object-src "none"}}})
      ;; Wire up interceptor chains
      server/default-interceptors
      server/dev-interceptors
      server/create-server
      server/start))


(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (println "\nCreating your server...")
  (cfg/init-config {:cli-args args})
  (mount/start)
  (.addShutdownHook (Runtime/getRuntime) (Thread. mount/stop))
  (server/start runnable-service))
