(ns orenolisp.model.editor
  (:require [orenolisp.model.tree :as tr]
            [clojure.set :refer [difference intersection]]
            [orenolisp.util :as ut])
  (:import [java.util.concurrent.atomic AtomicInteger]
           [java.util ArrayList]))

(defrecord Node [content attributes])
(defrecord Editor [current-id tree table])

(defn new-editor []
  (->Editor nil (tr/new-tree) {}))

(defn get-id [{:keys [current-id tree]} direction]
  (case direction
    :parent (tr/get-parent tree current-id)
    :child (first (tr/get-children tree current-id))
    :left (ut/find-prev current-id (tr/get-siblings tree current-id))
    :right (ut/find-next current-id (tr/get-siblings tree current-id))
    :self current-id))

(defn move [{:keys [current-id tree] :as editor} direction]
  (if (sequential? direction)
    (reduce move editor direction)
    (assoc editor :current-id (or (get-id editor direction) current-id))))

(defn get-ids [editor directions]
  (->> directions
       (reductions move editor)
       (map :current-id)))

(defn- drill-down [tree node-id]
  (let [xs (tr/get-children tree node-id)]
    (if (empty? xs)
      node-id
      (drill-down tree (last xs)))))

(defn move-most [{:keys [current-id tree] :as editor} direction]
  (let [next-id (case direction
                  :parent (tr/find-root tree)
                  :child (drill-down tree current-id)
                  :left (first (tr/get-siblings tree current-id))
                  :right (last (tr/get-siblings tree current-id))
                  (ut/error "unknown direction:" direction {:current-id current-id}))]
    (assoc editor :current-id (or next-id current-id))))
(defn jump [{:keys [table] :as editor} node-id]
  (assert (get table node-id) (str "no value found:" node-id))
  (assoc editor :current-id node-id))

(def id-counter (AtomicInteger.))

(def generate-new-id (ut/generate-counter))

(defn get-content
  ([editor] (get-content (:current-id editor)))
  ([{:keys [current-id table]} node-id]
   (some-> (get table node-id) :content)))
(defn get-attributes
  ([editor] (get-attributes editor (:current-id editor)))
  ([{:keys [table]} target-id] (some-> (get table target-id) :attributes)))

(defn set-attributes
  ([editor target-id attrs]
   (assoc-in editor [:table target-id :attributes] attrs)))

(defn root? [{:keys [current-id tree]}]
  (not (tr/get-parent tree current-id)))

(defn top-of-child? [{:keys [current-id tree]}]
  (= (.indexOf (tr/get-siblings tree current-id) current-id) 0))
(defn end-of-child? [{:keys [current-id tree]}]
  (let [siblings (tr/get-siblings tree current-id)]
    (= (.indexOf siblings current-id) (dec (count siblings)))))

(defn add
  ([{:keys [current-id] :as editor} direction value]
   (add editor current-id direction value))
  ([{:keys [tree] :as editor} target-id direction value]
   (let [new-id (generate-new-id)]
     (tr/add-node tree target-id direction new-id)
     (-> editor
         (assoc :current-id new-id)
         (update :table assoc new-id (->Node value nil))
         (ut/when-> (= direction :self)
                    (update :table dissoc target-id))))))

(defn- apply-tree-operation [editor f]
  (let [deleted-ids (f)]
    (-> editor
        (update :table (fn [table] (apply dissoc table deleted-ids))))))

(defn transport [{:keys [current-id tree] :as editor} direction target-id]
  (-> editor
      (apply-tree-operation #(tr/transport tree current-id target-id direction))))

(defn edit [{:keys [current-id] :as editor} f]
  (update editor :table update-in [current-id :content] f))

(defn delete [{:keys [current-id tree] :as editor}]
  (if (root? editor)
    (new-editor)
    (let [next-id (let [siblings (tr/get-siblings tree current-id)]
                    (if (empty? siblings)
                      (tr/get-parent tree current-id)
                      (or (ut/find-next current-id siblings)
                          (ut/find-prev current-id siblings))))]
      (-> editor
          (apply-tree-operation #(tr/delete tree current-id))
          (assoc :current-id next-id)))))


(defn copy [editor]
  (update editor :tree tr/copy-tree))

(defn diff [{old-table :table} {new-table :table}]
  (let [old-keys (set (keys old-table))
        new-keys (set (keys new-table))]
    {:created (difference new-keys old-keys)
     :modified (->> (intersection old-keys new-keys)
                    (filter (fn [k] (not= (get old-table k)
                                          (get new-table k))))
                    (set))
     :deleted (difference old-keys new-keys)}))

(defn- check-tree [{:keys [parent ids printer] :as option}
                   indent
                   {:keys [current-id table tree] :as editor} node-id]
  (assert node-id (str "node-id is nil" parent ids))
  (when (not= parent :root)
    (assert (tr/get-parent tree node-id) (str "no parent found: " node-id))
    (assert (= (tr/get-parent tree node-id) parent)
            (str "different parent:"
                 (tr/get-parent tree node-id) " and " parent)))
  (.add ids node-id)
  (assert (apply distinct? ids) (str "duplicated id found:" node-id))
  (when printer
    (println (format "%04d%s:" node-id (if (= node-id current-id) "*" " ")) indent
             (printer node-id (get-content editor node-id))))
  (doseq [c (tr/get-children tree node-id)]
    (check-tree (-> option (assoc :parent node-id))
                (str indent "  ")
                editor c)))

(defn check-consistency [printer {:keys [tree table] :as editor}]
  (if (empty? table)
    (do (when printer (println "(empty)"))
        true)

    (let [ids (ArrayList.)]
      (check-tree {:printer printer :parent :root :ids ids}
                  "" editor (tr/find-root tree))
      (or (= (set (keys table)) (set ids))
          (throw (Exception. (str "wrong key set\n"
                                  "HashMap: " (sort (keys table)) "\n"
                                  "Tree   : " (sort ids))))))))

(defn print-editor [editor]
  (check-consistency (fn [_ content] content) editor))

