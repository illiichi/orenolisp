(ns orenolisp.watcher.watchers
  (:require [orenolisp.commands.commands :as cmd]
            [orenolisp.model.editor :as ed]
            [orenolisp.util :as u]))

(defn gauge-watcher [exp-id node-id exp? from to]
  (if (= from to)
    (fn [x]
      (cmd/excursion exp-id node-id
                     #(-> %
                          (ed/move [:child :right])
                          (ed/transport :self node-id))))
    (fn [x]
      (when (not (nil? x))
        (let [r (/ (- x from) (- to from))]
          (if (u/enough-equal r 1.0)
            (cmd/excursion exp-id node-id
                           #(-> %
                                (ed/move [:child :right])
                                (ed/transport :self node-id)))
            (cmd/edit-attributes exp-id node-id #(-> %
                                                     (assoc :ratio r)
                                                     (assoc :value x)))))))))

(defn create-gauge-watcher [{:keys [exp-id editor]} node-id]
  (let [exp? (:exp? (ed/get-content editor))
        [from to] (map #(-> (ed/get-content editor %)
                            :value
                            read-string)
                       (ed/get-children-ids editor node-id))]
    (gauge-watcher exp-id node-id exp? from to)))
