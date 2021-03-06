(ns helping-hands.alert.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.interceptor.chain :as chain]
            [io.pedestal.http.body-params :as body-params]
            [helping-hands.alert.core :as core]))

(defn- get-uid
  "TODO: Integrate with Auth service"
  [token]
  (when (and (string? token)
             (not (empty? token)))
    ;; validate token
    {"uid" "hhuser"}))

(def auth
  {:name ::auth
   :enter
   (fn [context]
     (let [token (-> context :request :headers (get "token"))]
       (if-let [uid (and (not (nil? token))
                         (get-uid token))]
         (assoc-in context [:request :tx-data :user] uid)
         (chain/terminate
          (assoc context
                 :response {:status 401
                            :body "Auth token not found"})))))
   :error
   (fn [context ex-info]
     (assoc context
            :response {:status 500
                       :body (.getMessage ex-info)}))})

(def gen-events
  {:name ::events
   :enter
   (fn [context]
     (if (:response context)
       context
       (assoc context :response {:status 200 :body "Success"})))
   :error
   (fn [context ex-info]
     (assoc context
            :response {:status 500
                       :body (.getMessage ex-info)}))})

(def common-interceptors [(body-params/body-params) http/html-body])

(def routes #{["/alerts/email"
               :post (conj common-interceptors `auth
                           `core/validate `core/send-email `gen-events)
               :route-name :alert-email]
              ["/alerts/sms"
               :post (conj common-interceptors `auth
                           `core/validate `core/send-sms `gen-events)
               :route-name :alert-sms]})

(def service {:env :prod
              ::http/routes routes
              ::http/resource-path "/public"
              ::http/type :jetty
              ::http/port 8080
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})

