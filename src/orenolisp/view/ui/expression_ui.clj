(ns orenolisp.view.ui.expression-ui
  (:require [orenolisp.model.editor :as ed]
            [orenolisp.view.ui.component.paren :as paren]
            [orenolisp.view.ui.component.vector :as vector-ui]
            [orenolisp.view.ui.component.in-ugen :as in-ugen]
            [orenolisp.view.ui.component.new-line :as new-line]
            [orenolisp.view.ui.component.gauge :as gauge]
            [orenolisp.view.ui.component.editable-text :as text]))

(defmulti render-form (fn [node-id {:keys [type]} editor bounds] type))
(defmethod render-form :paren [node-id {:keys [component attributes]} editor bounds]
  (paren/render component attributes))
(defmethod render-form :vector [node-id {:keys [component attributes]} editor bounds]
  (vector-ui/render component attributes))
(defmethod render-form :newline [node-id {:keys [component attributes]} editor bounds]
  (new-line/render component attributes))
(defmethod render-form :in [node-id {:keys [component attributes]} editor bounds]
  (in-ugen/render component attributes (ed/get-content editor node-id)))
(defmethod render-form :gauge [node-id {:keys [component attributes]} editor bounds]
  (let [children-bounds (->> (ed/get-children-ids editor node-id)
                             (map bounds))]
    (gauge/render component attributes (ed/get-content editor node-id) children-bounds)))
(defmethod render-form :ident [node-id {:keys [component attributes]} editor bounds]
  (text/render component attributes (ed/get-content editor node-id)))
(defmethod render-form :default [node-id {:keys [component attributes]} editor bounds]
  (println "render:" node-id component attributes))

(defmulti create-component :type)
(defmethod create-component :vector [m]
  (vector-ui/create-node))
(defmethod create-component :paren [m]
  (paren/create-node))
(defmethod create-component :newline [m]
  (new-line/create-node))
(defmethod create-component :in [m]
  (in-ugen/create-node))
(defmethod create-component :gauge [m]
  (gauge/create-node))
(defmethod create-component :ident [m]
  (text/create-node))
(defmethod create-component :default [m]
  (println "create:" m))
