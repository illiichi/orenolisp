(ns orenolisp.view.layout.layout-decision
  (:require [orenolisp.view.layout.layout :as l]
            [orenolisp.view.layout.flow-layout :as fl]
            [orenolisp.view.layout.fix-layout :as fix]
            [orenolisp.util :as ut]
            [orenolisp.view.ui.theme :as theme]))

(defn- calcurate-string-size [string]
  (l/->Size (* theme/label-font-width (count string))
            (+ 8 theme/label-font-height)))
(defn- calcurate-in-ugen-size [{:keys [exp-id]}]
  (l/->Size (+ (* 3 12) theme/in-ugen-font-height
               (* theme/in-ugen-font-width (count exp-id)))
            (+ (* 2 8) theme/in-ugen-font-height)))

(defn build-size-or-option [{:keys [type value] :as m}]
  (case type
    :ident (calcurate-string-size value)
    :in    (calcurate-in-ugen-size m)
    :gauge (fix/->FixLayoutOption 5 0 [[32 5] [15 5] [15 15]])
    :paren (fl/->FlowOption true 10 0 5 2 theme/label-font-width theme/label-font-height)
    :newline (l/->Size-newline 0 theme/label-font-height)
    :vector (fl/->FlowOption false 8 3 5 2 theme/label-font-width theme/label-font-height)
    (ut/error "unknown type" type m)))


