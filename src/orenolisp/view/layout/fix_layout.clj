(ns orenolisp.view.layout.fix-layout
  (:require [orenolisp.view.layout.layout :as l]))

(defrecord FixLayoutOption [padding-x padding-y margins])

(defn- calcurate-children-bounds*
  [{:keys [calcurate-bounds]} {:keys [padding-x padding-y margins]} max-width node-ids]
  (assert (= (count node-ids) (count margins))
          (str "wrong number of children:" margins node-ids))
  (loop [{:keys [size bounds] :as acc} {:size (l/->Size padding-x 0) :bounds {}}
         [[node-id pad-l pad-r] & xs] (map cons node-ids margins)]
    (if node-id
      (let [rest-size (- max-width (:w size) pad-l pad-r)
            {child-size :size child-bounds :bounds} (calcurate-bounds rest-size node-id)
            this-bound (l/->Bound (+ (:w size) pad-l) padding-y child-size)
            new-acc (-> acc
                        (update :bounds merge child-bounds {node-id this-bound})
                        (update-in [:size :w] #(+ % pad-l (:w child-size) pad-r))
                        (update-in [:size :h] max (:h child-size)))]
        (recur new-acc xs))
      (-> acc
          (update-in [:size :w] #(+ % padding-x))
          (update-in [:size :h] #(+ % (* 2 padding-y)))))))

(defmethod l/calcurate-children-bounds FixLayoutOption
  [layout-env layout-option max-width node-ids]
  (calcurate-children-bounds* layout-env layout-option max-width node-ids))
