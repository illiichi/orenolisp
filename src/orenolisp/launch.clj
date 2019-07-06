(ns orenolisp.launch
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.main-ui :as mu]
            [orenolisp.view.controller.keyboard-input :as ki]
            [orenolisp.view.controller.main-controller :as mc]))

(fx/initialize)

(fx/run-now (mu/render-base mc/event-ch)
            (mu/layout-content (mu/render)))

(ki/start-loop mc/event-ch mc/on-key-event)



