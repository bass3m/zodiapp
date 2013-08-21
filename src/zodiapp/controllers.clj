(ns zodiapp.controllers
  (:require [clj-http.client :as http :only (post)]
            [clojure.data.json :as json :only (read-str write-str)]
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
       (map (fn [h] (assoc h :sentiment (future (get-sentiment h ctx)))))))

(defn all-zods
  "check db first"
  [req ctx]
  (-> (:astrology-params ctx)
      z/get-horoscopes
      (get-sentiments ctx)))