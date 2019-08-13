(ns orenolisp.sc.eval
  (:require [llll.core :as l4]
            [llll.macro.defsound :refer :all]
            [llll.macro.control :refer :all]
            [orenolisp.sc.builder :as sb]
            [orenolisp.sc.util :as u]
            [orenolisp.view.ui.component.logscreen :as log]
            [overtone.sc.server :as sc-server]))

(defn def-set-volume []
  (defn set-volume [exp v]
    (println "not initialized:" (:exp-id exp) ":" v)))

(defn finish []
  (l4/finish)
  (def-set-volume)
  (sc-server/kill-server)
  (log/writeln "SuperCollider finished."))

(defn doit [sexp]
  (binding [*ns* (find-ns 'orenolisp.sc.eval)]
    (log/writeln "evaluate:" sexp)
    (eval sexp)))

(defn initialize []
  (defn set-volume [exp v]
    (doit (sb/exp->control-vol exp {:set v})))

  (binding [*ns* (find-ns 'orenolisp.sc.eval)]
    (use 'overtone.core)
    (sc-server/connect-external-server "localhost" 57110)
    (l4/initialize {})
    (log/writeln "SuperCollider connected.")))
(def-set-volume)

(defn get-exps-vol [exp-ids]
  (let [lines @llll.engine.engine/%lines]
    (->> exp-ids
         (map (fn [id]
                {id (-> (get lines (keyword id))
                        :state :node-holder deref
                        :node :taps (get "out") deref)}))
         (into {}))))

(defn get-node-value [exp-id node-id]
  (let [lines @llll.engine.engine/%lines]
    (some-> (get lines (keyword exp-id))
            :state :node-holder deref
            :node
            :taps (get (str "n-" node-id)) deref)))

(defn stop-sound [exp-id]
  (log/writeln "sound is stopping... " exp-id)
  (l4/stop (keyword exp-id)))
