(ns orenolisp.state)

(defn ->State [current-exp-id windows expressions]
  {:current-exp-id current-exp-id
   :windows windows
   :expressions expressions})

(defn initial-state []
  (->State nil (array-map) (array-map)))


(defn current-window [{:keys [current-exp-id windows]}]
  (get windows current-exp-id))

(defn temporary-keymap [state description keymap]
  (-> state
      (assoc :tmp-keymap keymap
             :keymap-description description)))
