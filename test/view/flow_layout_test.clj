(ns view.flow-layout-test
  (:require  [clojure.test :refer [deftest testing is] :as t]
             [orenolisp.view.layout.layout :as l]
             [orenolisp.view.layout.flow-layout :as fl]))

(defn- arrange-lines [lines]
  (->> lines
       (map :elements)
       (sort-by :x)
       (map keys)))

(def elements [["a" (l/->Size 30 30)]
               ["b" (l/->Size 50 20)]
               ["c" (l/->Size 40 20)]
               ["d" (l/->Size 60 20)]
               ["e" (l/->Size 20 1)]
               ["f" (l/->Size 10 2)]
               ["g" (l/->Size 20 3)]
               ["h" (l/->Size 40 2)]])

(defn calcurate-fix-size [max-width node-id]
  (if (and (> max-width 500) (= node-id "e"))
    {:size (l/->Size-newline 0 0) :bounds nil}
    {:size (->> elements
                (filter #(= (first %) node-id))
                first
                second)
     :bounds {(str node-id node-id) (l/->Size 0 0)}}))

(defn check-consitency-bounds [bounds]
  (is (= (set (concat (map first elements)
                      (map (fn [[x]] (str x x)) elements)))
         (set (keys bounds)))))

(deftest arrangement-test
  (doseq [[option expected size]
          [[(fl/->FlowOption false 0 0 0 0 30 45)
            [["a" "b"]
             ["c" "d"]
             ["e" "f" "g" "h"]]
            (l/->Size 100 53)]
           [(fl/->FlowOption false 5 10 0 0 30 45)
            [["a" "b"]
             ["c"]
             ["d" "e" "f"]
             ["g" "h"]]
            (l/->Size 100 (+ 10 73 10))]
           [(fl/->FlowOption false 10 0 0 10 30 45)
            [["a" "b"]
             ["c"]
             ["d" "e"]
             ["f" "g" "h"]]
            (l/->Size 100 (+ 73 (* (dec 4) 10)))]
           [(fl/->FlowOption false 0 0 20 0 30 45)
            [["a" "b"]
             ["c"]
             ["d" "e"]
             ["f" "g"]
             ["h"]]
            (l/->Size 100 75)]
           [(fl/->FlowOption false 5 3 10 5 30 45)
            [["a" "b"]
             ["c"]
             ["d" "e"]
             ["f" "g" "h"]]
            (l/->Size 100 (+ 3 73 (* (dec 4) 5) 3))]

           [(fl/->FlowOption true 0 0 0 0 30 45)
            [["a" "b"]
             ["c"]
             ["d"]
             ["e" "f" "g"]
             ["h"]]
            (l/->Size 90 75)]
           [(fl/->FlowOption true 15 0 0 0 30 45)
            [["a"] ["b"] ["c"] ["d"]
             ["e" "f"]
             ["g"] ["h"]]
            (l/->Size 120 97)]
           [(fl/->FlowOption true 5 15 10 8 30 45)
            [["a" "b"] ["c"] ["d"]
             ["e" "f"]
             ["g"] ["h"]]
            (l/->Size 110 (+ 15 77 (* (dec 6) 8) 15))]]]
    (testing (str option)
      (let [max-width 100
            env {:calcurate-bounds calcurate-fix-size}
            node-ids (->> elements (map first))
            {:keys [lines bounds]} (fl/calcurate-wrap-layout option env max-width node-ids)]
        (is (= expected (arrange-lines lines)))
        (is (= size (fl/calcurate-container-size option  lines)))
        (check-consitency-bounds (fl/accumulate-bounds bounds lines))))))

(deftest new-line-arrangement-test
  (doseq [[option size]
          [[(fl/->FlowOption false 0 0 0 0 30 45)
            (l/->Size 180 33)]
           [(fl/->FlowOption false 5 10 0 0 30 45)
            (l/->Size 190 53)]
           [(fl/->FlowOption false 10 0 0 10 30 45)
            (l/->Size 200 43)]
           [(fl/->FlowOption false 0 0 20 0 30 45)
            (l/->Size 240 33)]
           [(fl/->FlowOption false 5 3 10 5 30 45)
            (l/->Size 220 44)]

           [(fl/->FlowOption true 0 0 0 0 30 45)
            (l/->Size 180 33)]
           [(fl/->FlowOption true 15 0 0 0 30 45)
            (l/->Size 210 33)]
           [(fl/->FlowOption true 5 15 10 8 30 45)
            (l/->Size 220 71)]]]
    (testing (str option)
      (let [max-width 1000
            env {:calcurate-bounds calcurate-fix-size}
            node-ids (->> elements (map first))
            {:keys [lines bounds]} (fl/calcurate-wrap-layout option env max-width node-ids)]
        (is (= [["a" "b" "c" "d" "e"]
                ["f" "g" "h"]] (arrange-lines lines)))
        (is (= size (fl/calcurate-container-size option  lines)))))))
