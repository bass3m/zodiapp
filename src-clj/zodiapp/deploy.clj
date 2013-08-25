(ns zodiapp.deploy)

(defrecord DefaultCfg [server-params db-params])
(defrecord DefaultDbParams [db-type db-conn db-coll])
(defrecord DefaultServerParams [hostname port])

(defn default-server-params []
  (->DefaultServerParams
    (System/getenv "HOSTNAME")
    (Integer. (System/getenv "PORT"))))

(defn get-alchemy-cfg
  "get the alchemy api configs"
  []
  {:api-key (System/getenv "ALCHEMY_API_KEY")
   :output-mode "json"
   :linked-data 0})

(defn get-astro-cfg
  "get the astrology configs"
  []
  {:astro-src (System/getenv "ASTRO_SOURCE")})


(defn default-db-params []
  (map->DefaultDbParams {:db-type :mongo
                         :db-conn (System/getenv "MONGODB_URI")
                         :db-coll "Horoscopes"}))
(defn default-cfg []
  (map->DefaultCfg {:server-params (default-server-params)
                    :db-params (default-db-params)
                    :alchemy-params (get-alchemy-cfg)
                    :astrology-params (get-astro-cfg)}))
