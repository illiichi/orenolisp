(ns orenolisp.watcher.volume-watcher
  (:require [orenolisp.sc.eval :as sc]
            [orenolisp.sc.timer :as timer]
            [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.component.window-indicator :as indicator]))

(def %exps (atom {}))

(defn register [expression]
  (let [info {:exp-id (get expression :exp-id)
              :rate (get-in expression [:sc-option :rate])
              :num-ch 2}]
    (fx/run-later (indicator/add-window info))
    (swap! %exps assoc (get expression :exp-id) info)))

(defn unregister [exp-id]
  (indicator/remove-window exp-id)
  (swap! %exps dissoc exp-id))

(defn accumulate []
  (let [exps @%exps]
    (fx/run-later
     (indicator/render-volumes (sc/get-exps-vol (keys exps))))))

(defn start []
  (timer/start (* 1/10 1000) #(accumulate)))
