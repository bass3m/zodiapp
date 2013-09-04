(ns zodiapp.models
  (:require [monger.collection :as mongo :only (find-one-as-map insert
                                                update-by-id remove-by-id)]
            [monger.joda-time]
            [monger.query :as mq :only (find sort limit
                                        skip fields with-collection)]
            [clj-time.core :as clj-time :only (today year month day date-time)]
            [clj-time.format :as clj-fmt :only (formatter unparse)])
  (:import  [org.bson.types ObjectId]))

(defn day-old []
  (->> (clj-time.core/today)
       ((juxt clj-time/year clj-time/month clj-time/day))
       (apply clj-time/date-time)))

(defn get-todays
  [cfg]
  (-> cfg
      :db-params
      :db-coll
      (mongo/find-one-as-map {:created-at {"$gt" (day-old)}})
      (dissoc :_id :created-at)))

(defn get-latest-n-horoscopes
  "Get last n horoscopes from db, sign passed as clj key"
  [cfg n sign]
  (let [coll (-> cfg :db-params :db-coll)]
    (mq/with-collection coll
      (mq/find {})
      (mq/fields [(str (name sign) ".sentiment") :created-at])
      (mq/sort (array-map :created-at -1))
      (mq/limit n))))

(defn get-older-than-n
  "Skip the latest n docs and return anything older"
  [cfg n]
  (let [coll (-> cfg :db-params :db-coll)]
    (mq/with-collection coll
      (mq/find {})
      (mq/fields [:created-at])
      (mq/sort (array-map :created-at -1))
      (mq/skip n))))

(defn first-letter-day
  "from Mon 08/23 -> M 08/23"
  [date-str]
  (as-> date-str _
    (clojure.string/split _  #"\s")
    ((juxt (comp #(subs % 0 1) first) second) _)
    (clojure.string/join " " _)))

;; {:aries [{:date xx :sent yy} ..] :taurus [{:date xx :sent yy} ..] ..}
(defn get-sign-history
  "Get historical data for sign, return map containing vector
  of maps of dates and scores"
  [cfg sign]
  (let [date-fmt (clj-time.format/formatter "E MM/dd")]
    (->> sign
      (get-latest-n-horoscopes cfg 7)
      (map (juxt sign
                 (comp (partial clj-time.format/unparse date-fmt)
                       :created-at)))
      (map (juxt first (comp first-letter-day second))))))

;; use from a call to get-today etc..
(defn get-histories
  "uses a day's horoscope to get all the signs and extract the history
  from db"
  [cfg h]
  (as-> h _
    (map first _)
    (zipmap (identity _) (map (partial get-sign-history cfg) _))))

(defn save-todays
  [hs cfg]
  (let [oid (ObjectId.)
        doc {:created-at (clj-time/now)}
        coll (-> cfg :db-params :db-coll)]
    (mongo/insert coll (assoc doc :_id oid))
    (doseq [h hs]
      (mongo/update-by-id coll
                          oid
                          {"$push" {(:title h)
                                    {:horoscope (:description h)
                                     :sentiment (:sentiment h)}}}))))

(defn age-old-entries
  [cfg old]
  (doall
    (map (comp
           (partial mongo/remove-by-id (-> cfg :db-params :db-coll)) :_id)
         old)))

(defn save-horoscopes
  "save to db, but first we age any entries than are a week old"
  [hs cfg]
  (when-let [old (seq (get-older-than-n cfg 6))]
    (age-old-entries cfg old))
  (save-todays hs cfg))
