(ns view.fix-layout-test
  (:require [clojure.test :refer [deftest testing is] :as t]
            [orenolisp.view.layout.layout :as l]
            [orenolisp.view.layout.fix-layout :as fix]))

(def item-size (l/->Size 100 30))

(defn- dummy-bounds [_ _]
  {:size item-size :bounds {}})

(deftest layout-test
  (let [env {:calcurate-bounds dummy-bounds}
        option (fix/->FixLayoutOption 5 15 [[10 5] [10 5] [10 25]])
        {:keys [size bounds]} (l/calcurate-children-bounds env option 500 [1 2 3])]
    (is (= (l/->Bound 15 15 item-size) (get bounds 1)))
    (is (= (l/->Bound 130 15 item-size) (get bounds 2)))
    (is (= (l/->Bound 245 15 item-size) (get bounds 3)))
    (is (= (l/->Size 375 60) size))))
