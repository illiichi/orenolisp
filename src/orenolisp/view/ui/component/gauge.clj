(ns orenolisp.view.ui.component.gauge
  (:require [orenolisp.view.ui.font-util :as f])
  (:import (javafx.scene.paint Color)
           (javafx.scene.canvas Canvas)
           [javafx.scene.effect ColorAdjust Glow]))

(defn create-node []
  (doto (Canvas.)
    (.setEffect (doto (Glow.)
                  (.setLevel 1)))))

(defn render [ui {:keys [size focus? mark? ratio]} {:keys [exp?]} children-bounds]
  (let [{:keys [w h]} size
        [size-from size-to size-dur] (map :size children-bounds)]
    (doto ui
      (.setWidth w)
      (.setHeight h))
    (if ratio
      (doto (.getGraphicsContext2D ui)
        (.clearRect 0 0 w h)
        (.setFill (Color/web "#AACCFF33"))
        (.fillRect 1 1 (* ratio w) (- h 2)))
      (doto (.getGraphicsContext2D ui)
        (.clearRect 0 0 w h)))
    (doto (.getGraphicsContext2D ui)
      (.setLineWidth (if focus? 3 1))
      (.setStroke (Color/web "#AACCFF"))
      (.strokeRect 0 0 w h)
      (.setFill Color/WHITE)
      (.setFont f/METER-FONT)
      (.fillText "from" 5 12)
      (.fillText "to" (+ 5 20 10 (:w size-from) 5) 12)
      (.fillText "in" (- w 5 10 (:w size-dur) 21) 12)
      (.fillText "sec"(- w 21) (- h 4))
      (.setFont f/METER-TYPE-FONT)
      (.fillText (if exp? "exp" "lin") 5 (- h 6)))))

