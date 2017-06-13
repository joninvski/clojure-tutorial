(ns sw-planets
  (:require [cheshire.core :as json]
            [clj-http.client :as http]))

(defn get-planets
  []
  (let [response (http/get "https://swapi.co/api/planets")
        body (json/parse-string (:body response) true)]
    (:results body))) 

(defn get-temperate-planets-names
  []
  (let [planets (get-planets)
        temperate-planets (filter #(= (:climate %) "temperate") planets)]
    (map #(:name %) temperate-planets)))

(defn get-temperate-planets-names
  []
  (let [planets (get-planets)
        temperate-planets (filter #(= (:climate %) "temperate") planets)]
    (map #(:name %) temperate-planets)))

(defn get-planet-population
  []
  (reduce 
    (fn [accum el]
      (let [planet-residents (:residents el)
            n-planet-residents (count planet-residents)
            planet-name (:name el)]
        (assoc accum planet-name n-planet-residents)))
    {} 
    (get-planets)))
