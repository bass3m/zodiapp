(ns zodiapp.views
  (:require [clojure.data.json :as json :only (write-str)]
            [clj-time.core :as clj-time :only (today)]
            [clj-time.format :as clj-fmt :only (formatter unparse-local)]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-css include-js]]))

(defn render-main
  "Display main page"
  [_]
  (html
    [:head
      [:title "Your outlook for today: "]
      [:meta  {:charset "UTF-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      (include-css "/css/style.css")]
    [:body
      [:div.container
        [:header {:style "margin: 0 0 0 2.5%;font-weight: 100;
                          letter-spacing: -2px; font-size: 30px;"}
         [:span "Outlook for today: "
          [:span.date (clj-fmt/unparse-local
                        (clj-fmt/formatter "E MM/dd")
                        (clj-time/today))]]]
       [:div#horoscope
       [:div#fortune.hidden
        [:p [:strong "Daily Horoscope for"]]
        [:p [:span.sign ""]]]]]]
    (include-js "/js/main.js")
    (include-js "http://d3js.org/d3.v3.min.js")
    ;; need to find a better way than use harcoded name
    [:script {:type "text/javascript" :language "javascript"} "zodiapp.core.main()"]))

(defn zods-response
  "Return horoscopes in json"
  [zods]
  {:headers  {"Content-Type" "application/json;charset=utf-8"}
   :body (json/write-str {:horoscopes zods})})
