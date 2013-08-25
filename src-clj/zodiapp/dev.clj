(ns zodiapp.dev)

(defn get-cfg
  "Get keys and tokens from config file. cfg-sel is keyword specifying
  which config params we're interested in."
  [cfg-sel]
  (-> "zodiapp-cfg.txt"
      clojure.java.io/resource
      slurp
      read-string
      cfg-sel))

(defrecord DefaultCfg [cfg-file server-params db-params])
(defrecord DefaultDbParams [db-type db-conn db-coll])
(defrecord DefaultServerParams [hostname port])

(defn default-server-params []
  (->DefaultServerParams "http://localhost:3267" 3267))

(defn default-db-params []
  (map->DefaultDbParams {:db-type :mongo
                         :db-conn "mongodb://127.0.0.1/zodiapp"
                         :db-coll "Horoscopes"}))

(defn default-cfg []
  (map->DefaultCfg {:cfg-file "zodiapp-cfg.txt"
                    :server-params (default-server-params)
                    :db-params (default-db-params)
                    :alchemy-params (get-cfg :alchemy-cfg)
                    :astrology-params (get-cfg :astrology-cfg)}))
