(ns orenolisp.view.controller.main-controller
  (:require [clojure.core.async :as async]
            [orenolisp.key-table :as key-table]
            [orenolisp.state :as st]
            [orenolisp.view.ui.component.typed-history :as history]
            [orenolisp.view.ui.component.context-display :as context-display]
            [orenolisp.view.ui.fx-util :as fx]))

(defonce event-ch (async/chan 1))
(def %state (atom (st/initial-state)))

(defn- should-enter? [{:keys [doing]} can-type?]
  (and (= doing :typing) can-type?))

(defn handle-key-event [{:keys [can-type?] :as key} state]
  (println key)
  (let [key (dissoc key :can-type?)
        window (st/current-window state)
        context (:context window)
        operation (key-table/get-operation context key (:tmp-keymap state))
        next-state (cond
                     (sequential? operation) (reduce (fn [state op] (op state))
                                                     state operation)
                     operation               (operation state)
                     (should-enter? context can-type?) (println "type-letters:" key)
                     true
                     (println "no operation defined:"
                              (:target-type context)
                              (:doing context) key))]
    (or next-state state)))

(defn on-key-event [key]
  (swap! %state #(handle-key-event key %))
  (history/update-typed-key key))

(defn initialize-state []
  (def %state (atom (st/initial-state))))
