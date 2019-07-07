(ns orenolisp.view.ui.component.window
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.font-util :as f])
  (:import (javafx.scene.paint Color)
           (javafx.scene.text Text)
           (javafx.scene.canvas Canvas)
           [javafx.scene.effect ColorAdjust]
           (javafx.scene.layout StackPane Pane)))

(def ^:const TOP-PADDING 20)
(def ^:const PADDING 15)

(def AUDIO-LINE-COLOR (Color/web "#00CCFF"))
(def CONTROL-LINE-COLOR (Color/web "#EEAAFF"))
(def BACKGROUND-COLOR (Color/web "#001122AA"))

(defn create []
  (Canvas.))

(def ^:const W 5)
(defn- render [canvas w h exp-id rate]
  (let [col (if (= rate :audio) AUDIO-LINE-COLOR CONTROL-LINE-COLOR)]
    (doto canvas
      (.setWidth w)
      (.setHeight h))
    (doto (.getGraphicsContext2D canvas)
      (.clearRect 0 0 w h)
      (.setFill BACKGROUND-COLOR)
      (.fillRect (* 1/2 W) (* 1/2 W) (- w W) (- h W))
      (.setStroke col)
      (.setLineWidth 1)
      (.strokeRect (* 1/2 W) (* 1/2 W) (- w W) (- h W))
      (.setFill col)
      (fx/fill-polyline [[0 0] [0 16] [(* 9 7.5) 16] [(* 10 7.5) 0]])
      (fx/fill-polyline [[w 0] [w W] [(- w W) W] [(- w W) 0]])
      (fx/fill-polyline [[0 h] [0 (- h W)] [W (- h W)] [W h]])
      (fx/fill-polyline [[w h] [w (- h W)] [(- w W) (- h W)] [(- w W) h]])
      (.setFill Color/BLACK)
      (.fillText exp-id 5 12))))

(def MAX-WIDTH (- 1280 (* 2 PADDING)))
(defn set-inside-size [ui exp-id w h]
  (let [w (min MAX-WIDTH w)
        required-width (+ (* 2 PADDING) w)
        required-height (+ TOP-PADDING PADDING h)]
    (render ui required-width required-height exp-id :audio)))

(defn select-window [window]
  (when window
    (doto window
      (.setEffect nil))))
(defn unselect-window [window]
  (when window
    (doto window
      (.setEffect (doto (ColorAdjust.)
                    (.setBrightness -0.5))))))
