(ns commands.transforms-test
  (:require  [clojure.test :refer [deftest testing is]]
             [orenolisp.model.editor :as ed]
             [orenolisp.model.conversion :as conv]
             [orenolisp.commands.commands :as cmd]
             [orenolisp.commands.text-commands :as tx]
             [orenolisp.commands.transforms :as trans]))

(deftest wrap-by-map
  (let [sexp '(xxx (* (f (+ 1 2 3) y z) (g a b)) zzz)
        editor (-> (conv/convert-sexp->editor sexp)
                   (ed/move [:root :child :right :child :right :child :right])
                   (ed/mark)
                   (ed/move [:root :child :right])
                   (trans/wrap-by-map)
                   (ed/edit #(tx/insert-char % "x")))]
    (is (ed/check-consistency nil editor))
    (is (= (conv/convert-editor->sexp editor)
           '(xxx (map (fn [x] (* (f x y z) (g a b))) [(+ 1 2 3)]) zzz)))))

(deftest wrap-by-reduce
  (let [sexp '(-> xxx (f (+ 1 2 3) y z) zzz)
        editor (-> (conv/convert-sexp->editor sexp)
                   (ed/move [:root :child :right :right :child :right])
                   (ed/mark)
                   (ed/move :parent)
                   (trans/wrap-by-reduce))]
    (is (ed/check-consistency nil editor))
    (is (= (conv/convert-editor->sexp editor)
        '(-> xxx (u/reduce-> (fn [acc x] (f acc x y z)) [(+ 1 2 3)]) zzz)))))

(deftest threading
  (let [sexp '(xxx (f X y z) zzz)
        editor (-> (conv/convert-sexp->editor sexp)
                   (ed/move [:root :child :right])
                   (trans/threading))]
    (is (ed/check-consistency nil editor))
    (is (= (conv/convert-editor->sexp editor)
           '(xxx (-> X (f y z)) zzz)))))

(deftest wrap-by-range
  (let [sexp '(xxx (f X y z) zzz)
        editor (-> (conv/convert-sexp->editor sexp)
                   (ed/move [:root :child :right])
                   (trans/wrap-by-range))]
    (is (ed/check-consistency nil editor))
    (is (= (conv/convert-editor->sexp editor)
        '(xxx (u/rg-lin (lf-cub:kr 0.01) (f X y z) (f X y z)) zzz)))))

(deftest let-binding
  (let [sexp '(-> xxx (f (+ 1 2 3) y z) zzz)
        editor (-> (conv/convert-sexp->editor sexp)
                   (ed/move [:root :child :right :right :child :right])
                   (ed/mark)
                   (ed/move :parent)
                   (trans/let-binding)
                   (ed/edit #(tx/insert-char % "x")))]
    (is (ed/check-consistency nil editor))
    (is (= (conv/convert-editor->sexp editor)
           '(-> xxx (let [x (+ 1 2 3)] (f x y z)) zzz)))))
