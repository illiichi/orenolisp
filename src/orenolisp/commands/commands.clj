(ns orenolisp.commands.commands
  (:require [orenolisp.state :as st]
            [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.model.editor :as ed]
            [orenolisp.view.controller.expression-controller :as ec]
            [orenolisp.view.controller.window-controller :as wc]))

(defn open-initial-window [state]
  (let [new-exp (ec/empty-expression)
        new-win (fx/run-now (wc/initial-window (:exp-id new-exp)))]
    (-> state
        (update :windows #(assoc % (:exp-id new-exp) new-win))
        (update :expressions #(assoc % (:exp-id new-exp) new-exp))
        (assoc :current-exp-id (:exp-id new-exp)))))

(defn set-temporary-keymap [description keymap]
  (fn [state] (st/temporary-keymap state description keymap)))

(defn cancel-temporary-keymap [state]
  (st/temporary-keymap state nil nil))

(defn- with-current-window [{:keys [current-exp-id windows expressions] :as state} f]
  (let [prev-exp (get expressions current-exp-id)
        new-exp (-> prev-exp
                    (ec/apply-step-function f))
        new-window (-> (get windows current-exp-id)
                       (wc/update-window (:editor prev-exp)
                                         (:editor new-exp)))]
    (-> state
        (assoc-in [:expressions current-exp-id] new-exp)
        (assoc-in [:windows current-exp-id] new-window))))

(defn window-command [f]
  (fn [state] (with-current-window state f)))

(defn add [direction form]
  (window-command #(ed/add % direction form)))
(defn move [direction]
  (window-command #(ed/move % direction)))
(defn edit [f]
  (window-command #(ed/edit % f)))
