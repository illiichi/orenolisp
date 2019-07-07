(ns ui.layout-test
  (:require  [clojure.test :refer [deftest testing is] :as t]
             [orenolisp.model.expression :as ex]
             [orenolisp.view.layout.layout :as l]
             [orenolisp.view.layout.layout-decision :as layout-decision]
             [orenolisp.model.conversion :as conv]))

(defn- bounds-printer [bounds]
  (fn [node-id {:keys [type value]}]
    (str (if (= type :ident) value type)
         " "
         (get bounds node-id "!!! NOT FOUND !!!"))))

(deftest layout-sexp
  (let [exp (ex/new-expression)
        sexp '(abcd (def ghi jk [lm (n) [o] [p q r]
                                 12345678901234567890]
                      ((st u) vw xyz)))
        ops (conv/convert-sexp->operations nil sexp)
        op1 (ex/insert-right (:new-id (nth ops 13)) (ex/create-newline))
        _ (ex/run exp (concat ops [op1]))
        bounds (l/calcurate-layout layout-decision/build-size-or-option 500 exp)]
    (is (ex/check-expression (bounds-printer bounds) exp))))



