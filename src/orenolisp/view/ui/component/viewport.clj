(ns orenolisp.view.ui.component.viewport
  (:require [orenolisp.view.ui.fx-util :as fx])
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
(def total-sec 400)
(def time-division 10)

(defn- calcurate-scale [z]
  (let [dz 0.2]
    (Math/pow (+ 1 (* z dz)) 2.0)))

(defn- calcurate-opacity [z]
  (let [dz 0.15
        ret (Math/pow (+ 1 (* z dz)) 1.5)]
    (if (< ret (+ 1 dz)) ret 0)))

(defn- move-animation [from to h v]
  (let [num-layers (count %layers)
        initial-z-orders (->> (range num-layers) (map #(- % from)))
        dz (/ (- from to) time-division)
        key-frames (concat
                    [(fx/->KeyFrame total-sec
                                    [(.vvalueProperty %scroll-pane) v]
                                    [(.hvalueProperty %scroll-pane) h])]
                    (mapcat
                     (fn [l iz]
                       (map (fn [t]
                              (let [z (+ iz (* dz t))]
                                (fx/->KeyFrame (* (/ t time-division) total-sec)
                                               [(.scaleXProperty l) (calcurate-scale z)]
                                               [(.scaleYProperty l) (calcurate-scale z)]
                                               [(.opacityProperty l) (calcurate-opacity z)])))
                            (range (inc time-division))))
                     %layers initial-z-orders))]
    (.play (fx/create-animation key-frames))))

(defn calcurate-scroll-position-to-focus [component]
  (let [[cx cy] (fx/ui-center component)
        bounds (.getLayoutBounds %scroll-pane)
        sx (- cx (* 1/2 (.getWidth bounds)))
        sy (- cy (* 1/2 (.getHeight bounds)))]
    [(/ sx (- v-width (.getWidth bounds))) (/ sy (- v-height (.getHeight bounds)))]))

(defn put-component [layer-no component x y]
  (let [layer (nth %layers layer-no)]
    (fx/add-child layer component)
    (.relocate component x y)))

(defn focus [current-layer-no new-layer-no component]
  (let [[h v] (calcurate-scroll-position-to-focus component)]
    (move-animation current-layer-no new-layer-no h v)))

