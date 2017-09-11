(ns web-server
  (:require [yada.yada :as yada]
            [com.stuartsierra.component :as component]
            [sw-planets]))

(defn get-planets-resource []
  (yada/resource
    {:methods
     {:get
      {:produces "text/plain"
       :response (fn [ctx]
                   (sw-planets/get-planets))}}}))

(defn get-population-resource []
  (yada/resource
    {:methods
     {:get
      {:produces "text/plain"
       :parameters {:query {:planet-name String}} ;; We will receive name as a query param
       :response (fn [ctx]
                  (let [planet-name (-> ctx :parameters :query :planet-name)]
                    (get (sw-planets/get-planet-population) planet-name)))}}}))

(defn sw-planet-api []
  ["/api/sw/planets"
   [
    ["" (get-planets-resource)]
    ["/population" (get-population-resource)]]])

(defn create-web-server [port]
 (yada/listener 
    (sw-planet-api)
    {:port port}))

(defn stop [server]
 ((:close server)))

(defrecord WebServer [port web-server]
  component/Lifecycle

  (start [component]
    (println ";; starting webserver")
    (let [server (create-web-server port)]
      (assoc component :web-server server)))
  (stop [component]
    (println ";; stopping webserver")
    (stop (:web-server component))
    (assoc component :web-server nil)))

(defn new-web-server [port]
  (map->WebServer {:port port}))
