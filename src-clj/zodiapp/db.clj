(ns zodiapp.db
   (:require [monger.core :as mongo :only (connect-via-uri! disconnect! use-db!)]
             [zodiapp.lifecycle :as life :only (Lifecycle)]))

(defn start
  [db-params]
  (when db-params
    (println "Started DB" db-params)
    (mongo/connect-via-uri! (:db-conn db-params))))

(defn stop
  [db-params]
  (when db-params
    (println "Stopping DB" db-params)
    (mongo/disconnect!)))

(defrecord DbStore [db-params]
  life/Lifecycle
  (start [_] (start db-params))
  (stop [_] (stop db-params)))

(defmulti db-store-init :db-type)

(defmethod db-store-init :mongo
  [db-cfg]
  "DB init for mongodb"
  (println "db-store-init:" db-cfg)
  (->DbStore db-cfg))

(defmethod db-store-init :default
  [db-cfg]
  "No init needed, just keep the db cfg that are given"
  (->DbStore db-cfg))
