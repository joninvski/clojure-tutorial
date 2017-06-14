(ns web-server
  (:require [yada.yada :as yada]
            [sw-planets]))

(defn get-planets-resource []
  (yada/resource
    {:methods
     {:get
      {:produces "text/plain"
       :response (sw-planets/get-planets)}}}))

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

(defn start []
  (def svr
    (yada/listener 
      (sw-planet-api)
      {:port 3000})))

(defn stop []
 ((:close svr)))

(defn restart []
 (stop)
 (start))
