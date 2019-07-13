(ns commands.transforms-test
  (:require  [clojure.test :refer [deftest testing is]]
             [orenolisp.model.editor :as ed]
             [orenolisp.model.conversion :as conv]
             [orenolisp.commands.commands :as cmd]
             [orenolisp.commands.transforms :as trans]))

(deftest wrap-by-map
  (let [sexp '(xxx (* (f (+ 1 2 3) y z) (g a b)) zzz)
        editor (-> (conv/convert-sexp->editor sexp)
                   (ed/move [:root :child :right :child :right :child :right])
                   (ed/mark)
                   (ed/move [:root :child :right])
                   (trans/wrap-by-map))]
    (is (ed/check-consistency nil editor))
    (= (conv/convert-editor->sexp editor)
       '(xxx (map (fn [x] (* (f x y z) (g a b))) [(+ 1 2 3)]) zzz))))

