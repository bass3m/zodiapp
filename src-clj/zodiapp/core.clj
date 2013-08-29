(ns zodiapp.core
  (:require [zodiapp.server :as server]
            [compojure.handler :as handler :only (api site)]
            [zodiapp.routes :as routes :only (app)]
            [ring.adapter.jetty :as jetty :only (run-jetty)]
            [zodiapp.lifecycle :as life :only (Lifecycle)]
            [zodiapp.db :as db]
            [zodiapp.cfg :as cfg]))

(defrecord Application [config web-server db-store]
  life/Lifecycle
  (start [_]
    (life/start web-server)
    (life/start db-store))
  (stop [_]
    (life/stop db-store)
    (life/stop web-server)))

(defn app
  "Return an instance of the application"
  [config]
  (let [config (cfg/merge-cfg config)
        db (db/db-store-init (:db-params config))
        web-server (server/web-server-init config)]
    (->Application config web-server db)))

(defn -main
  ([] (-main (cfg/default-cfg)))
  ([config]
   (when-not (empty? config)
     (let [system (app config)]
       (life/start system)
       (.. (Runtime/getRuntime) (addShutdownHook (proxy [Thread] []
                                                   (run [] (life/stop system)))))))))
