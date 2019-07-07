(ns orenolisp.commands.commands
  (:require [orenolisp.state :as st]
            [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.controller.expression-controller :as ec]
            [orenolisp.view.controller.window-controller :as wc]))

(defn open-initial-window [state]
  (let [new-exp (ec/empty-expression)
        new-win (fx/run-now (wc/initial-window (:exp-id new-exp)))]
    (-> state
        (update :windows #(assoc % (:exp-id new-exp) new-win))
        (update :expressions #(assoc % (:exp-id new-exp) new-win))
        (assoc :current (:exp-id new-exp)))))

(defn set-temporary-keymap [state description keymap]
  (st/temporary-keymap state description keymap))

(defn cancel-temporary-keymap [state]
  (st/temporary-keymap state nil nil))

(defn with-current-window [{:keys [current-exp-id windows expressions]} f]
  (ec/process exp f))
