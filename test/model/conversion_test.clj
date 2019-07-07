(ns model.conversion-test
  (:require  [clojure.test :refer [deftest testing is] :as t]
             [orenolisp.model.editor :as ed]
             [orenolisp.model.conversion :as conv]))

(deftest reflexivity-of-conversion
  (doseq [sexp ['()
                '(1)
                '[[[1]] 3 4/5]
                '(a [b c])
                '(* (sin-osc 880) (env-gen (env-perc 0.05 0.5)
                                           (in (l4/sound-bus :l3 :out-bus) 2)))]]
    (testing (str sexp)
      (let [editor (-> (ed/new-editor)
                       (conv/convert-sexp->editor sexp))]
        (ed/print-editor editor)
        (is (= sexp (conv/convert-editor->sexp editor)))
        ))))
