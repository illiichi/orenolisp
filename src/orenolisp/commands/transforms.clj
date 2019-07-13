(ns orenolisp.commands.transforms
  (:require [orenolisp.model.conversion :as conv]
            [orenolisp.model.forms :as form]
            [orenolisp.model.editor :as ed]))

(defn wrap-by-map [editor]
  (let [parent-editor (-> '(map (fn [x]) [x])
                          conv/convert-sexp->editor
                          (ed/move [:root :child :right]))
        element-id (last (ed/get-ids parent-editor [:right :child]))]
    (-> editor
        (ed/add-editor :parent parent-editor)
        (ed/with-marks (fn [editor [arg-node-id]]
                         (when arg-node-id
                           (-> editor
                               (ed/jump arg-node-id)
                               (ed/swap element-id))))))))

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

(defn wrap-by-range [editor]
  (transform-sexp editor
                  (fn [sexp]
                    (list 'u/rg-lin '(lf-cub:kr 0.01) sexp sexp))))

(defn wrap-by-line [editor]
  (transform-sexp editor
                  (fn [sexp]
                    (list 'line sexp sexp 30))))
