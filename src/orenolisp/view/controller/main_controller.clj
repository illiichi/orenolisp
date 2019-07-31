(ns orenolisp.view.controller.main-controller
  (:require [clojure.core.async :as async]
            [orenolisp.key-table :as key-table]
            [orenolisp.state :as st]
            [orenolisp.commands.commands :as cmd]
            [orenolisp.commands.text-commands :as tx]
            [orenolisp.view.ui.component.typed-history :as history]
            [orenolisp.view.ui.component.context-display :as context-display]
            [orenolisp.view.ui.fx-util :as fx]))

(defonce event-ch (async/chan 1))
(def %state (atom (st/initial-state)))

(defn- should-enter? [{:keys [doing]} can-type?]
  (and (= doing :typing) can-type?))

(defn process-command [state commands]
  (let [next-state (if (sequential? commands) (reduce (fn [state op]
                                                        (if-let [next-state (op state)]
                                                          next-state
                                                          (reduced nil)))
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
          (cmd/edit #(tx/insert-char % (str (:char key))))
          (println "no commands defined:"
                   (or (:node-type context) (:target-type context))
                   (:doing context) key)))))

(defn dispatch-command [commands]
  (async/go (async/>! event-ch {:type :command :command commands})))

(defmulti on-event (fn [key state] (:type key)))

(defmethod on-event :keyboard [{:keys [key]} state]
  (history/update-typed-key key)
  (when-let [next-state (some->> (get-commands-by-key-event state key)
                                 (process-command state))]
    (if-let [description (:keymap-description next-state)]
      (context-display/update-view description)
      (some-> next-state
              st/current-context
              context-display/update-view-by-context))
    next-state))
(defmethod on-event :command [{:keys [command]} state]
  (some->> command (process-command state)))

(defn initialize-state []
  (def %state (atom (st/initial-state))))

(defn start-loop [ch]
  (async/go-loop []
    (let [event (async/<! ch)
          current-state @%state
          next-state (try (on-event event current-state)
                          (catch Throwable e
                            (.printStackTrace e)))]
      (when next-state
        (reset! %state next-state))
      (recur))))
