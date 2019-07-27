(ns orenolisp.watcher.watchers
  (:require [orenolisp.commands.commands :as cmd]
            [orenolisp.model.editor :as ed]
            [orenolisp.util :as u]))

(defn gauge-watcher [exp-id node-id from to]
  (fn [x]
    (when (not (nil? x))
      (if (u/enough-equal to x)
        (cmd/excursion exp-id node-id
                       #(-> %
                            (ed/move [:child :right])
                            (ed/transport :self node-id)))
        (cmd/excursion exp-id node-id
                       #(ed/edit % (fn [m] (assoc m :ratio (/ (- x from) (- to from))))))))))
