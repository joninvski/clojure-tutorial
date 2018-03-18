# Clojure tutorial (Lesson 3)

## Channels 

A channel is similar to a Unix pike. You produce something on one side, and someone in the other side can consume it. Channels in clojure are part of the library `clojure.core.async` that you can require like this:

```clojure
(require '[clojure.core.async :as async])
```

Now in your REPL let's create a channel with no buffer and insert a string in that channel.

```clojure
(def my-channel (async/chan))

;; you will get stuck here
(async/>!! my-channel "hello world!") 
```

You just got stuck because the function `>!!` inserts a value in the channel but will block if no buffer space is available. Hit `Control+C` to quit the instruction and get your REPL back.

A simple solution for us not getting blocked is to use a thread whose job is solely to add that value to the channel. The thread will get blocked but our REPL will be freed.

```clojure
(async/thread (async/>!! my-channel "hello world"))
```

We can now consume what is in the channel using the `<!!` function.

```clojure
(println (async/<!! my-channel))
```

Note that `<!!` is also blocking, so if there is nothing to consume it will be blocked until someone adds something to the channel.

What we can do, is to create an eternal loop that will consume whatever is in the channel and print it. We will put in a thread so we can free out REPL to add values to the channel.

```clojure
(async/thread
  (while true (println (async/<!! my-channel))))

(async/>!! my-channel "More hello\n")
```

Now we need to understand some of the limitations of what we just did. First of all you cannot create threads in large numbers:

```clojure
;; this will break your REPL
(doseq [x (range 1 100000)]
 (println x)
 (async/thread
  (while true (Thread/sleep 10000))))
```

On my on the 10263 thread the REPL breaks but on your computer the value will be different.

```clojure
10262
10263
IOException Cannot run program "sh": error=11, Resource temporarily unavailable
        java.lang.ProcessBuilder.start (ProcessBuilder.java:1048)
        java.lang.Runtime.exec (Runtime.java:620)
        java.lang.Runtime.exec (Runtime.java:485)
        jline.internal.TerminalLineSettings.exec (TerminalLineSettings.java:196)
        jline.internal.TerminalLineSettings.exec (TerminalLineSettings.java:186)
        jline.internal.TerminalLineSettings.stty (TerminalLineSettings.java:181)
        jline.internal.TerminalLineSettings.set (TerminalLineSettings.java:78)
        jline.internal.TerminalLineSettings.restore (TerminalLineSettings.java:70)
        jline.UnixTerminal.restore (UnixTerminal.java:67)
        reply.reader.simple-jline/shutdown (simple_jline.clj:28)
        reply.reader.simple-jline/get-input-line (simple_jline.clj:104)
        clojure.lang.Atom.swap (Atom.java:37)
Caused by:
IOException error=11, Resource temporarily unavailable
        java.lang.UNIXProcess.forkAndExec (UNIXProcess.java:-2)
        java.lang.UNIXProcess.<init> (UNIXProcess.java:247)
        java.lang.ProcessImpl.start (ProcessImpl.java:134)
        java.lang.ProcessBuilder.start (ProcessBuilder.java:1029)
        java.lang.Runtime.exec (Runtime.java:620)
Bye for now!
```

Also while we are blocked by `<!!` or `>!!` the thread is blocked. You cannot reuse that thread to do some other useful work.

### Go blocks

Let's try to replace the `thread` function with `go` and see if it still works:

```clojure
(def my-channel (async/chan))

;; using <!! is incorrect here, we will see why in a minute
(async/go 
  (while true
    (println (async/<!! my-channel))))
    
(async/>!! my-channel "More hello")
```

Yep all is still working. But we can do better, we can use what makes `go` blocks so useful. If we replace `<!!` by `<!`, we are not going to block if no value exists to be consumed, but we are going to **park**. Park means that the thread does not need to get stuck waiting for the value to be received. It can do other work and when it has something to consume, it will proceed.
This allow the thread to be relieved to work on other tasks in the mean time, never having to be stuck.

The same logic that applies to `<!!` also applies to `>!!`. In a go block you should use `>!`.
```clojure
(def my-channel (async/chan))

;; using <!! is incorrect here, we will see why in a minute
(async/go 
  (while true
    (println (async/<! my-channel))))
    
(async/go (async/>! my-channel "More hello"))
```

And if you are curious of how many go blocks you can create: 

```clojure
;; this will break your REPL
(doseq [x (range 1 100000)]
 (println x)
 (async/go
  (while true (Thread/sleep 10000))))
```

Note that go blocks internally use a thread pool with a very limited number of threads. What we are doing is sharing those threads to do more task once they get blocked.

### Mult

Channels are a very flexible abstraction. One of the interesting things you can do with them is "multiply" them.

Let's the the following example:

```clojure
(def c (async/chan))

(defn consumer [n]
   (async/go (println "Consumer" n (async/<! c)))
   (async/go (println "Consumer" n (async/<! c)))
   (async/go (println "Consumer" n (async/<! c))))
(consumer 1)
  
(async/go
  (async/>! c "Item 1")
  (async/>! c "Item 2")
  (async/>! c "Item 3"))
``` 
  
Now imagine we have two consumers and both want to receive **all** items. In our previous example once a consumer consumes a value from a pipe, no one else can read it.

It is for these use cases that the `mult` and `tap` functions exist.
  
```clojure 
(def c (async/chan))
(def c-mult (async/mult c))

(def tap1 (async/tap c-mult (async/chan)))
(def tap2 (async/tap c-mult (async/chan)))


(defn consumer [n tap]
   (async/go (println "Consumer" n (async/<! c)))
   (async/go (println "Consumer" n (async/<! c)))
   (async/go (println "Consumer" n (async/<! c))))
   
(consumer 1 tap1)
(consumer 2 tap2)
  
(async/go
  (async/>! c "Item 1")
  (async/>! c "Item 2")
  (async/>! c "Item 3"))
```

## Using channels to provide SSE (Server Sent Event)

Let's assume we start with a baseline setup in similar to lesson 2.

```clojure
(ns web-server
  (:require [yada.yada :as yada]
            [com.stuartsierra.component :as component]
            [clojure.core.async :as async]))
            
(defn get-new []
  (yada/resource
    {:methods
     {:get
      {:produces "text/plain"
       :response (fn [ctx]
                    "Hello world!")}}}))
            
(defn new-api []
  ["/api/new"
   [
    ["" (get-new)]
    ["/sse" (get-sse)]]])
    
(defn stop [server]
 ((:close server)))
    
(defn create-web-server [port]
 (yada/listener 
    (new-api)
    {:port port}))
    
(def server (create-web-server 3000))    
```           

You can test the server in your shell:

```bash
$ curl -i localhost:3000/api/new

HTTP/1.1 200 OK
X-Frame-Options: SAMEORIGIN
X-XSS-Protection: 1; mode=block
X-Content-Type-Options: nosniff
Content-Length: 12
Content-Type: text/plain
Server: Aleph/0.4.1
Connection: Keep-Alive
Date: Fri, 16 Mar 2018 20:17:20 GMT

Hello world!% 
```

We can add an endpoint `/api/new/sse` that will return SSE events through a persistent connection.

```clojure
(def channel (async/chan 2))
(def mult (async/mult channel))

(defn get-sse []
  (yada/resource
    {:methods
     {:get
      {:produces "text/event-stream"
       :response (fn [ctx]
                   (async/tap mult (async/chan 2)))}}}))

(defn new-api []
  ["/api/new"
   [
    ["" (get-new)]
    ["/sse" (get-sse)]]])
    
    
(async/thread
  (doseq [x (range 1 1000)]
     (Thread/sleep 2000)
     (async/put! channel (str "Hello there -> " x))))

(stop server)
(def server (create-web-server 3000))    
```

And again, test it on your shell:

```bash
$ curl -i localhost:3000/api/new/sse

HTTP/1.1 200 OK
X-Frame-Options: SAMEORIGIN
X-XSS-Protection: 1; mode=block
X-Content-Type-Options: nosniff
Content-Type: text/event-stream
Server: Aleph/0.4.1
Connection: Keep-Alive
Date: Fri, 16 Mar 2018 20:23:31 GMT
transfer-encoding: chunked

data: Hello there -> 1

data: Hello there -> 2

data: Hello there -> 3
```
