(ns orenolisp.view.ui.main-ui
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.component.typed-history :as typed-history]
            [orenolisp.view.ui.component.context-display :as context-display]
            [orenolisp.view.ui.component.viewport :as viewport]
            [orenolisp.view.ui.component.logscreen :as logscreen]
            [clojure.core.async :as async])
  (:import
   (javafx.application Application)
   (javafx.stage Stage StageStyle)
   (javafx.geometry Insets)
   (javafx.scene Scene Group)
   (javafx.scene.transform Scale)
   (javafx.scene.layout Pane StackPane BorderPane GridPane ColumnConstraints Priority)))

(def ui-state (atom nil))
(declare %layer-parent)

(def base-width 1280)
(def base-height 1024)
(def scale 0.5)

(defn calcurate-scale [screen-width screen-height]
  (max (/ screen-width  base-width)
       (/ screen-height base-height)))

(defn render-base [input-ch]
  (reset! ui-state
          (let [root (doto (StackPane.)
                       (.setStyle "-fx-background-color: black")
                       (.setPadding (Insets. 25 0 0 0)))
                container (doto (Group.)
                            (fx/add-child root))
                scene (doto (Scene. container 500 300)
                        (.setOnKeyTyped
                         (fx/event-handler*
                          (fn [e] (async/go (async/>! input-ch e))))))
                stage (doto (Stage. StageStyle/DECORATED)
                        (.setScene scene)
                        (.sizeToScene))
                scale-listener (fx/changed-handler*
                                (fn [_ _ _]
                                  (let [scale (calcurate-scale (-> scene .getWidth)
                                                              (-> scene .getHeight))]
                                    (-> root (.setPrefWidth (/ (-> scene .getWidth) scale)))
                                    (-> root (.setPrefHeight (/ (-> scene .getHeight) scale)))
                                    (doto (-> root .getTransforms)
                                      .clear
                                      (.add (doto (Scale. scale scale)
                                              (.setPivotX 0.0)
                                              (.setPivotY 0.0)))))))]
            (-> scene .widthProperty (.addListener scale-listener))
            (-> scene .heightProperty (.addListener scale-listener))

            (-> scene .getStylesheets (.add "style.css"))
            (.show stage)
            {:root root :stage stage})))

(defn layout-content [content]
  (doto (-> @ui-state :root .getChildren)
    (.clear)
    (.add content)))

(defn- create-bottom [left right]
  (let [panel (GridPane.)]
    (doto (.getColumnConstraints panel)
      (.add (doto (ColumnConstraints.)
              (.setPercentWidth 0)
              (.setFillWidth true)
              (.setHgrow Priority/ALWAYS)))
      (.add (doto (ColumnConstraints.)
              (.setPercentWidth 70)
              (.setFillWidth true)
              (.setHgrow Priority/ALWAYS))))
    (.add panel left 1 1)
    (.add panel right 2 1)
    panel))

(defn render []
  (doto (BorderPane.)
    (.setCenter (doto (StackPane.)
                  (fx/add-child (logscreen/render))
                  (fx/add-child (viewport/render))))
    (.setBottom (create-bottom (typed-history/create-control)
                               (context-display/create)))))
