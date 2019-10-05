(ns orenolisp.commands.transforms
  (:require [orenolisp.model.conversion :as conv]
            [orenolisp.model.forms :as form]
            [orenolisp.model.editor :as ed]))

(defn- find-nearest-ident [editor ident-value]
  (ed/find-by-first-element editor #(ed/move % :parent)
                            #(= (:value %) ident-value)))

(defn- wrap-by-map [editor arg-node-id]
  (let [parent-editor (-> '(map (fn [___]) [___])
                          conv/convert-sexp->editor
                          (ed/move [:root :child :right]))
        element-id (last (ed/get-ids parent-editor [:right :child]))]
    (-> editor
        (ed/add-editor :parent parent-editor)
        (ed/jump arg-node-id)
        (ed/swap element-id))))

(defn- add-map-arguments [editor ugen-name]
  (let [editor-map (find-nearest-ident editor ugen-name)
        map-ident-id (ed/get-id editor-map :child)
        target-id (ed/get-id editor :self)
        end-of-arg (-> editor-map
                       (ed/move [:child :right :child :right :child :right])
                       (ed/move-most :right)
                       (ed/get-id :self))]
    (-> editor
        (ed/jump end-of-arg)
        (ed/add :right (form/input-ident))
        (ed/add-as-multiple-cursors)
        (ed/move [:parent :parent])
        (ed/move-most :right)
        (ed/add :right (form/vector))
        (ed/add :child (form/input-ident))
        (ed/add-as-multiple-cursors)
        (ed/swap target-id)
        (ed/jump map-ident-id)
        (ed/move-most :right)
        (ed/move :child))))

(defn transform-to-map [editor]
  (if (ed/has-mark? editor)
    (-> editor
        (ed/with-marks
          (fn [editor [first-node-id & rest-node-ids]]
            (-> (reduce (fn [editor node-id]
                          (-> editor
                              (ed/push-new-multiple-cursors)
                              (ed/jump node-id)
                              (add-map-arguments "map")))
                        (wrap-by-map editor first-node-id)
                        rest-node-ids))))
        (ed/reverse-multicursors))
    (add-map-arguments editor "map")))

(defn- transpose-vector [editor]
  (let [current-id (-> editor (ed/get-id :self))
        arg-vectors (->> (-> editor (ed/move :parent)
                             (ed/get-children-ids))
                         (drop-while #(not= current-id %)))
        [first-arg-ids & initial-rest-args-ids] (->> arg-vectors
                                                     (map #(ed/get-children-ids editor %)))]
    (loop [editor editor
           [first-arg-id & rest-first-ids] first-arg-ids
           rest-args-ids initial-rest-args-ids]
      (let [next-editor (-> editor
                            (ed/add first-arg-id :parent (form/vector)))
            parent-vec-id (ed/get-id next-editor :self)
            [next-editor rest-args-ids] (reduce (fn [[editor acc] [rest-arg-id & rest-ids]]
                                                  [(if rest-arg-id
                                                     (-> editor
                                                         (ed/jump rest-arg-id)
                                                         (ed/transport :child parent-vec-id))
                                                     editor)
                                                   (conj acc rest-ids)])
                                                [next-editor []] rest-args-ids)]
        (if (nil? rest-first-ids)
          (reduce ed/delete next-editor (rest arg-vectors))
          (recur next-editor rest-first-ids rest-args-ids))))))

(defn- convert-additional-t-map-argument [org-editor]
  (-> (reduce (fn [editor node-id]
                (ed/add-editor editor node-id :child (conv/sub-editor org-editor)))
              org-editor
              (ed/get-children-ids (-> org-editor (ed/move [:parent :left]))))
      (ed/delete (ed/get-id org-editor :parent))))

(defn transform-to-t-map [editor]
  (if (ed/has-mark? editor)
    (let [editor (-> (transform-to-map editor)
                     (find-nearest-ident "map")
                     (ed/move :child))
          map-id (ed/get-id editor :self)]
      (-> editor
          (ed/edit map-id #(assoc % :value "u/t-map"))
          (ed/move [:right :right])
          (transpose-vector)))

    (-> editor
        (add-map-arguments "u/t-map")
        (convert-additional-t-map-argument))))

(defn wrap-by-reduce [editor]
  (let [parent-editor (-> '(u/reduce-> (fn [acc x]) [x])
                          conv/convert-sexp->editor
                          (ed/move [:root :child :right]))
        element-id (last (ed/get-ids parent-editor [:right :child]))]
    (-> editor
        (ed/add-editor :parent parent-editor)
        (ed/with-marks (fn [editor [arg-node-id]]
                         (when arg-node-id
                           (-> editor
                               (ed/jump arg-node-id)
                               (ed/add :left (form/ident "acc"))
                               (ed/move :right)
                               (ed/swap element-id))))))))

(defn threading [editor]
  (let [parent-editor (-> (conv/convert-sexp->editor '(->))
                          (ed/move :root))
        first-arg-id (ed/get-id parent-editor :child)]
    (-> editor
        (ed/add-editor :parent parent-editor)
        (ed/move [:child :right :child :right])
        (ed/transport :right first-arg-id))))

(defn- transform-sexp [editor f]
  (let [new-editor (-> editor
                       (conv/convert-editor->sexp (ed/get-id editor :self))
                       f
                       (conv/convert-sexp->editor))]
    (ed/add-editor editor :self new-editor)))

(defn wrap-by-range [ugen]
  (fn [editor]
   (transform-sexp editor
                   (fn [sexp]
                     (list 'u/lin-lin (list ugen 1) sexp sexp)))))

(defn wrap-by-line [editor]
  (let [node-id (ed/get-id editor :self)]
    (-> editor
        (transform-sexp
         (fn [sexp]
           (list 'u/tap-line node-id sexp sexp 16 false)))
        (ed/move :parent)
        ;; fixme when node-id has been changed
        (as-> editor (ed/edit editor #(assoc % :node-id (ed/get-id editor :self)))))))

(defn- let-binding-for-new [editor]
  (let [parent-editor (-> '(let [___ ___])
                          conv/convert-sexp->editor
                          (ed/move :root))
        func-arg-id (last (ed/get-ids parent-editor [:child :right :child :right]))]
    (some-> editor
            (ed/add-editor :parent parent-editor)
            (ed/with-marks (fn [editor [arg-node-id]]
                             (when arg-node-id
                               (-> editor
                                   (ed/jump arg-node-id)
                                   (ed/swap func-arg-id)
                                   (ed/move :left))))))))

(defn- let-binding-for-add [editor]
  (when-let [editor-let (find-nearest-ident editor "let")]
    (let [end-of-binding (-> editor-let
                             (ed/move [:child :right :child])
                             (ed/move-most :right)
                             (ed/get-id :self))
          target-id (ed/get-id editor :self)]
      (-> editor
          (ed/jump end-of-binding)
          (ed/add :right (form/new-line))
          (ed/add :right (form/input-ident))
          (ed/add-as-multiple-cursors)
          (ed/add :right (form/input-ident))
          (ed/add-as-multiple-cursors)
          (ed/swap target-id)))))

(defn let-binding [editor]
  (if (ed/has-mark? editor)
    (let-binding-for-new editor)
    (let-binding-for-add editor)))

(defn append-splay-tanh [editor]
  (let [parent-editor (-> '(-> (splay) tanh)
                          conv/convert-sexp->editor
                          (ed/move :root))
        threading-node-id (ed/get-id parent-editor :child)]
    (some-> editor
            (ed/add-editor :parent parent-editor)
            (ed/move [:child :right :right :right])
            (ed/transport :right threading-node-id))))

(defn iterate-multiply [editor]
  (let [parent-editor (-> '(iterate (fn [x] (* x)))
                          conv/convert-sexp->editor
                          (ed/move :root))]
    (some-> editor
            (ed/add-editor :parent parent-editor)
            (ed/move [:child :right :child :right :right :child :right])
            (ed/add :right (form/input-ident)))))
