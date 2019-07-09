(ns orenolisp.view.controller.window-controller
  (:require [orenolisp.view.ui.component.window :as wu]
            [orenolisp.view.ui.component.viewport :as viewport]
            [orenolisp.view.layout.layout :as layout]
            [orenolisp.view.layout.layout-decision :as layout-decision]
            [orenolisp.model.editor :as ed]
            [orenolisp.util :as ut]
            [orenolisp.view.ui.expression-ui :as eu]
            [orenolisp.view.ui.fx-util :as fx]
            [clojure.set :refer [union]]))

(defrecord Size [w h])
(defrecord Position [x y])
(defrecord Layout [position size layer-no])
(defrecord Window [exp-id layout win-ui exp-table context])

(defn- put-into-viewport [{:keys [win-ui layout]}]
  (viewport/put-component (:layer-no layout) win-ui))

(defn- draw-frame [{:keys [win-ui exp-id layout]}]
  (fx/move win-ui (wu/outer-pos (:position layout)))
  (wu/draw-with-inner-size win-ui exp-id (:size layout)))

(defn new-window [exp-id layer-no x y inner-width inner-height]
  (doto (->Window exp-id (->Layout (->Position x y)
                                         (->Size inner-width inner-height)
                                         layer-no)
                  (wu/create) {} {:doing :selecting})
    (put-into-viewport)
    (draw-frame)))

(defn initial-window [exp-id]
  (let [[x y outer-width outer-height] (viewport/center-location)
        [sx sy] (wu/inner-pos x y)
        [inner-width inner-height] (wu/inner-size outer-width outer-height)]
    (new-window exp-id 0 sx sy inner-width inner-height)))

(defn focus [{:keys [layout win-ui]} current-layer-no]
  (viewport/focus current-layer-no (:layer-no layout) win-ui))


(defn- create-and-delete-ui [editor layer-no exp-table created-ids deleted-ids]
  (let [new-exps (->> created-ids
                      (map (fn [node-id]
                             (let [content (ed/get-content editor node-id)]
                               {node-id {:type (:type content)
                                         :component (eu/create-component content)
                                         :attributes {}}})))
                      (into {}))]
    (fx/run-now
     (viewport/put-components layer-no (map :component (vals new-exps)))
     (viewport/remove-components (keep (fn [[node-id m]]
                                         (when (deleted-ids node-id) (:component m)))
                                       exp-table)))
    (merge (apply dissoc exp-table deleted-ids) new-exps)))

(defn- check-and-move-component [component old-attributes new-attributes]
  (when (not= (:position old-attributes) (:position new-attributes))
    (fx/move component (:position new-attributes)))
  (not= (dissoc old-attributes :position) (dissoc new-attributes :position)))

(defn- update-node [editor bounds modified-ids node-id {:keys [attributes component] :as m}]
  (let [{:keys [x y size]} (or (get bounds node-id) (ut/error "bounds not found!" node-id))
        new-attributes (-> attributes
                           (assoc :focus? (= node-id (:current-id editor)))
                           (assoc :position {:x x :y y})
                           (assoc :size size))
        attribute-changed? (check-and-move-component component attributes new-attributes)
        result (assoc m :attributes new-attributes)]
    (when (or attribute-changed? (modified-ids node-id))
      (eu/render-form node-id result editor bounds))
    result))

(defn update-window [window prev-editor new-editor]
  (let [max-width (get-in window [:layout :size :w])
        layer-no (get-in window [:layout :layer-no])
        exp-table   (get-in window [:exp-table])
        layout-option (-> (get-in window [:layout :position])
                          (assoc :w max-width))
        new-bounds (layout/calcurate-layout layout-decision/build-size-or-option
                                            layout-option new-editor)
        {:keys [created modified deleted]} (ed/diff prev-editor new-editor)
        exp-table (create-and-delete-ui new-editor layer-no exp-table created deleted)
        new-exp-table (ut/map-kv (partial update-node new-editor new-bounds
                                          (union created modified)) exp-table)]
    ;; change window height
    (-> window
        (assoc :exp-table new-exp-table)
        (assoc-in [:context :node-type] (:type (ed/get-content new-editor))))))
