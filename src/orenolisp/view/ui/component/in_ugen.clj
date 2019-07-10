(ns orenolisp.view.ui.component.in-ugen
  (:require [orenolisp.view.ui.font-util :as f]
            [orenolisp.view.ui.fx-util :as u])
  (:import (javafx.scene.paint Color)
           (javafx.scene.canvas Canvas)
           [javafx.scene.effect Bloom]
           [javafx.scene.shape ArcType]))

(defn create-node []
  (Canvas.))

(def text-color (Color/web "#AACCFFDD"))
(def back-color (Color/web "#AACCFF99"))
(def line-color (Color/web "#AACCFF"))
(def border-color (Color/web "#AACCFF88"))
(def line-width 2)
(def DW 8)
(def focus-effect (Bloom.))

(defn render [body {:keys [size focus?]} {:keys [rate exp-id]}]
  (let [{:keys [w h]} size
        r (- (/ w 2) 4)
        cx (/ w 2)
        cy (/ h 2)
        PADDING-X 12
        PADDING-Y 8
        DIAMETER f/PORTAL-FONT-HEIGHT
        padding-x 2
        padding-y 2
        dw 8
        x1 dw
        x1-2 padding-x
        x2 (- w dw)
        x2-2 (- w padding-x)
        y1 dw
        y1-2 padding-y
        y2 (- h dw)
        y2-2 (- h padding-y)
        xss [[x1 y1-2] [x1-2 y1] [x1-2 y2] [x1 y2-2]
             [x2 y2-2] [x2-2 y2] [x2-2 y1] [x2 y1-2]
             [x1 y1-2]]]
    (doto body
      (.setWidth w)
      (.setHeight h)
      (.setEffect (when focus? focus-effect)))

    (doto (.getGraphicsContext2D body)
      (.clearRect 0 0 w h)
      (.setStroke border-color)
      (.setLineWidth line-width)
      (u/stroke-polyline xss)
      (.setStroke line-color)
      (.setFill back-color)
      (.fillArc (+ PADDING-X (* 1/4 DIAMETER)) (+ PADDING-Y (* 1/4 DIAMETER))
                (* 1/2 DIAMETER) (* 1/2 DIAMETER) 0 360 ArcType/OPEN)
      (.setFill text-color)
      (.strokeArc PADDING-X PADDING-Y
                  DIAMETER DIAMETER 10 270 ArcType/OPEN)
      (.fillArc (+ PADDING-X (* 3/4 DIAMETER)) (+ PADDING-Y (* 3/4 DIAMETER) -3)
                (* 1/3 DIAMETER) (* 1/3 DIAMETER)
                0 360 ArcType/OPEN)
      (.setFont f/PORTAL-FONT)
      (.fillText (clojure.string/upper-case exp-id)
                 (+ (* 2 PADDING-X) DIAMETER)
                 (+ cy (* 1/2 f/PORTAL-FONT-HEIGHT))))))


