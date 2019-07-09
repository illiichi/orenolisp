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
    (some-> next-state
            st/current-context
            context-display/update-view-by-context)
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
                   (or (:node-type context) (:target-type context))
                   (:doing context) key)))))

(defn dispatch-command [commands]
  (async/go (async/>! event-ch {:type :command :command commands})))

(defmulti on-event :type)
(defmethod on-event :keyboard [{:keys [key]}]
  (history/update-typed-key key)
  (swap! %state #(or (some->> (get-commands-by-key-event % key) (process-command %)) %)))
(defmethod on-event :command [{:keys [command]}]
  (swap! %state #(or (some->> command (process-command %)) %)))

(defn initialize-state []
  (def %state (atom (st/initial-state))))

(defn start-loop [ch]
  (async/go-loop []
    (let [event (async/<! ch)]
      (try (on-event event)
           (catch Exception e
             (.printStackTrace e)))
      (recur))))
