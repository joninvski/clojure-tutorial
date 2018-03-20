# Clojure tutorial (Lesson 2)

* [Objective](#objective)
* [Prerequisites](#prerequisites)
* [Web server](#web-server)
  * [Respond to GET](#1-respond-to-get)
  * [Personalized greeting](#2-personalized-greeting)
  * [Serving star wars](#3-serving-star-wars)
  * [Manage web-server (state) with component](#4-manage-web-server-state-with-component)

## Objective
Learn how to set up a web server in clojure ([Yada](https://github.com/juxt/yada)) and how to use [Stuart Sierra's Component](https://github.com/juxt/yada) framework.

## Prerequisites

1. If you never touched clojure doing [lesson1](https://github.com/joninvski/clojure-tutorial/blob/master/lesson1.md) first should prove useful.

## Web server

#### 1. Respond to GET

Let's first install the [yada](https://github.com/juxt/yada) library:

```clojure
(set-env!
  :resource-paths #{"src"}
  :dependencies '[[yada "1.2.6"]])
```

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

```clojure
(def ctx {:parameters
           {:query
             {:name "value"}}})

(-> ctx :parameters :query :name)
;; is equivalent to
(:name (:query (:parameters ctx)))
```

Note: if you see `Address already in use` you need to stop the server before starting it. Use the `restart` functions

If you now run `curl -i http://localhost:3000/` you will get a:

```clojure
{:status 400, :errors ([:query {:error {:name missing-required-key}}])}
```

It seems we are missing the name query parameter. Let's run `curl -i http://localhost:3000/\?name\=john`

```
Hello john. How are you?%
```

Yada is automatically validating the parameters and returning the appropriate error when parameter is missing.

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

First don't forget to add the yada dependency to `build.boot`.

```clojure
(set-env!
  :resource-paths #{"src"}
  :dependencies '[[clj-http "3.6.1"]
                  [cheshire "5.7.1"]
                  [yada "1.2.6"]])
```

And on the web_server.clj we add the two endpoints, and create functions for the yada resources (*get-planets-resource* and *get-population-resource*)

```clojure
(ns web-server
  (:require [yada.yada :as yada]
            [sw-planets]))

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
```

Now to define the resources:

```clojure
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
```

### 4. Manage web-server (state) with component

To be easy to create and stop our webserver, lets create some helper function on web_server.clj

```clojure
(defn create-web-server[]
 (yada/listener 
    (sw-planet-api)
    {:port 3305}))

(defn stop [server]
 ((:close server)))
```

Note that the `create-web-server` function returns the parameter needed for the stop function.

Now let's try to "plug" this web-server into [Stuart Sierra's components library](https://github.com/stuartsierra/component).
Components has the objecting of managing the lifecycle and dependencies of software components which have runtime state.

Let's first create the component lifecycle rules for the webserver component (still in `web_server.clj` file):

```clojure
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
```

Then we can create a file that will be responsible to manage all the "components" that make up the system. Let's create `main.clj`.

```clojure
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
```

Let's break down the code. First we need to indicate what are the components that compose our systems. This is what `system-map` does. We say that we have a single component (:http) and we tell it how to call the corresponding component/Lifecycle.

Then the `component/start` and `component/stop` do all the magic. They go through the system-map, and for each component sees it's dependencies (none for now) and initializes components in the correct order (currently it is easy as we have only one).

The `component/stop` method does the inverse functionality (again respecting dependencies).

For now this is quite simple and there is no real benefit. We only have one component. But as we add more the component/system abstraction will start to pay off.

The final result can be seen in [github](https://github.com/joninvski/clojure-tutorial/tree/master/lesson2).
