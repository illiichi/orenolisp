(ns orenolisp.view.ui.component.vector
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.component.animations :as anim])
  (:import (javafx.scene.paint Color)
           (javafx.scene.shape Rectangle)))

(defn create-node []
  (doto (Rectangle.)
    (.setStrokeWidth 2)
    (.setFill Color/TRANSPARENT)))

(def focus-effect (anim/drop-shadow 205 205 205 0.6 0 0 10 0.8))

(defn render [body {:keys [focus? mark? size]}]
  (doto body
    (.setEffect (when focus? focus-effect))
    (.setStroke (if focus? (Color/web "#FFFFFF") (Color/web "#8888AA88")))
    (.setWidth (:w size))
    (.setHeight (:h size))))

