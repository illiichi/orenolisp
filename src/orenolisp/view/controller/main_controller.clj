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

(defn process-command [state commands]
  (let [next-state (if (sequential? commands) (reduce (fn [state op] (op state))
                                                     state commands)
                       (commands state))]
    (or next-state state)))

(defn get-commands-by-key-event [state {:keys [can-type?] :as key}]
  (let [key (dissoc key :can-type?)
        window (st/current-window state)
        context (:context window)
        commands (key-table/get-operation context key (:tmp-keymap state))]
    (or commands
        (if (should-enter? context can-type?)
          (println "type-letters:" key)
          (println "no commands defined:"
                   (:target-type context)
                   (:doing context) key)))))

(defn dispatch-command [commands]
  (swap! %state #(or (process-command % commands)
                     %)))

(defn on-key-event [key]
  (history/update-typed-key key)
  (swap! %state #(or (some->> (get-commands-by-key-event % key)
                              (process-command %))
                     %)))

(defn initialize-state []
  (def %state (atom (st/initial-state))))
