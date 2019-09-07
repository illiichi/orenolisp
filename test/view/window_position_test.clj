(ns view.window-position-test
  (:require [clojure.test :refer [deftest testing is] :as t]
            [orenolisp.view.controller.window-controller :as wc]
            [orenolisp.view.controller.window-position :as wp]))

(def calcurate-overwrapped
  #'orenolisp.view.controller.window-position/calcurate-overwrapped)
(def move
  #'orenolisp.view.controller.window-position/move)

(deftest move-direction-test
  (doseq [[args expected] [[[10 20 30 40 5] nil]
                           [[30 40 10 20 5] nil]
                           [[10 20 15 40 5] 5]
                           [[10 20 15 19 5] 5]
                           [[10 20 11 18 5] 9]
                           [[10 20 5 12 -5] -2]
                           [[10 20 5 22 -5] -12]
                           [[10 20 8 22 -5] -12]]]
    (is (= expected (apply calcurate-overwrapped args)))))

(deftest move-test
  (let [base (wc/->layout 0 1000 800 100 50)]
    (doseq [[[dx dy] [direction d] [expect-dx expect-dy]]
            [[[500 0] [:w 10] nil]
             [[500 0] [:h 10] nil]
             [[80 0] [:w 20] [(+ 10 20) 0]]
             [[-50 30] [:w 150] [(+ 10 150) 0]]
             [[-70 30] [:w -30] [(- -10 30) 0]]
             [[-50 30] [:h 20] [0 (+ 10 20)]]
             [[-50 60] [:w 150] nil]]]
      (testing (str "move " [dx dy direction expect-dx expect-dy])
        (let [b-layout (-> base
                           (update-in [:position :x] #(+ dx %))
                           (update-in [:position :y] #(+ dy %)))]
          (if expect-dx
            (is (= (-> b-layout
                       (update-in [:position :x] #(+ expect-dx %))
                       (update-in [:position :y] #(+ expect-dy %)))
                   (move base b-layout 10 direction d)))
            (is (= nil (move base b-layout 10 direction d)))))))))

(deftest window-position-test
  (let [l1 (wc/->layout 0 100 150 100 50)
        l2 (update-in l1 [:position :y] #(+ 80 %))
        l3 (update-in l1 [:position :x] #(+ 130 %))
        l4 (update-in l2 [:position :y] #(+ 60 %))
        layouts {"exp-1" l1 "exp-2" l2 "exp-3" l3 "exp-4" l4}]
    (is (= (wp/resize layouts "exp-1" :h 50)
           {"exp-1" (update-in l1 [:size :h] #(+ 50 %))
            "exp-2" (update-in l2 [:position :y] #(+ 30 %))
            "exp-4" (update-in l4 [:position :y] #(+ 30 %))}))))

