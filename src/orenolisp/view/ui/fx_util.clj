(ns orenolisp.view.ui.fx-util
  (:import
   [javafx.util Duration]
   [javafx.scene.layout FlowPane]
   [javafx.scene.canvas Canvas]
   [javafx.animation KeyFrame KeyValue Timeline Interpolator]))
;; based on https://coderwall.com/p/4yjy1a/getting-started-with-javafx-in-clojure

(defn initialize []
  (defonce force-toolkit-init (javafx.embed.swing.JFXPanel.)))

(defn run-later*
  [f]
  (javafx.application.Platform/runLater f))

(defmacro run-later
  [& body]
  `(run-later* (fn [] ~@body)))

(defn run-now*
  [f]
  (let [result (promise)
        _ (run-later
           (deliver result (try (f) (catch Throwable e e))))
        ret @result]
    (if (instance? Exception ret)
      (do (.printStackTrace ret) (throw ret))
      ret)))

(defmacro run-now
  [& body]
  `(run-now* (fn [] ~@body)))

(defn event-handler*
  [f]
  (reify javafx.event.EventHandler
    (handle [this e] (f e))))

(defn changed-handler*
  [f]
  (reify javafx.beans.value.ChangeListener
    (changed [this observable old-value new-value] (f observable old-value new-value))))

(defn add-child [parent child]
  (-> parent .getChildren (.add child))
  parent)

(defn on-animation-finished [anim f]
  (.setOnFinished anim (event-handler* f)))

(defn remove-node
  ([node] (remove-node node nil))
  ([node animation]
   (when-let [parent (.getParent node)]
     (if animation
       (doto animation
         (on-animation-finished (fn [_]
                                  (-> parent .getChildren (.remove node))))
         (.play))
       (-> parent .getChildren (.remove node))))
   node))

(defn css-class [node class-name]
  (doto (-> node .getStyleClass)
    .clear (.add class-name))
  node)

(defn ->KeyFrame [t & props]
  [t props])

(defn create-animation [key-frames]
  (let [animation (doto (Timeline.)
                    ;; (.setInterpolator Interpolator/EASE_OUT)
                    )
        key-frames (->> key-frames
                        (map (fn [[t xs]]
                               (KeyFrame. (Duration. t)
                                          (into-array KeyValue (map (fn [[p v]]
                                                                      (KeyValue. p v))
                                                                    xs))))))]
    (-> animation
        (.getKeyFrames)
        (.addAll key-frames))
    animation))

(defn run-animation
  ([key-frames] (run-animation key-frames nil))
  ([key-frames on-finish]
   (doto (create-animation key-frames)
     (.setOnFinished (event-handler* (fn [_]
                                       (when on-finish (on-finish)))))
     (.play))))

(defn run-animation [key-frames]
  (.play (create-animation key-frames)))

(defn ui-bounds [ui]
  (assert ui)
  {:x (.getLayoutX ui)
   :y (.getLayoutY ui)
   :w (.getWidth ui)
   :h (.getHeight ui)})

(defn ui-center [ui]
  (let [{:keys [x y w h]} (ui-bounds ui)]
    [(+ x (* 1/2 w)) (+ y (* 1/2 h))]))

(defn translate
  ([ui x y max-parent] (translate ui x y (.getWidth max-parent) (.getHeight max-parent)))
  ([ui x y max-x max-y]
   (let [new-x (if (> (+ x (.getPrefWidth ui) 30) max-x)
                 (- max-x (.getPrefWidth ui) 30)
                 x)
         new-y (if (> (+ y (.getPrefHeight ui) 30) max-y)
                 (- max-y (.getPrefHeight ui) 30)
                 y)]
     (.relocate ui (max 0 new-x) (max 0 new-y)))))

(defn translate-on-center
  ([ui x y max-parent]
   (translate ui (- x (.getWidth ui)) (- y (.getHeight ui)) max-parent))
  ([ui x y max-x max-y]
   (translate ui (- x (.getWidth ui)) (- y (.getHeight ui)) max-x max-y)))

(defn resizable-canvas [draw-func]
  (let [container (FlowPane.)
        canvas (proxy [Canvas] []
                 (prefWidth [height] (.getWidth this))
                 (prefHeight [width] (.getHeight this))
                 (isResizable [] true))
        handler (changed-handler* (fn [_ _ _]
                                       (let [gc (.getGraphicsContext2D canvas)
                                             w (.getWidth canvas)
                                             h (.getHeight canvas)] (draw-func gc w h))))]
    (.addListener (.widthProperty canvas) handler)
    (.addListener (.heightProperty canvas) handler)
    (.addListener (.widthProperty container)
                  (changed-handler* (fn [_ _ x] (.setWidth canvas x))))
    (.addListener (.heightProperty container)
                  (changed-handler* (fn [_ _ x] (.setHeight canvas x))))
    (doto container
      (add-child canvas))

    container))

(defn stroke-polyline [graphics-context xxs]
  (let [[xs ys] (->> (apply mapv vector xxs)
                     (map double-array))]
    (.strokePolyline graphics-context xs ys (count xxs))))
(defn fill-polyline [graphics-context xxs]
  (let [[xs ys] (->> (apply mapv vector xxs)
                     (map double-array))]
    (.fillPolygon graphics-context xs ys (count xxs))))

(defn move [component {:keys [x y]}]
  (.relocate component x y))
