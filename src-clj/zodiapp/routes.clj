(ns zodiapp.routes
  (:require [compojure.core :refer [GET routes]]
            [compojure.route :as route :only (route)]
            [zodiapp.controllers :as c]
            [zodiapp.models :as m]
            [zodiapp.views :as v]))

(defn app [ctx]
  (routes
    (GET "/" request (-> request
                         v/render-main))
    (GET "/horoscopes" request (-> request
                                   (c/all-zods ctx)
                                   v/zods-response))
    (route/resources "/")
    (route/not-found "Sorry, there's nothing here.")))
