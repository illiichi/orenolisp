(ns commands.transforms-test
  (:require  [clojure.test :refer [deftest testing is]]
             [orenolisp.model.editor :as ed]
             [orenolisp.model.conversion :as conv]
             [orenolisp.commands.commands :as cmd]
             [orenolisp.commands.text-commands :as tx]
             [orenolisp.commands.transforms :as trans]))

(deftest wrap-by-map
  (testing "wrap"
    (let [sexp '(xxx (* (f (+ 1 2 3) y z) (g a b)) zzz)
          editor (-> (conv/convert-sexp->editor sexp)
                     (ed/move [:root :child :right :child :right :child :right])
                     (ed/mark)
                     (ed/move [:root :child :right])
                     (trans/transform-to-map)
                     (ed/edit #(tx/insert-char % "x")))]
      (is (ed/check-consistency nil editor))
      (is (= (conv/convert-editor->sexp editor)
             '(xxx (map (fn [x] (* (f x y z) (g a b))) [(+ 1 2 3)]) zzz)))))
  (testing "two arguments"
    (let [sexp '(xxx (* (f (+ 1 2 3) 4 z) (g a b)) zzz)
          editor (-> (conv/convert-sexp->editor sexp)
                     (ed/move [:root :child :right :child :right :child :right])
                     (ed/mark)
                     (ed/move :right)
                     (ed/mark)
                     (ed/move [:root :child :right])
                     (trans/transform-to-map)
                     (ed/edit #(tx/insert-char % "x"))
                     (ed/pop-multicursor)
                     (ed/edit #(tx/insert-char % "y")))]
      (is (ed/check-consistency nil editor))
      (is (= (conv/convert-editor->sexp editor)
             '(xxx (map (fn [x y] (* (f x y z) (g a b))) [(+ 1 2 3)] [4]) zzz)))))
  (testing "add argument"
    (let [sexp '(xxx (map (fn [x] (* (f x y z) (g a b))) [(+ 1 2 3)]) zzz)
          editor (-> (conv/convert-sexp->editor sexp)
                     (ed/move [:root :child :right :child :right :child :right :right
                               :child :right :child :right])
                     (trans/transform-to-map)
                     (ed/edit #(tx/insert-char % "x2")))]
      (is (ed/check-consistency nil editor))
      (is (= (conv/convert-editor->sexp editor)
             '(xxx (map (fn [x x2] (* (f x2 y z) (g a b))) [(+ 1 2 3)] [x]) zzz))))))

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
           '(xxx (u/lin-lin (lf-cub:kr 1) (f X y z) (f X y z)) zzz)))))

(deftest let-binding
  (testing "new let binding"
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
  (testing "add binding"
    (let [sexp '(-> xxx (let [x1 (+ 4 5 6)]
                          (+ 1 (* 2 3)) y z)
                    zzz)
          editor (-> (conv/convert-sexp->editor sexp)
                     (ed/move [:root :child :right :right :child :right :right
                               :child :right :right])
                     (trans/let-binding)
                     (ed/edit #(tx/insert-char % "x2")))]
      (is (ed/check-consistency nil editor))
      (is (= (conv/convert-editor->sexp editor)
             '(-> xxx (let [x1 (+ 4 5 6)
                            x2 (* 2 3)]
                        (+ 1 x2) y z) zzz))))))

(deftest iterate-multiply
  (let [sexp '(-> xxx 1 zzz)
        editor (-> (conv/convert-sexp->editor sexp)
                   (ed/move [:root :child :right :right])
                   (trans/iterate-multiply)
                   (ed/edit #(tx/insert-char % "3/2")))]
    (is (ed/check-consistency nil editor))
    (is (= (conv/convert-editor->sexp editor)
           '(-> xxx (iterate (fn [x] (* x 3/2)) 1) zzz)))))
