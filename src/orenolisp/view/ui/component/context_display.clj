(ns orenolisp.view.ui.component.context-display
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.theme :as theme])
  (:import (javafx.scene.text Text)
           (javafx.scene.layout GridPane StackPane ColumnConstraints Pane)))

(declare %canvas)

(defn- render-border [gc w h]
  (doto gc
    (.setStroke theme/primary-color)
    (.setFill theme/primary-color)
    (.setLineWidth 2)
    (fx/stroke-polyline [[0 12] [12 0] [w 0]])
    (.setFont theme/small-label-font)
    (.fillText "doing" 24 18)
    (.fillText "target" (+ 24 (/ w 2)) 18)))

(defn- get-canvas []
  (-> %canvas (.getChildren) (.get 0)))

(defn- render-text [doing-msg in-msg ]
  (let [canvas (get-canvas)
        w (.getWidth canvas)
        h (.getHeight canvas)]
    (doto (.getGraphicsContext2D canvas)
      (.clearRect 22 24 w h)
      (.setFill theme/primary-color)
      (.setFont theme/context-font)
      (.fillText doing-msg 24 (- h 10))
      (.fillText in-msg (+ 24 (/ w 2)) (- h 10)))))

(defn create []
  (def %canvas (fx/resizable-canvas render-border))
  %canvas)

(defn update-view [description]
  (render-text description ""))

(defn update-view-by-context
  [{:keys [doing target-type node-type] :as context}]
  (when context
    (render-text (name doing)
                 (some->> (or node-type target-type) name clojure.string/upper-case))))

