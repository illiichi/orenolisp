(ns orenolisp.launch
  (:require [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.view.ui.main-ui :as mu]
            [orenolisp.view.controller.keyboard-input :as ki]
            [orenolisp.model.editor :as ed]
            [orenolisp.commands.commands :as cmd]
            [orenolisp.view.ui.component.viewport :as viewport]
            [orenolisp.view.controller.main-controller :as mc]
            [orenolisp.view.controller.window-controller :as wc]
            [clojure.core.async :as async])
  (:import (javafx.scene.paint Color)
           (javafx.scene.canvas Canvas)
           (javafx.scene.text Text)))

(fx/initialize)

(do (fx/run-now (mu/render-base ki/keyboard-ch)
                (mu/layout-content (mu/render)))
    (mc/start-loop (async/pipe ki/keyboard-ch mc/event-ch))
    (viewport/move-center))

(do
  (mc/initialize-state)
  (fx/run-now (mu/layout-content (mu/render)))
  (viewport/move-center))

(mc/dispatch-command (partial cmd/with-current-window
                              #(-> % (ed/add :child {:type :paren}))))

mc/%state
