(ns orenolisp.watcher.engine
  (:require [orenolisp.sc.timer :as timer]
            [clojure.core.async :as async]
            [orenolisp.view.ui.component.logscreen :as log]))

(def %handler-table (atom {}))

(defn- accumulate [event-ch]
  (let [commands (keep (fn [[[exp-id node-id] f]]
                         (try (f exp-id node-id) (catch Exception e (prn e))))
                       @%handler-table)]
    (async/go (async/>! event-ch {:type :command :command commands}))))

(defn start [event-ch]
  (timer/start (* 1/10 1000) #(accumulate event-ch)))

(defn stop []
  (timer/stop))

(defn register [exp-id node-id f]
  (swap! %handler-table assoc [exp-id node-id] f))

(defn unregister [exp-id node-id]
  (println "unregistered:" exp-id node-id)
  (log/writeln "unregistered: " exp-id " node-id:" node-id)
  (swap! %handler-table dissoc [exp-id node-id]))

(defn unregister-all [exp-id node-ids]
  (swap! %handler-table
         #(reduce (fn [acc node-id]
                    (if-let [handler (get acc [exp-id node-id])]
                      (do (println "unregistered:" exp-id node-id)
                          (log/writeln "unregistered: " exp-id " node-id:" node-id)
                          (dissoc acc [exp-id node-id]))
                      acc))
                  % node-ids)))
