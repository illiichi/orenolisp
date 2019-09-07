(ns orenolisp.view.controller.window-position
  (:require [orenolisp.util :as u]))

(def window-margin 10)

(defn- corners [{:keys [position size]} margin]
  (let [x1 (:x position)
        y1 (:y position)]
    [(- x1 margin) (- y1 margin) (+ x1 (:w size) margin) (+ y1 (:h size) margin)]))

(defn- calcurate-overwrapped [a1 a2 b1 b2 d]
  (let [a2-b1 (- a2 b1)
        a1-b2 (- a1 b2)]
    (when (< (* a2-b1 a1-b2) 0)
      (if (< 0 d)
        a2-b1 a1-b2))))

(defn- partition-margin [margin direction x]
  (let [x2 (max 0 (- x margin))]
    [(* direction x2) (* direction (- x x2))]))

(defn- move [a-layout b-layout margin direction d]
  (let [[ax1 ay1 ax2 ay2] (corners a-layout margin)
        [bx1 by1 bx2 by2] (corners b-layout 0)
        dx (calcurate-overwrapped ax1 ax2 bx1 bx2 d)
        dy (calcurate-overwrapped ay1 ay2 by1 by2 d)]
    (when (and dx dy)
      (case direction
        :w (update-in b-layout [:position :x] #(+ % dx))
        :h (update-in b-layout [:position :y] #(+ % dy))))))

(defn resize [layouts exp-id direction d]
  (loop [acc {}
         exp-id exp-id
         base-layout (-> (get layouts exp-id)
                         (update-in [:size direction] #(+ % d)))
         layouts layouts]
    (let [new-layouts (->> (dissoc layouts exp-id)
                           (u/map-value #(move base-layout % window-margin direction d))
                           (u/filter-value identity))
          next-layouts (merge layouts new-layouts)
          next-acc (merge acc {exp-id base-layout})]
      (if (empty? new-layouts)
        next-acc
        (let [[next-exp-id next-base-layout] (first new-layouts)]
          (recur next-acc next-exp-id next-base-layout next-layouts))))))

