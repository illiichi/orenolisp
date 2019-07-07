(ns orenolisp.view.layout.layout
  (:require [orenolisp.model.tree :as tr]
            [orenolisp.model.editor :as ed]))

(defrecord Size [w h newline?])
(defn ->Size [w h]
  (Size. w h false))
(defn ->Size-newline [w h]
  (Size. w h true))

(defrecord Bound [x y size]
  Object
  (toString [this] (str [x y (:w size) (:h size)])))
(defrecord LayoutEnv [calcurate-bounds])

(defmulti calcurate-children-bounds
  (fn [layout-env layout-option max-width node-ids] (type layout-option)))

(defn- get-item [table key]
  (or (get table key)
      (throw (Exception. (str "key not found: " key " from: " (keys table))))))

(defn convert-from-local-coord [bounds tree top-x top-y node-id]
  (let [bound (-> (get-item bounds node-id)
                  (update :x (partial + top-x))
                  (update :y (partial + top-y)))
        children-bounds (->> (tr/get-children tree node-id)
                             (map (partial convert-from-local-coord
                                           bounds tree (:x bound) (:y bound))))]
    (into {node-id bound} children-bounds)))

(defn calcurate-layout [decide-layout parent-max-width {:keys [tree] :as editor}]
  (letfn [(calcurate-bounds [max-width node-id]
            (let [option-or-size (decide-layout (ed/get-content editor node-id))]
              (if (instance? Size option-or-size)
                {:size option-or-size :bounds {}}

                (let [children (tr/get-children tree node-id)
                      layout-env (->LayoutEnv calcurate-bounds)]
                  (calcurate-children-bounds layout-env
                                             option-or-size
                                             max-width
                                             children)))))]
    (let [root-node-id (tr/find-root tree)
          {:keys [size bounds]} (calcurate-bounds parent-max-width root-node-id)]
      (-> bounds
          (assoc root-node-id (->Bound 0 0 size))
          (convert-from-local-coord tree 0 0 root-node-id)))))

