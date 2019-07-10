(ns orenolisp.view.ui.component.new-line
  (:import (javafx.scene.paint Color)
           (javafx.scene.shape Rectangle)
           [javafx.scene.effect ColorAdjust Glow]))

(defn create-node []
  (doto (Rectangle.)
    (.setStroke Color/TRANSPARENT)
    (.setWidth 3)
    (.setHeight 38)))

(def focus-effect (doto (javafx.scene.effect.Glow.)
                    (.setLevel 1)))
(defn render [body {:keys [focus?]}]
  (doto body
    (.setFill (if focus? Color/WHITE Color/TRANSPARENT))
    (.setEffect (when focus? focus-effect))))
