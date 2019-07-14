(ns orenolisp.sc.timer
  (:require [overtone.at-at :as at]))

(defonce my-pool (at/mk-pool))

(declare timer-func)

(defn- repeat-timer [interval f]
  (f)
  (at/at (+ (at/now) interval) #(#'timer-func interval f) my-pool))

(defn- stop-timer [interval f]
  (println "stop timer"))

(defn start [interval f]
  (def timer-func repeat-timer)
  (timer-func interval f))

(defn stop []
  (def timer-func stop-timer))



