(ns orenolisp.view.ui.component.viewport
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.component.window-indicator :as window-indicator]
            [orenolisp.view.ui.component.animations :as anim])
  (:import (javafx.scene.layout Pane StackPane)
           (javafx.scene.control ScrollPane)))

(def v-width 2560)
(def v-height 2048)

(declare %scroll-pane)
(declare %layers)

(defn render []
  (def %scroll-pane (ScrollPane.))
  (def %layers (repeatedly 5 #(Pane.)))
  (let [container (StackPane.)]
    (doseq [l %layers]
      (fx/add-child container l))
    (doto %scroll-pane
      (.setContent (doto container
                     (.setPrefSize v-width v-height))))))

(def distance-to-screen 1)
(def distance-between-screen 1)
(def total-sec 200)
(def time-division 5)

(defn- calcurate-scale [z]
  (let [dz 0.2]
    (Math/pow (+ 1 (* z dz)) 2.0)))

(defn- calcurate-opacity [z]
  (let [dz 0.15
        ret (Math/pow (+ 1 (* z dz)) 1.5)]
    (if (< ret (+ 1 dz)) ret 0)))

(defn- scroll-keyframes [h v]
  [(fx/->KeyFrame total-sec
                  [(.vvalueProperty %scroll-pane) v]
                  [(.hvalueProperty %scroll-pane) h])])

(defn- z-order-keyframes [from to]
  (let [num-layers (count %layers)
        initial-z-orders (->> (range num-layers) (map #(- % from)))
        dz (/ (- from to) time-division)]
    (mapcat
     (fn [l iz]
       (map (fn [t]
              (let [z (+ iz (* dz t))]
                (fx/->KeyFrame (* (/ t time-division) total-sec)
                               [(.scaleXProperty l) (calcurate-scale z)]
                               [(.scaleYProperty l) (calcurate-scale z)]
                               [(.opacityProperty l) (calcurate-opacity z)])))
            (range (inc time-division))))
     %layers initial-z-orders)))

(defn- move-animation
  ([h v] (.play (fx/create-animation (scroll-keyframes h v))))
  ([from to h v]
   (if (= from to)
     (move-animation h v)
     (let [key-frames (concat (scroll-keyframes h v)
                              (z-order-keyframes from to))]
       (.play (fx/create-animation key-frames))))))

(defn calcurate-scroll-position-to-focus [component]
  (let [[cx cy] (fx/ui-center component)
        screen-width (.getWidth %scroll-pane)
        screen-height (.getHeight %scroll-pane)
        sx (- cx (* 3/8 screen-width))
        sy (- cy (* 1/2 screen-height))]
    [(/ sx (- v-width screen-width))
     (/ sy (- v-height screen-height))]))

(defn- with-layer [layer-no f]
  (let [layer (nth %layers layer-no)]
    (f layer)))

(defn put-component [layer-no component]
  (with-layer layer-no #(fx/add-child % component)))

(defn put-components [layer-no components]
  (with-layer layer-no
    #(doseq [component components] (fx/add-child % component))))

(defn focus [current-layer-no new-layer-no component]
  (let [[h v] (calcurate-scroll-position-to-focus component)]
    (move-animation current-layer-no new-layer-no h v)))

(defn calcurate-visible-area []
  (let [screen-width (.getWidth %scroll-pane)
        screen-height (.getHeight %scroll-pane)
        vvalue (.getVvalue %scroll-pane)
        hvalue (.getHvalue %scroll-pane)
        minX (* hvalue (- v-width screen-width))
        minY (* vvalue (- v-height screen-height))]
    [minX minY (+ minX screen-width) (+ minY screen-height)]))

(defn frame-out? [component]
  ;; ScrollPane.getViewportBounds seems to work, but not.
  (let [[x1 y1 x2 y2] (calcurate-visible-area)
        child-bounds (.getBoundsInParent component)]
    (or (< (.getMinX child-bounds) x1)
        (> (.getMaxX child-bounds)
           (- x2 window-indicator/window-width))
        (< (.getMinY child-bounds) y1)
        (> (.getMaxY child-bounds) y2))))

(defn focus-if-frame-out [container-component focus-component]
  (when (frame-out? container-component)
    (let [[h v] (calcurate-scroll-position-to-focus focus-component)]
      (move-animation h v))))

(defn location-by-ratio
  "w h - ratio to the screen, x y - center position"
  [w h cx cy]
  (let [screen-width (.getWidth %scroll-pane)
        screen-height (.getHeight %scroll-pane)
        width (* w screen-width)
        height (* h screen-height)
        sx (- (* cx v-width) (* 1/2 width))
        sy (- (* cy v-height) (* 1/2 height))]
    [sx sy width height]))

(defn move-center []
  (doto %scroll-pane
    (.setVvalue 0.5)
    (.setHvalue 0.5)))

