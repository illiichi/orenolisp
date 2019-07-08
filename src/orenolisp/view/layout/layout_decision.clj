(ns orenolisp.view.layout.layout-decision
  (:require [orenolisp.view.layout.layout :as l]
            [orenolisp.view.layout.flow-layout :as fl]
            [orenolisp.util :as ut]
            [orenolisp.view.ui.font-util :as f]))

(def ^:const LINE-WIDTH 2)
(defn- calcurate-string-size [string]
  (l/->Size (+ (* 2 LINE-WIDTH) (* f/LABEL-FONT-WIDTH (count string)))
            (+ 8 f/LABEL-FONT-HEIGHT)))
(defn- calcurate-in-ugen-size [{:keys [layer-id]}]
  (l/->Size (+ (* 3 12) f/PORTAL-FONT-HEIGHT (* f/PORTAL-FONT-WIDTH (count layer-id)))
            (+ (* 2 8) f/PORTAL-FONT-HEIGHT)))

(defn build-size-or-option [{:keys [type value] :as m}]
  (case type
    :ident (calcurate-string-size value)
    :in    (calcurate-in-ugen-size m)
    :paren (fl/->FlowOption true 15 0 10 4 f/LABEL-FONT-WIDTH f/LABEL-FONT-HEIGHT)
    :newline (l/->Size-newline 0 f/LABEL-FONT-HEIGHT)
    :vector (fl/->FlowOption false 8 0 10 2 f/LABEL-FONT-WIDTH f/LABEL-FONT-HEIGHT)
    (ut/error "unknown type" type m)))


