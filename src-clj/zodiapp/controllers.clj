(ns zodiapp.controllers
  (:require [clj-http.client :as http :only (post)]
            [clojure.data.json :as json :only (read-str write-str)]
            [zodiapp.models :as m]
            [zodiapp.zodiac :as z]))

(defn get-sentiment
  "right now use alchemy until we use our own classifier"
  [h ctx]
  (when-let [sentiment-resp (try
                              (http/post
                                "http://access.alchemyapi.com/calls/text/TextGetTextSentiment"
                               {:form-params {:apikey (-> ctx :alchemy-params :api-key)
                                              :outputMode (-> ctx :alchemy-params :output-mode)
                                              :text (:description h)}})
                              (catch Exception e nil))]
    (-> sentiment-resp :body (json/read-str :key-fn keyword) :docSentiment)))

(defn get-sentiments
  [hs ctx]
  (->> hs
       (map (fn [h] (assoc h :raw-sentiment (future (get-sentiment h ctx)))))
       (map (fn [h] (assoc h :sentiment ((comp :score deref :raw-sentiment) h))))
       (map (fn [h] (dissoc h :raw-sentiment)))))

(defn all-zods
  "If we already have today's horoscope then use today's stored entry."
  [_ ctx]
  (or (some->> (m/get-todays ctx)
               ((juxt identity (partial m/get-histories ctx))))
      (as-> ctx _
          (z/get-horoscopes _)
          (get-sentiments _ ctx)
          (m/save-horoscopes _ ctx)
          ((constantly (m/get-todays ctx)) _)
          ((juxt identity (partial m/get-histories ctx)) _))))
