(ns orenolisp.view.controller.expression-controller
  (:require [orenolisp.model.editor :as ed]
            [orenolisp.util :as ut]))

(def exp-id-counter (ut/generate-counter))
(defn- new-exp-id []
  (str "exp-" (exp-id-counter)))

(defrecord Expression [exp-id editor])

(defn empty-expression []
  (->Expression (new-exp-id) (ed/new-editor)))

(defn apply-step-function [^Expression expression step-func]
  (update expression :editor #(or (step-func (ed/copy %)) %)))
