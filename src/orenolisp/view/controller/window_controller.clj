(ns orenolisp.view.controller.window-controller
  (:require [orenolisp.view.ui.component.window :as wu]
            [orenolisp.view.ui.component.viewport :as viewport]
            [orenolisp.view.layout.layout :as layout]
            [orenolisp.view.layout.layout-decision :as layout-decision]
            [orenolisp.view.ui.component.animations :as anim]
            [orenolisp.model.editor :as ed]
            [orenolisp.util :as ut]
            [orenolisp.view.ui.expression-ui :as eu]
            [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.watcher.engine :as we]
            [clojure.set :refer [union]]))

(defrecord Size [w h])
(defrecord Position [x y])
(defrecord Layout [position size layer-no])
(defn ->layout [layer-no x y w h]
  (->Layout (->Position x y) (->Size w h) layer-no))
(defrecord Window [exp-id layout win-ui exp-table context])

(defn get-frame-ui [{:keys [win-ui]}] win-ui)

(defn- put-into-viewport [{:keys [win-ui layout]}]
  (.play (anim/white-in 150 win-ui))
  (viewport/put-component (:layer-no layout) win-ui))

(defn- draw-frame [{:keys [win-ui exp-id layout]}]
  (fx/move win-ui (wu/outer-pos (:position layout)))
  (wu/draw-with-inner-size win-ui exp-id (:size layout)))

(defn update-window-size [window new-size]
  (doto (assoc-in window [:layout :size] new-size)
    (draw-frame)))

(defn new-window [exp-id inner-layout]
  (doto (->Window exp-id inner-layout
                  (wu/create) {} {:doing :selecting :modified? true})
    (put-into-viewport)
    (draw-frame)))

(defn focus [{:keys [layout win-ui]} current-layer-no]
  (viewport/focus current-layer-no (:layer-no layout) win-ui))

(defn convert-to-inner-layout [layout]
  (-> layout
      (update :position wu/inner-pos)
      (update :size wu/inner-size)))

(defn open-new-window [exp-id current-layer-no new-layout]
  (let [window (new-window exp-id new-layout)]
    (focus window current-layer-no)
    window))

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
  (let [{:keys [x y size]} (or (get bounds node-id) (ut/error "bounds not found!" node-id bounds))
        new-attributes (-> attributes
                           (assoc :focus? (ed/focus? editor node-id))
                           (assoc :mark? (ed/marked? editor node-id))
                           (assoc :position {:x x :y y})
                           (assoc :size size))
        attribute-changed? (check-and-move-component component attributes new-attributes)
        result (assoc m :attributes new-attributes)]
    (when (or attribute-changed? (modified-ids node-id))
      (fx/run-now (eu/render-form node-id result editor bounds)))
    result))

(defn layout
  "fixme: almost same as update-window"
  ([editor window width] (layout editor window width false))
  ([editor window width fit-height?]
   (let [org-height (get-in window [:layout :size :h])
         exp-table   (get-in window [:exp-table])
         layout-option (-> (get-in window [:layout :position])
                           (assoc :w width))
         new-bounds (layout/calcurate-layout layout-decision/build-size-or-option
                                             layout-option editor)
         new-exp-table (ut/map-kv (partial update-node editor new-bounds
                                           #{}) exp-table)
         root-id (ed/get-id editor :root)
         inner-size (-> (:size (get new-bounds root-id))
                        (update :w #(max % width))
                        (update :h #(if fit-height? %
                                        (max % org-height))))]
     (-> window
         (update-window-size inner-size)
         (assoc :exp-table new-exp-table)))))

(defn- check-modified? [diff]
  (not (every? empty? (vals diff))))

(defn- add-animation [uis]
  (doseq [ui uis]
    (.play (anim/enphasize ui))))

(defn update-window [window prev-editor new-editor]
  (let [max-width (get-in window [:layout :size :w])
        org-height (get-in window [:layout :size :h])
        layer-no (get-in window [:layout :layer-no])
        exp-id (:exp-id window)
        exp-table   (get-in window [:exp-table])
        layout-option (-> (get-in window [:layout :position])
                          (assoc :w max-width))
        new-bounds (layout/calcurate-layout layout-decision/build-size-or-option
                                            layout-option new-editor)
        {:keys [created modified deleted] :as diff} (ed/diff prev-editor new-editor)
        modified? (check-modified? diff)
        exp-table (create-and-delete-ui new-editor layer-no exp-table created deleted)
        new-exp-table (ut/map-kv (partial update-node new-editor new-bounds
                                          (union created modified)) exp-table)
        root-id (ed/get-id new-editor :root)
        inner-size (-> (:size (get new-bounds root-id))
                       (update :w #(max % max-width))
                       (update :h #(max % org-height)))]
    (we/unregister-all exp-id deleted)
    (add-animation (->> (union created modified)
                        (map new-exp-table)
                        (map :component)))
    (-> window
        (update-window-size inner-size)
        (update :watcher-gens #(apply dissoc % deleted))
        (assoc :exp-table new-exp-table)
        (update-in [:context :modified?] #(or % modified?))
        (assoc-in [:context :node-type] (:type (ed/get-content new-editor))))))

(defn- delete-forms [{:keys [exp-table]} ids]
  (let [components (->> ids (keep exp-table) (map :component))]
    (when-let [c (first components)]
      (fx/run-now
       (viewport/remove-components-quick (.getParent c) components)))))

(defn refresh [window {:keys [editor]}]
  (let [ids (ed/all-node-ids editor)]
    (delete-forms window ids)
    (update-window window (ed/new-editor) editor)))

(defn- make-bounds-of-children [window editor node-id]
  (let [children-id (ed/get-children-ids editor node-id)]
    (reduce (fn [acc id]
              (let [size (get-in window [:exp-table id :attributes :size])]
                (assoc acc id {:size size})))
            {}
            children-id)))

(defn update-node-attributes [window editor node-id f]
  (let [temporary-bounds (make-bounds-of-children window editor node-id)
                                        ; ad-hoc implementation for gauge
        m (-> (get-in window [:exp-table node-id])
              (update :attributes f))]
    (fx/run-now (eu/render-form node-id m editor temporary-bounds))
    (assoc window [:exp-table node-id] m)))
