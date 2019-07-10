(ns orenolisp.state)

(defn ->State [current-exp-id windows expressions]
  {:current-exp-id current-exp-id
   :windows windows
   :expressions expressions})

(defn initial-state []
  (->State nil (array-map) (array-map)))

(defn current-window [{:keys [current-exp-id windows]}]
  (get windows current-exp-id))

(defn current-context [{:keys [current-exp-id windows]}]
  (get-in windows [current-exp-id :context]))

(defn current-editor [{:keys [current-exp-id expressions]}]
  (get-in expressions [current-exp-id :editor]))

(defn temporary-keymap [state description keymap]
  (-> state
      (assoc :tmp-keymap keymap
             :keymap-description description)))

(defn update-current-context [{:keys [current-exp-id] :as state} f]
  (update-in state [:windows current-exp-id :context] f))
