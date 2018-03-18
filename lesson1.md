# Clojure tutorial (Lesson 1)

* [Objective](#objective)
* [Prerequisites](#prerequisites)
* [Get a repl working](#get-a-repl-working)
* [First Rest request](#first-rest-request)
* [All planets, not just Tatooine](#all-planets-not-just-tatooine)
* [Cleaning everything up](#cleaning-everything-up)

## Objective
Learn clojure basics using the star wars REST api: https://swapi.co/

We will finish the lesson with three functions.

1. One that will get all star wars planets
2. Another that will get all planet names with temperate climate
3. One that will get get the number of residents in each planet

## Prerequisites

1. Guarantee you have Java Development Kit (JDK) version >= 1.7

## Get a repl working

### 1. Install boot (an alternative to leiningen)

Follow the instructions https://github.com/boot-clj/boot#install

In Linux do:

```bash
$ sudo bash -c "cd /usr/local/bin && curl -fsSLo boot https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh && chmod 755 boot"
```

In mac:

```bash
brew install boot-clj
```

### 2. Start the repl (if you just installed boot you might have to run it twice)

```bash
boot repl
```

You should see something similar to (the port number will be different):

```
nREPL server started on port 42495 on host 127.0.0.1 - nrepl://127.0.0.1:42495
REPL-y 0.3.7, nREPL 0.2.12
Clojure 1.7.0
Java HotSpot(TM) 64-Bit Server VM 1.8.0_111-b14
        Exit: Control+D or (exit) or (quit)
    Commands: (user/help)
        Docs: (doc function-name-here)
              (find-doc "part-of-name-here")
Find by Name: (find-name "part-of-name-here")
      Source: (source function-name-here)
     Javadoc: (javadoc java-object-or-class-here)
    Examples from clojuredocs.org: [clojuredocs or cdoc]
              (user/clojuredocs name-here)
              (user/clojuredocs "ns-here" "name-here")
boot.user=>
```

### 3. Evaluate your first s-expressions.

On the repl write:

```clojure
(+ 1 2)
```

Should be evaluated to `3` (the result of the s-expression)

Another example

```clojure
(clojure.string/upper-case "hello")
```

You should see `"HELLO"`

## First Rest request

### 1. See the api first

Before using clojure, see the response by doing a curl request from your command line (not the repl)

```bash
curl https://swapi.co/api/planets/1/
```

Tatooine is a planet of the start wars universe.

You can also see it from your browser by going to "http://swapi.co/api/planets/1/"

### 2. Do the http request in clojure

Get back to your repl and let's download a http client library.

The library name is [clj-http](https://github.com/dakrone/clj-http) a clojure wrapper on the Java's [Apache HttpComponents](http://hc.apache.org/) client.

Just write:

```clojure
(set-env!  :dependencies '[[clj-http "3.6.1"]])
```

What we did was to use boot to fetch the dependency library to our local system. Now we need to require it:

```clojure
(require '(clj-http [client]))
```

We can now make the request

```clojure
(clj-http.client/get "https://swapi.co/api/planets/1/")
```

And we get as a result the http result

### 3. Store the request result and play with it

```clojure
(def response (clj-http.client/get "https://swapi.co/api/planets/1/"))
```

We now associated the var `response` to the string that returned by the HTTP request.
If we write

```clojure
response

;; {:request-time 549, :repeatable? false, :protocol-version ...}
```

We see the string. Let's see what this response is

```clojure
(class response)
```

It indicates it is a `clojure.lang.PersistentHashMap`. A map is a structure that links keys to values. So we can ask it for the keys:

```clojure
(keys response)
;; (:protocol-version ... :headers :status :body ...)  ;; abreviated
```

Let's dig into some keys:

```clojure
(:status response)
(:protocol-version response)
(:headers response)
(:body response)
(class (:body response))
```

### 4. Transform json to a clojure map

We can see the body is a string. It would be much nicer if we could get the body as a data structure (since it is json). Let's convert it.

First fetch a library to do the JSON parsing

```clojure
(set-env!  :dependencies '[[clj-http "3.6.1"] [cheshire "5.7.1"]])
```

Then require it:

```clojure
(require '(cheshire [core]))
```

And finally let's parse it (and story it in another var):

```clojure
(def body (cheshire.core/parse-string (:body response)))
(class body)
(keys body)
```

And if we try to fetch a key as before:

```clojure
(:name body)
;; nil
```

It doesn't work. And that's because if you notice carefully the keys are strings not symbols

```clojure
(keys body)
;; ("created" "url" "rotation_period" "climate" "surface_water"...)
```

One way to quickly solve it is to tell *cheshire* to symbolize keys:

```clojure
(def body (cheshire.core/parse-string (:body response) true))
(keys body)
(:name body)
```

We can also fetch string keys on a map using the `get` command but we will leave that for later.

#### Hint: Better printing

If you want to see an big map in a nicer format just use pprint

```clojure
(pprint body)
```

##  All planets, not just Tatooine

### 1. Fetch all the planets

```clojure
(def response (clj-http.client/get "https://swapi.co/api/planets"))
(def body (cheshire.core/parse-string (:body response) true))
(class body)
(keys body)
(class (:results body))
```

As you can see we now have a results that is a vector. Think of a vector as a ordered list of elements.  We can:

Count them

```clojure
(count (:results body))
;; 10
```

Get the any element:

```clojure
(first (:results body))
(last (:results body))
(nth (:results body) 3) ;; Fetches the third element on the vector
```

### 2. Get the name of the all the planets

If we wanted name for the first we could do:

```clojure
(:name (first (:results body)))
;; or equivalently
(def planet (first (:results body)))
(:name planet)
```

What about creating a function that given a planet returns its name:

```clojure
(defn get-name
  [planet]
  (:name planet))
(get-name planet)
```

Now that we know how to fetch the name given a single planet, we just need to iterate over all planets and ask the name of the planet:

```clojure
(def planets (:results body)) ;; Just to make it easier to refer to planets
(map get-name planets)

;; ("Alderaan" "Yavin IV" "Hoth" "Dagobah" "Bespin" "Endor" "Naboo" "Coruscant" "Kamino" "Geonosis")
```

Hurray, we now have the planet names. Note that we could have used an anonymous function to do this:

```clojure
(map get-name planets)
;; is similar to
(map #(:name %) planets)
;; which is a equivalent to
(map (fn [p] (:name p)) planets)
```

If we wanted to to get all planet names in UPPERCASE we could do:

```clojure
(map #(clojure.string/upper-case (get-name %)) planets)
```

### 3. I only want temperate weather planets

Imagine that from those 10 planets I just want to fetch the planets with "temperate" climate.

Just to get an idea, let's see all climates:


```clojure
(map #(:climate %) planets)
```

Now to only get the planets with temperate climate:

```clojure
(def temperate-planets (filter #(= (:climate %) "temperate") planets))
(count temperate-planets)
```

And if we want the names of those planes

```clojure
(map #(:name %) temperate-planets)
```

### 4. Count the residents in each planet

Each planet indicates the characters that belong to that planet

```clojure
(pprint (map #(:residents %) planets))
```

We can easily count them by using `count`

```clojure
(map #(count (:residents %)) planets)
;; (3 0 0 0 1 1 11 3 3 1)
```

One option is to use the `reduce` function.
The reduce function applies a function against an accumulator (accum) and each element (el) in the array.

Let's first use reduce to count the total number of residents in all planets. 0 is the initial accum value.

```clojure
(reduce
  (fn [accum el]
    (+ accum (count (:residents el))))
    0
    planets)
;; 23
```

If we want to make it more legible we can use let:

```clojure
(reduce
  (fn [accum el]
    (let [planet-residents (:residents el)
          n-planet-residents (count planet-residents)]
      (+ accum n-planet-residents)))
  0
  planets)
;; 23
```

Here we used let to give a temporary name to two intermediate calculations. The residents of a planet and counting their number.

Let's now change the function to return a list of vectors, where the first position of the name is the plane-name and the second one the number of planet residents.

```clojure
(reduce
  (fn [accum el]
    (let [planet-residents (:residents el)
          n-planet-residents (count planet-residents)
          planet-name (:name el)]
      (cons [planet-name n-planet-residents] accum)))
  [] ;; Initial value is an empty list
  planets)
;; (["Geonosis" 1] ["Kamino" 3] ["Coruscant" 3] ["Naboo" 11] ["Endor" 1] ["Bespin" 1] ["Dagobah" 0] ["Hoth" 0] ["Yavin IV"])
```

We have replaced the `+` function with the `cons` function. `cons` allows us to add a element to a sequence. In this case we start with the empty vector `[]` instead of zero, and then we `cons` (instead of add) each element to the accumulator. The accumulator is this way progressively having one more element.

But that is not good enough. What would be really useful is too have a map, where the key is the planet name, and the value is the number of residents.

```clojure
(reduce
  (fn [accum el]
    (let [planet-residents (:residents el)
          n-planet-residents (count planet-residents)
          planet-name (:name el)]
      (assoc accum planet-name n-planet-residents)))
  {} ;; We now start with an empty map
  planets)
;; {"Geonosis" 1, "Naboo" 11, "Coruscant" 3, "Endor" 1, "Bespin" 1, "Kamino" 3, "Yavin IV" 0, "Alderaan" 3, "Hoth" 0, "Dagobah" 0}
```

## Cleaning everything up

Until now we have been using the repl to do our code. When the repl stops we loose all the work we just did.

### 1. Store our code on a file

Lets store our work in a file.

Create a new `src` in the current folder and inside it create a new `sw_planets.clj` (notice the `_`)

```
.
└── src
    └── sw_planets.clj
```

If you remember we started by requiring the libraries, so let's add this to our file.

```clojure
(set-env!  :dependencies '[[clj-http "3.6.1"] [cheshire "5.7.1"]])
```

Then we require the two libraries. Cheshire for JSON parsing and clj-http for doing requests.

```clojure
(require '(cheshire [core]))
(require '(clj-http [client]))
```

Finally we wanted three functions:

1. One that will get all star wars planets
2. Another that will get all planet names with temperate climate
3. One that will get the number of residents in each planet

So lets add these functions to the file:

```clojure
(defn get-planets
  []
  (let [response (clj-http.client/get "https://swapi.co/api/planets")
        body (cheshire.core/parse-string (:body response) true)]
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
```

Save the file, close and open the repl again and try to load the file.

```clojure
(load-file "src/sw_planets.clj")
```

And your functions are now available to you.

```clojure
(get-planet-population)
(get-planets)
(get-temperate-planets-names)
```

### 2. Move build dependencies and project configuration to specific file

As you can see everything worked. But it is not normal for our project dependencies to live inside the same file as the code.

Let's create a new file `build.boot` at our root directory, and move the `set-env` function to there (don't forget to remove that line from sw_planets.clj)

```
.
├── build.boot
└── src
    └── sw_planets.clj
```

Then restart the repl, load the file again and see, everything still works.

We normally also configure the project to indicate where are the src files locates. So change the `build.boot` file to be like this:

```clojure
(set-env!
  :resource-paths #{"src"}
  :dependencies '[[clj-http "3.6.1"] [cheshire "5.7.1"]])
```

This will be usefull latter down the road.

### 3. Specifying a namespace to our functions

When we interact with the repl we are always in a specific namespace.

```clojure
boot.user => ;; we see this whenever we input expressions
```

The functions we created are also in that namespace. Try it out:

```clojure
(get-planets)
;; is equivalent to
(boot.user/get-planets)
```

But if we change namespace using `ns`

```clojure
(ns my.ns)

(get-planets) ;; now fails
(boot.user/get-planets) ;; succeeds
```

That is because when you write `(get-planet)` it is assuming `get-planets` is on the `my.ns` namespace.

Now that we know about namespace, let's make our functions be part of the `sw-planets` namespace.
Just add this to the start of the `sw_planets.clj` file.

```clojure
(ns sw-planets)
```

We can now load the file, and start to use the `sw-planets` namespace to call the functions.

```clojure
(load-file "src/sw_planets.clj")
(sw-planets/get-planets) ;; works
```

Note that the functions are also in the old namepace, we didn't remove them. Let's reset the repl so we can start clean.

```clojure
;; before repl reset
(boot.core/get-planets) ;; works
;; after repl reset
(load-file "src/sw_planets.clj")
(boot.core/get-planets) ;; no longer works
```

### 4. Aliasing the requires

We are almost finishing. The start of our `src/sw_planets.clj` file look like this.

```clojure
(ns sw-planets)

(require '(cheshire [core]))
(require '(clj-http [client]))
```

We can do the require inside the `ns` macro.

```clojure
(ns sw-planets
  (:require [cheshire.core]
            [clj-http.client]))
```

Finally so no not have to do `clj-http.client/get` whenever we want to do a get request, we can do an alias of `clj-http` to `http`. Same thing for `cheshire.core`, we can alias it to `json`. Don't forget to then change the `get-planets` function to use these alias.

Final `src/sw_planets.clj` file:

```clojure
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
```

Final `build.boot` file:

```clojure
(set-env!
  :resource-paths #{"src"}
  :dependencies '[[clj-http "3.6.1"] [cheshire "5.7.1"]])
```

The final result can be seen in [github](https://github.com/joninvski/clojure-tutorial/tree/master/lesson1).
