# Clojure tutorial (Lesson 1)

## Web server

#### 1. Respond to GET

Let's first install the yada library

(set-env!  
  :resource-paths #{"src"}
  :dependencies '[[yada "1.2.6"]])

And require it:

```clojure
(require '(yada [yada :as yada]))
```

And now let's quickly create a server.

```clojure
(def svr
  (yada/listener
    ["/" (yada/resource
           {:methods
            {:get
             {:produces "text/plain"
              :response "Hello World!"}}})]
    {:port 3000}))
```

Browse to `http://localhost:3000/` and you should be greeted.

You can also do it with curl on the command line.

```
curl -i http://localhost:3000/
```

To stop the server

```clojure
((:close svr))
```

To make it easier to start and stop the server let's create this auxiliary functions:

```clojure
(defn start-server []
  (def svr
    (yada/listener
      ["/" (yada/resource
             {:methods
              {:get
               {:produces "text/plain"
                :response "Hello World!"}}})]
      {:port 3000})))

(defn stop-server []
 ((:close svr)))

(defn restart []
 (stop-server)
 (start-server))
```


### 2. Personalized greeting
Let's add a parameter that indicates the name of the person to be greeted.

```clojure
(defn start-server []
  (def svr
    (yada/listener
      ["/" (yada/resource
             {:methods
              {:get
               {:produces "text/plain"
                :parameters {:query {:name String}} ;; We will receive name as a query param
                :response (fn [ctx]
                            (format "Hello %s. How are you?" 
                                    (-> ctx :parameters :query :name)))}}})]
      {:port 3000}))
```

The response is not a function that receives a context `ctx`, and then creates a string with the name passed as a query parameter.
Note that the `->` macro is to help the readability of the code.

```
(def ctx {:parameters 
           {:query
             {:name "value"}}})

(-> ctx :parameters :query :name)
;; is equivalent to
(:name (:query (:parameters ctx)))
```

Note: if you see `Address already in use` you need to stop the server before starting it. Use the `restart` functions

If you now run `curl -i http://localhost:3000/` you will get a:

```json
{:status 400, :errors ([:query {:error {:name missing-required-key}}])}
```

It seems we are missing the name query parameter. Let's run `curl -i http://localhost:3000/\?name\=john`

```
Hello john. How are you?% 
```

Yada is automatically validating the parameters and returning the appropriate error when parameters is missing.

### 3. Serving star wars

Let's now clean a little bit what we learn and start serving our star wars api.

We want two endpoints

 * **/api/sw/planets/** - Returns all planets
 * **/api/sw/planets/<planet-name>/population/** - Returns the population for that planet

We need to bring the `sw-planets.clj` and `build.boot` file from lesson one and create a new `src/web_server.clj` file.

```
.
├── build.boot
└── src
    ├── sw_planets.clj
    └── web_server.clj
```

Don't forget to add the yada dependency to `build.boot` and create a `web_server.clj`

```clojure
(ns web-server
  (:require [yada.yada :as yada]))

(defn sw-planet-api [db]
  ["/phonebook"
     [["" (-> (new-index-resource db)
                 (assoc :id ::index))]
                     [["/" :entry] (-> (new-entry-resource db)
                                           (assoc :id ::entry))]]])
 
(def svr
  (yada/listener
    ["/" (yada/resource
           {:methods
            {:get
             {:produces "text/plain"
              :response "Hello World!"}}})]
    {:port 3000}))
```


### 4. Manage web-server (state) with component


