(ns orenolisp.view.ui.component.window-indicator
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.theme :as theme])
  (:import (javafx.scene.canvas Canvas)
           [javafx.scene.effect Bloom]
           (javafx.geometry Insets)
           (javafx.scene.control ScrollPane)
           (javafx.scene.layout BorderPane VBox)))

(declare %pane)
(def %windows (atom []))

(def window-width 300)
(def window-height 120)
(def window-margin 20)
(def padding 8)

(defn- draw-background [gc x y w h]
  (doto gc
    (.clearRect x y w h)
    (.fillRect x y w h)))

(defn- calcurate-volume-level [vol]
  (let [min-value 1e-3
        m (* -1 (Math/log min-value))]
    (-> (or vol 0) (min 1) (max min-value)
        Math/log
        (+ m) (/ m)
        (* 20) int)))

(defn- draw-volume [gc vol]
  (let [x (+ (* 9 7) (* 2 padding))
        y (+ 32 24 24 24 -12)
        width (* 1/20 (- window-width (+ x padding)))]
    (doto gc
      (.setFill theme/window-backcolor)
      (draw-background (+ x (* width 0) 2) y
                       (- window-width (+ x padding)) (- 24 (* 2 2)))
      (draw-background (- window-width (+ (* 5 7) padding) ) (+ 32 24 -12)
                       (+ (* 5 7)) 24)
      (.setFill theme/window-indicator-text-color)
      (.fillText (if vol "ON" "OFF") (- window-width (+ (* 5 7) padding)) (+ 32 24)))
    (doseq [i (range 0 (calcurate-volume-level vol))]
      (doto gc
        (.fillRect (+ x (* width i) 2) y
                   (- width (* 2 2))
                   (- 18 (* 2 2)))))))

(defn- draw-window [gc {:keys [exp-id rate num-ch]}]
  (doto gc
    (.setStroke theme/primary-color)
    (.setLineWidth 1)
    (.setFill theme/window-backcolor)
    (.fillRect 0 0 window-width window-height)
    (.setFill theme/window-indicator-text-color)
    (.strokeRect 0 0 window-width window-height)
    (.setFont theme/window-indicator-large-font)
    (.fillText exp-id padding 32)
    (.setFont theme/window-indicator-font)
    (.fillText "status:" (- window-width (+ (* 12 7) padding)) (+ 32 24))
    (.fillText "channels:" padding (+ 32 24))
    (.fillText (str num-ch) (+ (* 10 7) padding) (+ 32 24))
    (.fillText "rate:" padding (+ 32 24 24))
    (.fillText (case rate :audio "AUDIO" "CONTROL")
               (+ (* 10 7) padding) (+ 32 24 24))
    (.fillText "volume:" padding (+ 32 24 24 24))))

(defn render-volumes [volumes]
  (let [windows @%windows]
    (doseq [{:keys [exp-id canvas]} windows]
      (draw-volume (.getGraphicsContext2D canvas) (get volumes exp-id)))))

(defn- render-window [info]
  (let [c (Canvas.)]
    (doto c
      (.setEffect (Bloom. 0))
      (.setWidth window-width)
      (.setHeight window-height)
      (VBox/setMargin (Insets.  10 10 10 10)))
    (draw-window (.getGraphicsContext2D c) info)
    c))

(defn add-window [window-info]
  (let [canvas (render-window window-info)]
    (fx/add-child %pane canvas)
    (swap! %windows conj
           (assoc window-info :canvas canvas))))

(defn render []
  (def %pane (VBox.))
  (doto (BorderPane.)
    (.setRight (doto (ScrollPane.)
                 (.setContent %pane)))))
