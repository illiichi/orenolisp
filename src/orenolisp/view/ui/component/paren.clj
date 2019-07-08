(ns orenolisp.view.ui.component.paren
  (:require [orenolisp.view.ui.component.animations :as anim]
            [orenolisp.view.ui.fx-util :as fx])
  (:import (javafx.scene.paint Color)
           (javafx.scene.canvas Canvas)))

(defn create-node []
  (Canvas.))

(def dw 8)
(def padding-x 2)
(def padding-y 2)
(def line-width 1)
(def line-color (Color/web "#AACCFF"))

(defn render [body {:keys [focus? mark? size]}]
  (let [{:keys [w h]} size
        x1 dw
        x1-2 padding-x
        x2 (- w dw)
        x2-2 (- w padding-x)
        y1 dw
        y1-2 padding-y
        y2 (- h dw)
        y2-2 (- h padding-y)
        gc (.getGraphicsContext2D body)]
    (doto body
      (.setWidth w)
      (.setHeight h))
    (doto gc
      (.clearRect 0 0 w h)
      (.setStroke (if focus? Color/WHITE line-color))
      (.setLineWidth (if focus? (* 3 line-width) line-width))
      (fx/stroke-polyline [[x1 y1-2] [x1-2 y1] [x1-2 y2] [x1 y2-2]])
      (fx/stroke-polyline [[x2 y1-2] [x2-2 y1] [x2-2 y2] [x2 y2-2]]))
    (when mark? (doto gc
                  (.setFill (Color/web "#8888FF22"))
                  (fx/fill-polyline [[x1 y1-2] [x1-2 y1] [x1-2 y2] [x1 y2-2]
                                     [x2 y2-2] [x2-2 y2] [x2-2 y1] [x2 y1-2]])))

    (when focus?
      (.setEffect body (anim/drop-shadow 255 255 255 0.3 0 0 15 0.8))
      (doto gc
        (.setLineWidth 1)
        (.setStroke (Color/web "#AAAAAAAA"))
        (.strokeLine x1 y1-2 x2 y1-2)
        (.strokeLine x1 y2-2 x2 y2-2)))))

