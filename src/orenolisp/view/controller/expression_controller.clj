(ns orenolisp.view.controller.expression-controller
  (:require [orenolisp.model.editor :as ed]
            [orenolisp.util :as ut]))

(def exp-id-counter (ut/generate-counter))
(defn- new-exp-id []
  (str "exp-" (exp-id-counter)))

(defrecord Expression [exp-id sc-option editor])

(defn empty-expression []
  (->Expression (new-exp-id) {:rate :audio :layer-no 0} (ed/new-editor)))

(defn new-expression [editor sc-option]
  (->Expression (new-exp-id) sc-option editor))

(defn apply-step-function [^Expression expression step-func]
  (update expression :editor #(or (step-func (ed/copy %)) %)))
