(ns zodiapp.core
  (:require [goog.net.XhrIo :as xhr]
            ;[goog.events :as events]
            [goog.json :as json]))

(defn log [& more]
  (binding [*print-fn* #(.log js/console %)]
    (apply pr more)))

(defrecord Data [horoscopes])

(defn data-init []
  (->Data (atom {})))

(defn get-dimesions
  []
  (let [margin    {:top 50 :right 0 :bottom 100 :left 30}
        width     (- 960 (+ (:left margin) (:right margin)))
        height    (- 500 (+ (:top margin) (:bottom margin)))
        grid-sz   (Math.floor (/ (min height width) 4))
        legend-sz (* 2 grid-sz)
        padding   2]
    {:margin    margin
     :width     width
     :height    height
     :grid-sz   grid-sz
     :legend-sz legend-sz
     :padding   padding}))

(defn calc-color-range
  [dataset colors]
  (let [dataset-min (-> js/d3 (.min dataset (fn [d] d)))
        dataset-max (-> js/d3 (.max dataset (fn [d] d)))]
    (-> js/d3
        .-scale
        .quantize
        (.domain (array dataset-min dataset-max))
        (.range (into-array colors)))))

(defn create-svg
  [dimensions]
  (.. js/d3
      (select "#horoscope")
      (append "svg")
      (attr "width" (:width dimensions))
      (attr "height" (:height dimensions))
      (append "g")
      (attr "transform" (format "translate(%d, %d)"
                                (-> dimensions :margin :left)
                                (-> dimensions :margin :top)))))

(defn get-sign-property
  [dataset sign property]
  (-> dataset
      (aget sign)
      first
      (aget property)))

(defn get-sign-sentiment
  [dataset sign]
  (-> dataset
      (get-sign-property sign "sentiment")
      js/parseFloat))

(defn get-sign-horoscope
  [dataset sign]
  (-> dataset
      (get-sign-property sign "horoscope")))

(defn create-tooltips
  [dataset]
  (.. js/d3
      (selectAll "rect")
      (on "mouseover"
          (fn [d]
            (this-as el
                     (.. js/d3
                         (select "#tooltip")
                         (style "left" (str (.. js/d3
                                                (select el)
                                                (attr "x")) "px"))
                         (style "top" (str (.. js/d3
                                               (select el)
                                               (attr "y")) "px"))
                         (select ".sign")
                         (html (format "<p><em>%s   %s</em></p><p> %s</p>"
                                       "&#x2653;" d (get-sign-horoscope dataset d)))
                         (transition)
                         (duration 2000)
                         (ease "linear")
                         (style "opacity" 0.3)))
            (.. js/d3
                (select "#tooltip")
                (classed "hidden" false))))
      (on "mouseout" (fn [_]
                       (.. js/d3
                           (select "#tooltip")
                           (classed "hidden" true))))))

(defn create-labels
  [svg dimensions signs]
  (.. svg
      (selectAll "text")
      (data (into-array (mapcat keys signs)))
      (enter)
      (append "text")
      (text (fn [d] (.substring d 0 2)))
      (attr "fill" "black")
      (attr "dy" ".35em")
      (attr "x" (fn [_ i] (+ 5 (* (rem i 4) (+ (:grid-sz dimensions)
                                          (:padding dimensions))))))
      (attr "y" (fn [_ i] (+ 10 (* (quot i 4) (+ (:grid-sz dimensions)
                                           (:padding dimensions))))))))

(defn create-vis
  [dataset]
  (let [colors      ["#D73027" "#F46D43" "#FDAE61" "#FEE08B" "#FFFFBF"
                     "#D9EF8B" "#A6D96A" "#66BD63" "#1A9850"]
        dimensions  (get-dimesions)
        signs-ucode [{"Aries" "&#x2648;"} {"Taurus" "&#x2649;"} {"Gemini" "&#x264A;"}
                     {"Cancer" "&#x264B;"} {"Leo" "&#x264C;"} {"Virgo" "&#x264D;"}
                     {"Libra" "&#x264E;"} {"Scorpio" "&#x264F;"} {"Sagittarius" "&#x2650;"}
                     {"Capricorn" "&#x2651;"} {"Aquarius" "&#x2652;"} {"Pisces" "&#x2653;"}]
        signs       (into-array (mapcat keys signs-ucode))
        sentiments  (into-array (map (partial get-sign-sentiment dataset) signs))
        color-scale (calc-color-range sentiments colors)
        svg         (create-svg dimensions)
        vis         (.. svg
                        (selectAll "rect")
                        (data signs)
                        (enter)
                        (append "rect")
                        (attr "x" (fn [_ i] (* (rem i 4)
                                               (+ (:grid-sz dimensions)
                                                  (:padding dimensions)))))
                        (attr "y" (fn [_ i] (* (quot i 4)
                                               (+ (:grid-sz dimensions)
                                                  (:padding dimensions)))))
                        (attr "rx" 4)
                        (attr "ry" 4)
                        (attr "width" (:grid-sz dimensions))
                        (attr "height" (:grid-sz dimensions))
                        (style "fill" (fn [d]
                                        (color-scale (get-sign-sentiment dataset d)))))
        ttips (create-tooltips dataset)
        labels (create-labels svg dimensions signs-ucode)
        ]
    (.log js/console signs)
    (.log js/console labels)
    (.log js/console (into-array (mapcat vals signs-ucode)))
    ))

(defn extract-dataset
  [data]
  (let [hs @(:horoscopes data)]
    (doall
      (reduce (fn [acc h]
                (let [harray (aget hs (first h))]
                  (assoc acc (first h) (.-sentiment (aget harray 0)))))
              {} (js->clj hs)))))

(defn handle-horoscopes-resp
  "Handle data received back from server"
  [data e]
  (let [target (.-target e)]
    (if (some #{(.getStatus target)} [200 201 202])
      (let [response (-> target
                         .getResponseText
                         json/parse)]
        (reset! (:horoscopes data) (aget response "horoscopes"))
        (-> data
            :horoscopes
            deref
            create-vis)))))

(defn get-horoscopes
  "Send a GET request to our server, requesting the horoscopes"
  [data]
  (xhr/send "/horoscopes" (partial handle-horoscopes-resp data) "GET"))

(defn ^:export main []
  "Our main function, called from html file. Google closure doesn't have
  an onready type event, so we have to resort to this."
  (log "Starting app")
  (let [data (data-init)]
    (get-horoscopes data)))
