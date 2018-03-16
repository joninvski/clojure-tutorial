(ns web-server
  (:require [yada.yada :as yada]
            [com.stuartsierra.component :as component]
            [clojure.core.async :as async]))

(defn get-new []
  (yada/resource
    {:methods
     {:get
      {:produces "text/plain"
       :response (fn [ctx] "hello world")}}}))

(defn get-sse [mult]
  (yada/resource
    {:methods
     {:get
      {:produces "text/event-stream"
       :response (fn [ctx]
                   (async/tap mult (async/chan 2)))}}}))

(defn new-api [mult]
  ["/api/new"
   [
    ["" (get-new)]
    ["/sse" (get-sse mult)]]])
    
(def channel (async/chan))
(defn create-web-server [port]
 (yada/listener 
   (let [mult (async/mult channel)]
    (new-api mult)
    {:port port})))

(defn stop [server]
 ((:close server)))

;; just to have something producing it
(async/go
  (doseq [x (range 1 1000)]
     (Thread/sleep 2000)
     (async/>! channel (str "Hello there -> " x))))

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
