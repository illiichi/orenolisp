(ns orenolisp.view.controller.main-controller
  (:require [clojure.core.async :as async]
            [orenolisp.view.ui.component.typed-history :as history]
            [orenolisp.view.ui.component.context-display :as context-display]
            [orenolisp.view.ui.fx-util :as fx]))

(defonce event-ch (async/chan 1))
(def %state (atom {}))

(defn on-key-event [key]
  (history/update-typed-key key))

