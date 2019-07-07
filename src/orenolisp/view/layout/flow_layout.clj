(ns orenolisp.view.layout.flow-layout
  (:require [orenolisp.view.layout.layout :as l]))

(defrecord FlowOption [indent-top padding-x padding-y gap-v gap-h min-width min-height]
  Object
  (toString [this] (str [indent-top padding-x padding-y gap-v gap-h min-width min-height])))

(defn calcurate-wrap-layout [{:keys [indent-top padding-x padding-y gap-h gap-v]}
                             {:keys [calcurate-bounds]}
                             max-width
                             children-ids]
  (letfn [(empty-line [top-x top-y]
            {:x top-x :y top-y :width 0 :height 0 :elements {}})

          (concat-line [line gap node-id size]
            (-> line
                (update :width (partial + gap (:w size)))
                (update :height (partial max (:h size)))
                (update :elements assoc node-id
                        (l/->Bound (+ gap (:x line) (:width line)) (:y line) size))))

          (new-line [indent prev-line]
            (-> (empty-line (:x prev-line) (+ (:y prev-line) (:height prev-line)))
                (update :width (partial + indent))
                (update :y (partial + gap-h))))
          (concat-new-line [indent prev-line node-id size]
            (-> (new-line indent prev-line)
                (concat-line 0 node-id size)))]
    (loop [[node-id & xs] children-ids
           indent 0
           line (empty-line padding-x padding-y)
           lines []
           acc-bounds {}]
      (if (nil? node-id)
        {:lines (conj lines line) :bounds acc-bounds}
        (let [{:keys [size bounds]} (calcurate-bounds (- max-width indent) node-id)
              gap (if (-> line :elements empty?) 0 gap-v)
              padding (* 2 padding-x)
              acc-bounds (merge acc-bounds bounds)
              next-indent (if (and indent-top (= indent 0)) (+ gap-v (:w size)) indent)]
          (cond
            (:newline? size)
            (recur xs next-indent (new-line next-indent line)
                   (conj lines (concat-line line 0 node-id size))
                   acc-bounds)
            (> (+ (:w size) (:width line) padding gap) max-width)
            (recur xs next-indent (concat-new-line next-indent line node-id size)
                   (conj lines line) acc-bounds)
            true
            (recur xs next-indent
                   (concat-line line gap node-id size) lines acc-bounds)))))))

(defn- calcurate-min-size [{:keys [padding-x padding-y min-width min-height]}]
  (l/->Size (+ min-width (* 2 padding-x))
            (+ min-height (* 2 padding-y))))

(defn calcurate-container-size [{:keys [padding-x padding-y]} lines]
  (let [width (reduce max 0 (map :width lines))
        {:keys [y height]} (last lines)]
    (l/->Size (+ width (* 2 padding-x)) (+ y height padding-y))))

(defn accumulate-bounds [bounds lines]
  (into bounds (mapcat :elements lines)))

(defmethod l/calcurate-children-bounds FlowOption
  [env option max-width node-ids]
  (if (empty? node-ids)
    {:size (calcurate-min-size option) :bounds {}}
    (let [{:keys [bounds lines]} (calcurate-wrap-layout option env max-width node-ids)
          container-size (calcurate-container-size option lines)
          bounds (accumulate-bounds bounds lines)]
      {:size container-size :bounds bounds})))


