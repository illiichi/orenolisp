(ns model.expression-test
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
          ed/print-editor)))

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
  (is (let [editor (ed/new-editor)
            editor (-> editor
                       (ed/add :child 1)
                       (ed/add :child 2)
                       (ed/add :child 3)
                       (ed/add :left 4)
                       (ed/add :left 5)
                       (ed/add :child 6))
            parent-id (ed/get-id editor :parent)]
        (-> editor
            (ed/transport :self parent-id)
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


