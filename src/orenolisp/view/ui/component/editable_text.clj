(ns orenolisp.view.ui.component.editable-text
  (:require [orenolisp.view.ui.font-util :as f]
            [orenolisp.view.ui.component.animations :as anim])
  (:import (javafx.scene.paint Color)
           (javafx.scene.canvas Canvas)
           [javafx.scene.effect ColorAdjust Glow]))

(def ^:const LINE-WIDTH 2)

(defn create-node []
  (doto (Canvas.)
    (.setHeight (+ 8 f/LABEL-FONT-HEIGHT))))

(defn- draw-caret [gc position total-width]
  (when position
    (let [caret-x (min total-width
                       (* f/LABEL-FONT-WIDTH position))]
      (doto gc
        (.setLineWidth LINE-WIDTH)
        (.strokeLine caret-x 6 caret-x (+ f/LABEL-FONT-HEIGHT 2))))))
(def COLOR-IDLE (Color/web "#CCCCFF"))
(def COLOR-TYPING (Color/web "#FFFFFF"))

(def focus-effect
  (anim/blend [(doto (ColorAdjust.)
                 (.setBrightness 1))
               (doto (Glow.)
                 (.setLevel 1))]))

(defn render [^Canvas pane {:keys [focus? mark?]} {:keys [value position]}]
  (doto pane
    (.setEffect (when focus? focus-effect)))
  (if (empty? value)
    (do
      (.setWidth pane 6)
      (doto (.getGraphicsContext2D pane)
        (.clearRect 0 0 6 f/LABEL-FONT-HEIGHT)
        (.setFill (if position Color/WHITE Color/RED))
        (.setStroke (if position Color/WHITE Color/RED))
        (draw-caret (or position 0) 6)))

    (let [gc (.getGraphicsContext2D pane)
          padding-x (+ LINE-WIDTH -3)
          width (+ padding-x (* f/LABEL-FONT-WIDTH (count value)))
          height (+ 8 f/LABEL-FONT-HEIGHT)
          color (cond
                  position COLOR-TYPING
                  focus? COLOR-TYPING
                  mark? Color/BLACK
                  true COLOR-IDLE)]
      (.setWidth pane (+ width LINE-WIDTH))
      (.clearRect gc 0 0 (+ width LINE-WIDTH) height)
      ;; (.setStroke gc Color/WHITE)
      ;; (.strokeRect gc 0 0 width height)
      (when mark?
        (doto gc
          (.setFill (Color/web "#AAAAFFAA"))
          (.fillRoundRect 0 0 width height 20 10)))
      (doto gc
        (.setStroke color)
        (.setFill color)
        (.setFont f/LABEL-FONT)
        (.fillText value padding-x f/LABEL-FONT-HEIGHT)
        (draw-caret position width))
      ))
  pane)
