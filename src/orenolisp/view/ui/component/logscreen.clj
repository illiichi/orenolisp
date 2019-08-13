(ns orenolisp.view.ui.component.logscreen
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.font-util :as f]
            [clojure.core.async :as async])
  (:import (javafx.scene.control TextArea)
           (javafx.scene.paint Color)
           (javafx.scene.layout StackPane Pane)))

(declare %textarea)

(def %message-queue (async/chan))

(defn render []
  (def %textarea (doto (TextArea.)
                   (.setFont f/LOG-FONT)
                   (.setStyle "-fx-text-fill: #88AAAA;")
                   (.setFocusTraversable false)
                   (.setWrapText true)
                   (.setMouseTransparent true)
                   (.setEditable false)))
  %textarea)

(def ^:const typing-speed-per-sentence 1000)

(def line-counter (atom 0))

(defn writeln [& messages]
  (swap! line-counter inc)
  (async/go (async/>! %message-queue messages)))

(defn write-a-letter [c]
  (fx/run-later (.appendText %textarea c)))

(defn start []
  (async/go-loop []
    (let [messages (async/<! %message-queue)
          message (apply str messages)
          typing-speed (/ typing-speed-per-sentence (count message))]
      (when (> @line-counter 400)
        (reset! line-counter 0)
        (fx/run-later (.clear %textarea)))
      (doseq [c message]
        (write-a-letter (str c))
        (async/<! (async/timeout typing-speed)))
      (write-a-letter "\n")
      (recur))))
