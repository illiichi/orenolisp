(ns orenolisp.view.layout.layout-decision
  (:require [orenolisp.view.layout.layout :as l]
            [orenolisp.view.layout.flow-layout :as fl]
            [orenolisp.view.layout.fix-layout :as fix]
            [orenolisp.util :as ut]
            [orenolisp.view.ui.font-util :as f]))

(defn- calcurate-string-size [string]
  (l/->Size (* f/LABEL-FONT-WIDTH (count string))
            (+ 8 f/LABEL-FONT-HEIGHT)))
(defn- calcurate-in-ugen-size [{:keys [exp-id]}]
  (l/->Size (+ (* 3 12) f/PORTAL-FONT-HEIGHT (* f/PORTAL-FONT-WIDTH (count exp-id)))
            (+ (* 2 8) f/PORTAL-FONT-HEIGHT)))

(defn build-size-or-option [{:keys [type value] :as m}]
  (case type
    :ident (calcurate-string-size value)
    :in    (calcurate-in-ugen-size m)
    :gauge (fix/->FixLayoutOption 5 0 [[32 5] [15 5] [15 15]])
    :paren (fl/->FlowOption true 10 0 5 2 f/LABEL-FONT-WIDTH f/LABEL-FONT-HEIGHT)
    :newline (l/->Size-newline 0 f/LABEL-FONT-HEIGHT)
    :vector (fl/->FlowOption false 8 0 5 2 f/LABEL-FONT-WIDTH f/LABEL-FONT-HEIGHT)
    (ut/error "unknown type" type m)))


