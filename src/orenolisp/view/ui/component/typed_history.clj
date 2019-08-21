(ns orenolisp.view.ui.component.typed-history
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.theme :as theme])
  (:import (javafx.scene.control TextArea)
           (javafx.scene.layout StackPane Pane)))

(declare %key-history-control)

(defn create-control []
  (let [con (doto (TextArea.)
              (.setFont theme/typing-log-font)
              (.setPrefRowCount 2)
              (.setFocusTraversable false)
              (.setWrapText true)
              (.setMouseTransparent true)
              (.setEditable false)
              (.setStyle (str "-fx-border-color: " theme/style-primary-color ";"
                              "-fx-padding: 0px 2px 0 2px;"
                              "-fx-border-width: 1px 1px 0 0;")))
        container (doto (StackPane.)
                    (.setStyle (str "-fx-padding: 12px 0 0 0;"))
                    (fx/add-child con))]
    (def %key-history-control con)
    container))

(defn- key->string [{:keys [char specials]}]
  (if (empty? specials) char
      (->> [[:alt "Meta"]
            [:ctrl "Ctrl"]
            [:super "Super"]]
           (keep #(when (specials (first %)) (second %)))
           (cons char)
           reverse
           (clojure.string/join "-"))))

(defn update-typed-key [key]
  (let [message (str " " (key->string key))]
    (fx/run-later
     (-> %key-history-control (.appendText message)))))
