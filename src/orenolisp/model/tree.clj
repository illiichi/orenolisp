(ns orenolisp.model.tree
  (:require [orenolisp.util :as ut]
            [clojure.set :refer [intersection difference]])
  (:import [java.util HashMap ArrayList]))

(defrecord Tree [parent-table children-table])

(defn- new-node []
  (ArrayList.))
(defn get-children [tree node-id]
  (.get (:children-table tree) node-id))

(defn get-parent [tree node-id]
  (.get (:parent-table tree) node-id))

(defn clear [{:keys [parent-table children-table]}]
  (.clear parent-table)
  (.clear children-table))

(defn new-tree []
  (->Tree (HashMap.) (HashMap.)))

(defn copy-tree [{:keys [parent-table children-table]}]
  (let [ret (new-tree)]
    (.putAll (:parent-table ret) parent-table)
    (doseq [k (keys children-table)]
      (.put (:children-table ret) k (.clone (get children-table k))))
    ret))

(defn all-keys [tree]
  (-> tree :children-table keys set))

(defn- single-element-tree [v]
  (->Tree {} {v (new-node)}))

(defn- root-node? [{:keys [parent-table]} target-id]
  (nil? (get parent-table target-id)))

(defn find-root [{:keys [parent-table children-table]}]
  (or (->> (keys children-table)
           (filter #(nil? (get parent-table %)))
           first)
      (ut/error "no root found!")))

(defn- get-siblings [tree node-id]
  (->> (or (get-parent tree node-id)
           (ut/error "no parent:" node-id))
       (get-children tree)))

(defn- check-able-to-add-siblings [tree target-id]
  (or (nil? target-id)
      (not (root-node? tree target-id))))

(defn- empty-tree? [tree]
  (-> tree :children-table empty?))

(defn get-siblings [tree node-id]
  (->> (or (get-parent tree node-id)
           (throw (Exception. (str "no parent:" node-id))))
       (get-children tree)))

(defn- check-able-to-add-siblings [tree target-id]
  (or (nil? target-id)
      (not (root-node? tree target-id))))

(defn- add-sibling
  [dx {:keys [parent-table children-table] :as tree}
   target-id new-id]
  (let [siblings (get-siblings tree target-id)
        idx (-> siblings (.indexOf target-id))]
    (.add siblings (+ idx dx) new-id)))

(defn- list-descendants [tree node-id]
  (cons node-id (mapcat (partial list-descendants tree) (get-children tree node-id))))

(defn- remove-from-parent [tree target-id]
  (-> (get-siblings tree target-id)
      (.remove target-id)))

(defn- remove-from-table [{:keys [parent-table children-table]} ids]
  (ut/remove-key-from-hashmap children-table ids)
  (ut/remove-key-from-hashmap parent-table ids)
  ids)

(defn- assert-independent? [a-tree b-tree]
  (let [duplicated-keys (intersection (set (keys (:children-table a-tree)))
                                      (set (keys (:children-table b-tree))))]
    (assert (empty? duplicated-keys)
            (str "duplicated keys found: " duplicated-keys))))

(defn- add-tree [{:keys [parent-table children-table] :as tree} target-id direction new-tree]
  (if (empty-tree? tree)
    (do (assert (nil? target-id)
                (str "tree is empty, but target-id is specified:" target-id))
        (.putAll children-table (:children-table new-tree))
        (.putAll parent-table (:parent-table new-tree))
        #{})

    (do
      (assert (get children-table target-id) (str "target-id not found:" target-id))
      (assert-independent? tree new-tree)
      (ut/merge-hashmap children-table (:children-table new-tree))
      (ut/merge-hashmap parent-table (:parent-table new-tree))
      (let [new-root-id (find-root new-tree)]
        (case direction
          :child (do (.put parent-table new-root-id target-id)
                     (.add (get-children tree target-id) new-root-id)
                     #{})
          :left (do (.put parent-table new-root-id (get-parent tree target-id))
                    (add-sibling 0 tree target-id new-root-id)
                    #{})
          :right (do (.put parent-table new-root-id (get-parent tree target-id))
                     (add-sibling 1 tree target-id new-root-id)
                     #{})
          :parent (ut/error "only single node could be added to parent:"
                            {:target-id target-id :new-tree new-tree})
          (ut/error "unknown direction:" direction {:target-id target-id
                                                    :new-tree new-tree}))))))

(defn- replace-tree [{:keys [parent-table children-table] :as tree} target-id new-tree]
  (assert (get children-table target-id) (str "target-id not found:" target-id))
  (if (root-node? tree target-id)
    (let [old-keys (all-keys tree)]
      (clear tree)
      (add-tree tree nil :child new-tree)
      old-keys)

    (let [descendants (->> (list-descendants tree target-id) set)
          new-root-id (find-root new-tree)]
      (ut/merge-hashmap children-table (:children-table new-tree))
      (ut/merge-hashmap parent-table (:parent-table new-tree))
      (ut/replace-element (get-siblings tree target-id) target-id new-root-id)
      (.put parent-table new-root-id (.get parent-table target-id))
      (difference (remove-from-table tree descendants)
                  (all-keys new-tree)))))

(defn add
  [{:keys [parent-table children-table] :as tree} target-id direction new-tree]
  (case direction
    :self (replace-tree tree target-id new-tree)
    (add-tree tree target-id direction new-tree)))

(defn has? [{:keys [children-table]} node-id]
  (ut/has-key? children-table node-id))

(defn add-parent [{:keys [parent-table children-table] :as tree} target-id
                  sub-tree attach-id]
  (assert (has? tree target-id) (str "target-id not found:" target-id
                                     " from " (keys (:parent-table tree))
                                     " or " (keys (:children-table tree))))
  (assert (has? sub-tree attach-id))
  (assert-independent? tree sub-tree)
  (let [parent-id (get-parent tree target-id)
        root-of-sub-tree (find-root sub-tree)]
    (ut/merge-hashmap children-table (:children-table sub-tree))
    (ut/merge-hashmap parent-table (:parent-table sub-tree))
    (when (not (root-node? tree target-id))
      (ut/replace-element (get-children tree parent-id) target-id root-of-sub-tree))
    (.put parent-table root-of-sub-tree parent-id)
    (.add (get-children tree attach-id) target-id)
    (.put parent-table target-id attach-id)
    #{}))

(defn add-node
  [{:keys [parent-table children-table] :as tree}
   target-id direction new-id]
  (case direction
    :parent (add-parent tree target-id (single-element-tree new-id) new-id)
    (add tree target-id direction (single-element-tree new-id))))

(defn delete
  [{:keys [parent-table children-table] :as tree} target-id]
  (let [descendants (->> (list-descendants tree target-id) set)]
    (when (get-parent tree target-id)
      (remove-from-parent tree target-id))
    (remove-from-table tree descendants)))

(defn- subset [tree target-id]
  (let [descendants (->> (list-descendants tree target-id) set)]
    (->Tree (doto (ut/extract-from-hashmap (:parent-table tree) descendants)
              (.put target-id nil))
            (ut/extract-from-hashmap (:children-table tree) descendants))))

(defn transport
  [tree source-id target-id direction]
  (let [sub-tree (subset tree source-id)]
    (delete tree source-id)
    (add tree target-id direction sub-tree)))

(defn- replace-node [{:keys [parent-table children-table] :as tree}
                     parent-id target-id new-id]
  (when parent-id
    (ut/replace-element (get-children tree parent-id) target-id new-id))
  (.put parent-table new-id parent-id))

(defn swap [tree source-id target-id]
  (if (= (get-parent tree source-id) (get-parent tree target-id))
    (ut/swap-element (get-siblings tree source-id) source-id target-id)
    (let [parent-src (get-parent tree source-id)
          parent-tar (get-parent tree target-id)]
      (replace-node tree parent-src source-id target-id)
      (replace-node tree parent-tar target-id source-id)))
  #{})

(defn pre-walk [{:keys [parent-table children-table] :as tree} acc f]
  (loop [acc acc
         [x & xs] []
         children-stack [(get-children tree (find-root tree))]]
    (if x
      (recur (f acc x) xs (cons (get-children tree x) children-stack))
      (if (empty? children-stack)
        acc
        (recur acc (first children-stack) (rest children-stack))))))


