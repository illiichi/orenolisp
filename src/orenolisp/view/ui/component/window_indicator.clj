(ns orenolisp.view.ui.component.window-indicator
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.font-util :as f])
  (:import (javafx.scene.canvas Canvas)
           [javafx.scene.effect Bloom]
           (javafx.geometry Insets)
           (javafx.scene.paint Color)
           (javafx.scene.layout BorderPane VBox)))

(declare %pane)
(def %windows (atom []))

(def window-width 300)
(def window-height 120)
(def window-margin 20)
(def padding 8)
(def line-color (Color/web "#AACCFF"))
(def BACKGROUND-COLOR (Color/web "#001122AA"))

(defn- draw-background [gc x y w h]
  (doto gc
    (.clearRect x y w h)
    (.fillRect x y w h)))

(defn- draw-volume [gc vol]
  (let [x (+ (* 9 7) (* 2 padding))
        y (+ 32 24 24 24 -12)
        width (* 1/20 (- window-width (+ x padding)))]
    (doto gc
      (.setFill BACKGROUND-COLOR)
      (draw-background (+ x (* width 0) 2) y
                       (- window-width (+ x padding)) (- 24 (* 2 2)))
      (draw-background (- window-width (+ (* 5 7) padding) ) (+ 32 24 -12)
                       (+ (* 5 7)) 24)
      (.setFill (Color/web "FFFFFF"))
      (.fillText (if vol "ON" "OFF") (- window-width (+ (* 5 7) padding)) (+ 32 24)))
    (doseq [i (range 0 (int (* 20 (min 1 (or vol 0)))))]
      (doto gc
        (.fillRect (+ x (* width i) 2) y
                   (- width (* 2 2))
                   (- 18 (* 2 2)))))))

(defn- draw-window [gc {:keys [exp-id rate num-ch]}]
  (doto gc
    (.setStroke line-color)
    (.setLineWidth 1)
    (.setFill BACKGROUND-COLOR)
    (.fillRect 0 0 window-width window-height)
    (.setFill (Color/web "FFFFFF"))
    (.strokeRect 0 0 window-width window-height)
    (.setFont f/WINDOW-INDICATOR-LARGE-FONT)
    (.fillText exp-id padding 32)
    (.setFont f/WINDOW-INDICATOR-FONT)
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
    (.setRight %pane)))
