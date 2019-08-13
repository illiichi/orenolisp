(ns orenolisp.view.ui.component.logscreen
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.font-util :as f]
            [clojure.core.async :as async])
  (:import (javafx.scene.control TextArea)
           (javafx.scene.paint Color)
           (javafx.scene.layout StackPane Pane)))

(declare %textarea)

(def %message-queue (async/chan 1))

(defn render []
  (def %textarea (doto (TextArea.)
                 (.setFont f/LOG-FONT)
                 (.setPrefRowCount 2)
                 (.setFocusTraversable false)
                 (.setWrapText true)
                 (.setMouseTransparent true)
                 (.setEditable false)))
  %textarea)

(def ^:const typing-speed-per-sentence 500)

(defn write [messages]
  (let [letters (mapcat (partial map str) messages)
        speed (/ typing-speed-per-sentence (count letters))]
    (async/go (doseq [c letters]
                (println c)
                (async/>! %message-queue [c speed])))))
(defn writeln [& messages]
  (write (concat messages ["\n"])))

(defn flash-a-letter [c]
  (.appendText %textarea c))

(defn start []
  (async/go-loop []
    (let [[c typing-speed] (async/<! %message-queue)]
      (fx/run-later (flash-a-letter c))
      (async/<! (async/timeout typing-speed))
      (recur))))
