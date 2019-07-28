(ns orenolisp.launch
  (:require [overtone.config.store]
            [overtone.sc.defaults]
            [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.util :as ut]
            [orenolisp.view.ui.main-ui :as mu]
            [orenolisp.view.controller.keyboard-input :as ki]
            [orenolisp.model.forms :as form]
            [orenolisp.commands.commands :as cmd]
            [orenolisp.model.editor :as ed]
            [orenolisp.watcher.engine :as we]
            [orenolisp.state :as st]
            [orenolisp.sc.eval :as sc]
            [orenolisp.view.ui.component.viewport :as viewport]
            [orenolisp.view.controller.main-controller :as mc]
            [orenolisp.view.controller.window-controller :as wc]
            [clojure.core.async :as async])
  (:import (javafx.scene.paint Color)
           (javafx.scene.canvas Canvas)
           (javafx.scene.text Text)))

(fx/initialize)
(sc/initialize)

(do (fx/run-now (mu/render-base ki/keyboard-ch)
                (mu/layout-content (mu/render)))
    (we/start mc/event-ch)
    (mc/start-loop (async/pipe ki/keyboard-ch mc/event-ch))
    (viewport/move-center))
