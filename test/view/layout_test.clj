(ns view.layout-test
  (:require  [clojure.test :refer [deftest testing is] :as t]
             [orenolisp.model.editor :as ed]
             [orenolisp.view.layout.layout :as l]
             [orenolisp.view.layout.layout-decision :as layout-decision]
             [orenolisp.model.conversion :as conv]))

(defn- bounds-printer [bounds]
  (fn [node-id {:keys [type value]}]
    (str (if (= type :ident) value type)
         " "
         (get bounds node-id "!!! NOT FOUND !!!"))))

(deftest layout-sexp
  (let [editor (ed/new-editor)
        sexp '(abcd (def ghi jk [lm (n) [o] [p q r]
                                 12345678901234567890]
                      ((st u) vw xyz)))
        editor (conv/convert-sexp->editor editor sexp)
        option {:w 500 :x 0 :y 0}
        bounds (l/calcurate-layout layout-decision/build-size-or-option option editor)]
    (is (ed/check-consistency (bounds-printer bounds) editor))))



