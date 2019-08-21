(ns orenolisp.view.ui.component.editable-text
  (:require [orenolisp.view.ui.theme :as theme]
            [orenolisp.view.ui.component.animations :as anim])
  (:import (javafx.scene.paint Color)
           (javafx.scene.canvas Canvas)
           [javafx.scene.effect ColorAdjust Glow]))

(def ^:const line-width 2)

(defn create-node []
  (doto (Canvas.)
    (.setHeight (+ 8 theme/label-font-height))))

(defn- draw-caret [gc position total-width]
  (when position
    (let [caret-x (min total-width
                       (* theme/label-font-width position))]
      (doto gc
        (.setLineWidth line-width)
        (.strokeLine caret-x 6 caret-x (+ theme/label-font-height 2))))))

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
        (.clearRect 0 0 6 theme/label-font-height)
        (.setFill (if position theme/caret-color theme/empty-caret-color))
        (.setStroke (if position theme/caret-color theme/empty-caret-color))
        (draw-caret (or position 0) 6)))

    (let [gc (.getGraphicsContext2D pane)
          padding-x (+ line-width -3)
          width (+ padding-x (* theme/label-font-width (count value)))
          height (+ 8 theme/label-font-height)
          color (cond
                  position theme/focus-text-color
                  focus? theme/focus-text-color
                  mark? theme/mark-text-color
                  true theme/unfocus-text-color)]
      (.setWidth pane (+ width line-width))
      (.clearRect gc 0 0 (+ width line-width) height)
      (when mark?
        (doto gc
          (.setFill theme/mark-text-backcolor)
          (.fillRoundRect 0 0 width height 20 10)))
      (doto gc
        (.setStroke color)
        (.setFill color)
        (.setFont theme/label-font)
        (.fillText value padding-x theme/label-font-height)
        (draw-caret position width))
      ))
  pane)
