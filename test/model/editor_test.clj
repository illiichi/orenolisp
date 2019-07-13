(ns model.editor-test
  (:require  [clojure.test :refer [deftest testing is]]
             [orenolisp.model.editor :as ed]))

(def print? true)

(deftest add-test
  (is (-> (ed/new-editor)
          (ed/add :child 1)
          (ed/add :child 2)
          (ed/add :child 3)
          (ed/move :parent)
          (ed/move :parent)
          (ed/add :child 4)
          (ed/add :left 5)
          (ed/add :parent 6)
          (ed/move-most :parent)
          (ed/add :child 7)
          (ed/move-most :left)
          (ed/add :right 8)
          (ed/move-most :parent)
          (ed/move-most :child)
          (ed/add :child 9)
          ed/print-editor))
  (is (-> (ed/new-editor)
          (ed/add :child 1)
          (ed/add :child 2)
          (ed/add :self 3)
          ed/print-editor)))

(deftest add-editor-test
  (let [editor (-> (ed/new-editor)
                   (ed/add :child 1)
                   (ed/add :child 2)
                   (ed/add :child 3)
                   (ed/move :parent)
                   (ed/move :parent)
                   (ed/add :child 4)
                   (ed/add :left 5)
                   (ed/move :left))
        editor2 (-> (ed/new-editor)
                    (ed/add :child "a")
                    (ed/add :child "b")
                    (ed/add :child "c")
                    (ed/move :parent)
                    (ed/move :parent)
                    (ed/add :child "d")
                    (ed/add :left "e"))
        editor3 (-> (ed/new-editor)
                    (ed/add :child "a"))]
    (is (-> (ed/copy editor)
            (ed/add-editor :child (ed/copy editor2))
            ed/print-editor))
    (is (-> (ed/copy editor)
            (ed/add-editor :self editor2)
            ed/print-editor))
    (is (-> (ed/copy editor)
            (ed/add-editor :parent (ed/copy editor2))
            ed/print-editor))
    (is (-> (ed/copy editor)
            (ed/move :parent)
            (ed/add-editor :parent (ed/copy editor2))
            ed/print-editor))
    (is (-> (ed/copy editor)
            (ed/add-editor :parent (ed/copy editor3))
            ed/print-editor))
    (is (-> (ed/copy editor)
            (ed/move :parent)
            (ed/add-editor :parent (ed/copy editor3))
            ed/print-editor))))

(deftest get-ids-test
  (let [editor (-> (ed/new-editor)
                   (ed/add :child 1)
                   (ed/add :child 2)
                   (ed/add :child 3)
                   (ed/move :parent)
                   (ed/move :parent)
                   (ed/add :child 4)
                   (ed/add :left 5)
                   (ed/add :parent 6)
                   (ed/move-most :parent)
                   (ed/add :child 7)
                   (ed/move-most :left)
                   (ed/add :right 8)
                   (ed/move-most :parent)
                   (ed/move-most :child)
                   (ed/add :child 9)
                   (ed/move-most :parent))]
    (ed/print-editor editor)
    (is (= (->> (ed/get-ids editor [:child :right :right :child])
                (map #(ed/get-content editor %)))
           [1 2 8 6 5]))))

(deftest update-test
  (is (-> (ed/new-editor)
          (ed/add :child 1)
          (ed/add :child 2)
          (ed/add :child 3)
          (ed/add :left 4)
          (ed/add :left 5)
          (ed/add :child 6)
          (ed/add :right 7)
          (ed/edit inc)
          (ed/move :parent)
          (ed/edit (partial + 10))
          ed/print-editor)))

(deftest delete-test
  (testing "delete node"
    (is (-> (ed/new-editor)
            (ed/add :child 1)
            (ed/add :child 2)
            (ed/add :left 3)
            (ed/add :child 4)
            (ed/add :left 5)
            (ed/add :child 6)
            (ed/add :right 7)
            (ed/delete)
            (ed/edit (partial + 10))
            (ed/move-most :parent)
            (ed/move :child)
            (ed/move :right)
            (ed/add :child 9)
            (ed/move :parent)
            (ed/move :left)
            (ed/delete)
            ed/print-editor)))
  (testing "delete root"
    (is (-> (ed/new-editor)
          (ed/add :child 1)
          (ed/add :child 2)
          (ed/add :child 3)
          (ed/add :left 4)
          (ed/add :left 5)
          (ed/add :child 6)
          (ed/move-most :parent)
          (ed/delete)
          ed/print-editor))))

(deftest raise-test
  (is (let [editor (-> (ed/new-editor)
                       (ed/add :child 1)
                       (ed/add :child 2)
                       (ed/add :child 3)
                       (ed/add :left 4)
                       (ed/add :left 5)
                       (ed/add :child 6)
                       (ed/move :parent))
            parent-id (ed/get-id editor :parent)]
        (-> editor
            (ed/transport :self parent-id)
            ed/print-editor))))

(deftest swap-test
  (let [editor (-> (ed/new-editor)
                   (ed/add :child 1)
                   (ed/add :child 2)
                   (ed/add :child 3)
                   (ed/add :left 4)
                   (ed/add :child 7)
                   (ed/move :parent)
                   (ed/add :left 5)
                   (ed/add :child 6)
                   (ed/move :parent))
        [_ id1 id2] (ed/get-ids editor [:right :child])]
    (ed/print-editor editor)
    (is (-> (ed/copy editor)
            (ed/swap id1)
            ed/print-editor))
    (is (-> (ed/copy editor)
            (ed/swap id2)
            ed/print-editor))))

(deftest diff-test
  (let [editor (-> (ed/new-editor)
                   (ed/add :child 1)
                   (ed/add :child 2)
                   (ed/add :child 3)
                   (ed/add :left 4)
                   (ed/add :left 5)
                   (ed/add :child 6))
        editor2 (-> (ed/copy editor)
                    (ed/add :child 7)
                    (ed/edit (partial + 20))
                    (ed/move :parent)
                    (ed/edit (partial + 10))
                    (ed/move-most :parent)
                    (ed/move :child)
                    (ed/move :child)
                    (ed/move :right)
                    (ed/delete))
        {:keys [created modified deleted]} (ed/diff editor editor2)]
    (is (= (map #(ed/get-content editor2 %) created)
           [27]))
    (is (= (map #(ed/get-content editor2 %) modified)
           [16]))
    (is (= (map #(ed/get-content editor %) deleted)
           [4]))))


