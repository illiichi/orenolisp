(ns orenolisp.state
  (:require [orenolisp.model.editor :as ed]))

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
(defn current-expression [{:keys [current-exp-id expressions]}]
  (get expressions current-exp-id))

(defn current-editor [{:keys [current-exp-id expressions]}]
  (get-in expressions [current-exp-id :editor]))

(defn current-content [state]
  (ed/get-content (current-editor state)))

(defn current-id [state]
  (let [expression (current-expression state)
        node-id (ed/get-id (:editor expression) :self)]
    [(:exp-id expression) node-id]))

(defn current-node-id [state]
  (:current-id (current-editor state)))

(defn get-ui [{:keys [current-exp-id windows]} node-id]
  (get-in windows [current-exp-id :exp-table node-id :component]))

(defn current-ui [{:keys [current-exp-id] :as state}]
  (get-ui state (current-node-id state)))

(defn temporary-keymap [state description keymap]
  (-> state
      (assoc :tmp-keymap keymap
             :keymap-description description)))

(defn update-current-context [{:keys [current-exp-id] :as state} f]
  (update-in state [:windows current-exp-id :context] f))


(defn pop-multicursor [{:keys [current-exp-id] :as state}]
  (update-in state [:expressions current-exp-id :editor] ed/pop-multicursor))
