(ns zodiapp.core
  (:require [goog.net.XhrIo :as xhr]
            [goog.json :as json]))

(defn log [& more]
  (binding [*print-fn* #(.log js/console %)]
    (apply pr more)))

(defrecord Data [horoscopes])

(defn data-init []
  (->Data (atom {})))

(defn get-dimesions
  []
  (let [margin    {:top 70 :right 0 :bottom 100 :left 30}
        width     (- 740 (+ (:left margin) (:right margin)))
        height    (- 790 (+ (:top margin) (:bottom margin)))
        grid-max  122
        grid-sz   (min grid-max (Math.floor (/ (min height width) 4)))
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

(defn create-rand-sentiment-data
  [n]
  (->> (repeatedly #(rand 0.6))
       (map (fn [n] (if (< n 0.3) (* -1 n) (- n 0.3))))
       (take n)))

(defn create-history-bar-chart
  [svg dataset]
  (let [data (create-rand-sentiment-data 10)
        width 120
        height 100
        data-range 0.3
        x-scale (-> js/d3
                    .-scale
                    .ordinal
                    (.domain (into-array (range (count data))))
                    (.rangeRoundBands (array 0 width) 0.05))
        y-scale (-> js/d3
                    .-scale
                    .linear
                    (.clamp true)
                    (.domain (array (- data-range) data-range))
                    (.range (array 0 height)))
        hist (.. svg
                 (selectAll ".hist")
                 (data (into-array data))
                 (enter)
                 (append "g")
                 (attr "width" width)
                 (attr "height" height)
                 (attr "class" "hist")
                 (attr "transform" (format "translate(%d, %d)" 380 400)))]
    (.log js/console (into-array data))
    (.. hist
        (append "rect")
        (attr "x" (fn [_ i] (x-scale i)))
        (attr "y" (fn [d] (- height (y-scale d))))
        (attr "width" (.rangeBand x-scale))
        (attr "height" (fn [d] (y-scale d)))
        (style "fill" "#ddd"))))

(defn create-tooltips
  [d dataset]
  (.. js/d3
      (select "#tooltip")
      (style "left" "30px")
      (style "top" "520px")
      (select ".sign")
      (html (format "<p><strong><em>%s : </em></strong></p><p>%s</p>"
                    d (get-sign-horoscope dataset d)))
      (transition)
      (duration 2000)
      (ease "linear")
      (style "opacity" 0.3))
  (.. js/d3
      (select "#tooltip")
      (classed "hidden" false)))

(defn create-event-handlers
  [svg dataset]
  (.. js/d3
      (selectAll "rect")
      (on "mouseover"
          (fn [d]
            (create-tooltips d dataset)
            (create-history-bar-chart svg dataset)))
      (on "mouseout" (fn [_]
                       (.. js/d3
                           (select "#tooltip")
                           (classed "hidden" true))
                       (.. js/d3
                           (selectAll ".hist")
                           (remove))))))

(defn create-legend
  [svg dimensions colors]
  (let [legend (.. svg
                   (selectAll ".legend")
                   (data (into-array colors))
                   (enter)
                   (append "g")
                   (attr "class" "legend")
                   (attr "transform" (format "translate(%d, %d)" 0 -50)))]
    (.. legend
        (append "rect")
        (attr "x" (fn [_ i] (* (/ (:grid-sz dimensions) 4) i)))
        (attr "width" (/ (:grid-sz dimensions) 4))
        (attr "height" (/ (:grid-sz dimensions) 16))
        (style "fill" (fn [_ i] (colors i))))
    (.. legend
        (append "text")
        (attr "class" "legend-key")
        (text (fn [d i]
                (cond
                  (= i 0) "☹"
                  (= i 1) "←"
                  (= i 2) "less"
                  (= i 3) "lucky"
                  (= i 5) "more"
                  (= i 6) "lucky"
                  (= i 7) "→"
                  (= i 8) "☺")))
        (attr "x" (fn [_ i]
                    (cond
                      (or (= i 0) (= i 1) (= i 2) (= i 3) (= i 7) (= i 8))
                          (+ (/ (:grid-sz dimensions) 14)
                             (* (/ (:grid-sz dimensions) 4) i))
                      :default (* (/ (:grid-sz dimensions) 4) i))))
        (attr "y" (+ (/ (:grid-sz dimensions) 7)
                     (* (:padding dimensions) 3)))
        (attr "dy" ".35em")
        (attr "font-size"
              (fn [_ i]
                (cond
                  (or (= i 0) (= i 1) (= i 7) (= i 8)) "18px"
                  :default "10px")))
        (style "fill" (fn [_ i]
                        (cond
                          (= i 0) "red"
                          (= i 8) "green"
                          :default "#aaa"))))))

(defn create-labels
  [svg dimensions signs]
  (let [dates [{"Aries" "3/21-3/20"} {"Taurus" "4/21-5/21"} {"Gemini" "5/22-6/21"}
               {"Cancer" "6/22-7/22"} {"Leo" "7/23-8/22"} {"Virgo" "8/23-9/22"}
               {"Libra" "9/23-10/22"} {"Scorpio" "10/23-11/22"} {"Sagittarius" "11/23-12/21"}
               {"Capricorn" "12/22-1/20"} {"Aquarius" "1/21-2/19"} {"Pisces" "2/20-3/20"}]
        sign (.. svg
                 (selectAll ".sign")
                 (data (into-array (mapcat keys signs)))
                 (enter)
                 (append "g")
                 (attr "class" "sign")
                 (attr "transform" (fn [_ i] (format "translate(%d, %d)"
                                               (* (rem i 4)
                                                  (+ (:grid-sz dimensions)
                                                     (:padding dimensions)))
                                               (* (quot i 4)
                                                  (+ (:grid-sz dimensions)
                                                     (:padding dimensions)))))))]
    (.. sign
      (append "text")
      (attr "class" "symbol")
      (text  (fn [d] (->> signs (filter d) first vals first)))
      (attr "fill" "black")
      (attr "font-size" "24px")
      (attr "x" (- (:grid-sz dimensions) 28))
      (attr "y" (- (:grid-sz dimensions) 8)))
    (.. sign
      (append "text")
      (attr "class" "name")
      (text (fn [d] (.substring d 0 2)))
      (attr "fill" "black")
      (attr "x" 8)
      (attr "y" 18))
    (.. sign
      (append "text")
      (attr "class" "date")
      (text  (fn [d] (->> dates (filter d) first vals first)))
      (attr "fill" "black")
      (attr "font-size" "11px")
      (attr "x"  6)
      (attr "y" (- (:grid-sz dimensions) 12)))))

(defn create-vis
  [dataset]
  (let [dimensions  (get-dimesions)
        signs-ucode [{"Aries" "♈"} {"Taurus" "♉"} {"Gemini" "♊"}
                     {"Cancer" "♋"} {"Leo" "♌"} {"Virgo" "♍"}
                     {"Libra" "♎"} {"Scorpio" "♏"} {"Sagittarius" "♐"}
                     {"Capricorn" "♑"} {"Aquarius" "♒"} {"Pisces" "♓"}]
        signs       (into-array (mapcat keys signs-ucode))
        sentiments  (into-array (map (partial get-sign-sentiment dataset) signs))
        colors      ["#D73027" "#F46D43" "#FDAE61" "#FEE08B" "#FFFFBF"
                     "#D9EF8B" "#A6D96A" "#66BD63" "#1A9850"]
        color-scale (calc-color-range sentiments colors)
        svg         (create-svg dimensions)]
    (.. svg
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
    (create-labels svg dimensions signs-ucode)
    (create-legend svg dimensions colors)
    (create-event-handlers svg dataset)
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
