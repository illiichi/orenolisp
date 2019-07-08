(ns orenolisp.view.ui.expression-ui
  (:require [orenolisp.view.ui.component.paren :as paren]))

(defmulti render-form (fn [node-id {:keys [type]} editor bounds] type))
(defmethod render-form :paren [node-id {:keys [component attributes]} editor bounds]
  (paren/render component attributes))
(defmethod render-form :default [node-id {:keys [component attributes]} editor bounds]
  (println "render:" node-id component attributes))

(defmulti create-component :type)
(defmethod create-component :paren [m]
  (paren/create-node))
(defmethod create-component :default [m]
  (println "create:" m))
