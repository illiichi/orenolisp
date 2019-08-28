(ns orenolisp.view.ui.component.window
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.theme :as theme])
  (:import (javafx.scene.text Text)
           (javafx.scene.canvas Canvas)
           [javafx.scene.effect ColorAdjust]
           (javafx.scene.layout StackPane Pane)))

(def ^:const TOP-PADDING 20)
(def ^:const PADDING 15)

(defn create []
  (Canvas.))

(def ^:const W 5)
(defn- render [canvas w h exp-id {:keys [rate playing?]}]
  (let [col (case [playing? rate]
              [true :audio] theme/window-border-color-audio
              [true :control] theme/window-border-color-control
              theme/window-border-color-stop)]
    (doto canvas
      (.setWidth w)
      (.setHeight h))
    (doto (.getGraphicsContext2D canvas)
      (.clearRect 0 0 w h)
      (.setFill theme/window-backcolor)
      (.fillRect (* 1/2 W) (* 1/2 W) (- w W) (- h W))
      (.setStroke col)
      (.setLineWidth 1)
      (.strokeRect (* 1/2 W) (* 1/2 W) (- w W) (- h W))
      (.setFill col)
      (fx/fill-polyline [[0 0] [0 16] [(* 9 7.5) 16] [(* 10 7.5) 0]])
      (fx/fill-polyline [[w 0] [w W] [(- w W) W] [(- w W) 0]])
      (fx/fill-polyline [[0 h] [0 (- h W)] [W (- h W)] [W h]])
      (fx/fill-polyline [[w h] [w (- h W)] [(- w W) (- h W)] [(- w W) h]])
      (.setFill theme/window-title-color)
      (.fillText exp-id 5 12))))

(def MAX-WIDTH (- 1280 (* 2 PADDING)))

(defn inner-pos [{:keys [x y]}]
  {:x (+ x PADDING) :y (+ y TOP-PADDING)})
(defn outer-pos [pos]
  (-> pos
      (update :x #(- % PADDING))
      (update :y #(- % TOP-PADDING))))
(defn inner-size [{:keys [w h]}]
  {:w (- w (* 2 PADDING)) :h (- h TOP-PADDING PADDING)})
(defn outer-size [w h]
  [(+ (* 2 PADDING) w) (+ TOP-PADDING PADDING h)])

(defn draw-with-inner-size [ui exp-id {:keys [w h]}]
  (let [w (min MAX-WIDTH w)
        [required-width required-height] (outer-size w h)]
    (render ui required-width required-height exp-id {:rate :audio :playing? true})))

(defn select-window [window]
  (when window
    (doto window
      (.setEffect nil))))
(defn unselect-window [window]
  (when window
    (doto window
      (.setEffect (doto (ColorAdjust.)
                    (.setBrightness -0.6))))))
