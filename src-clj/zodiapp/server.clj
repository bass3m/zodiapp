(ns zodiapp.server
  (:require [ring.adapter.jetty :as jetty :only (run-jetty)]
            [compojure.handler :as handler :only (api)]
            [zodiapp.routes :as routes :only (app)]
            [zodiapp.lifecycle :as life :only (Lifecycle)]))

(defn start
  "Start our web server"
  [svr]
  (try
    (if (.isStopped @svr)
      (do
        (.start @svr)
        (println "ReStarting Web Server"))
      (println "Web Server already started. Stop it first."))
    (catch IllegalArgumentException _
      (when-let [run-server (@svr)]
        (reset! svr run-server)
        (println "Starting Web Server")))))

(defn stop
  [svr]
  (when @svr
    (.stop @svr)
    (println "Stopping Web Server")))

(defrecord WebServer [svr]
  life/Lifecycle
  (start [_] (start svr))
  (stop [_] (stop svr)))

(defn web-server-init [config]
  (->WebServer (atom #(jetty/run-jetty (-> config
                                           routes/app
                                           handler/api)
                                       {:port (-> config :server-params :port)
                                        :join? false}))))
