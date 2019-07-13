(ns orenolisp.commands.transforms
  (:require [orenolisp.model.conversion :as conv]
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
