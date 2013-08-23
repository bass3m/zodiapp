(ns zodiapp.zodiac
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]))

(def ^:const zodiac-str
  "Aries|Taurus|Gemini|Cancer|Leo|Virgo|Libra|Scorpio|Sagittarius|Capricorn|Aquarius|Pisces")

(defn clean-horoscope
  "Get rid of extraneous html tags in description,
  and only keep nonredundant part of title"
  [h]
  (-> h
      (assoc :title
             (second (first (re-seq (re-pattern (str "\\b(:?" zodiac-str ")\\b.+"))
                                    (:title h)))))
      (assoc :description (second (first (re-seq #"<p>(.+?)</p>" (:description h)))))))

(defn extract-horoscopes
  "separate horoscopes into a map for each individual horoscope"
  [hs]
  (loop [h       (take 3 hs)
         rest-hs (drop 3 hs)
         acc     []]
    (if (empty? h)
      acc
      (recur (take 3 rest-hs)
             (drop 3 rest-hs)
             (conj acc (into {} h))))))

(defn get-horoscopes
  "Parse xml and keep only relavent parts of horoscope"
  [cfg]
  (->> (xml/parse (-> cfg :astrology-params :astro-src))
       zip/xml-zip
       zip/down
       zip/children
       (filter (fn [tag] (= (:tag tag) :item)))
       (mapcat :content)
       (map (juxt (comp second first) (comp first :content)))
       (filter (comp (partial not= :link) first))
       extract-horoscopes
       (map clean-horoscope)))
