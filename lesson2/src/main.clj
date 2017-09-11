(ns main.clj
  (:require [com.stuartsierra.component :as component]
            [web-server]))

(defn system-map [config]
  (component/system-map 
    :http (web-server/new-web-server (:port config))))

(defn start-all []
  (def system (component/start (system-map {:port 3500}))))

(defn stop-all []
  (component/stop system))
