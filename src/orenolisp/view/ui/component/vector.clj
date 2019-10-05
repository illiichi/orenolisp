(ns orenolisp.view.ui.component.vector
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.theme :as theme]
            [orenolisp.view.ui.component.animations :as anim])
  (:import (javafx.scene.paint Color)
           (javafx.scene.canvas Canvas)
           (javafx.scene.shape Rectangle)))

(defn create-node []
  (Canvas.))

(def focus-effect (anim/drop-shadow 205 205 205 0.6 0 0 10 0.8))

(defn- draw [body line-color {:keys [w h]}]
  (doto (.getGraphicsContext2D body)
    (.clearRect 0 0 w h)
    (.setLineWidth 2)
    (.setStroke line-color)
    (.setFill Color/TRANSPARENT)
    (.strokeRect 3 3 (- w 6) (- h 6))))


(defn render [body {:keys [focus? mark? size]}]
  (doto body
    (.setEffect (when focus? focus-effect))
    (.setWidth (:w size))
    (.setHeight (:h size))
    (draw (if focus? theme/focus-vector-color theme/unfocus-vector-color) size)))

