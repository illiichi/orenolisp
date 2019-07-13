(ns orenolisp.util
  (:import [java.util HashMap ArrayList]
           [java.util.concurrent.atomic AtomicInteger]))


(defn error [message & args]
  (throw (RuntimeException. (str message " " (apply prn-str args)))))

(defn generate-counter []
  (let [counter (AtomicInteger.)]
    (fn []
      (.incrementAndGet counter))))

(defn uuid [] (.toString (java.util.UUID/randomUUID)))

(defmacro when-> [x cond body]
  `(if ~cond (-> ~x ~body) ~x))

(defn has-key? [m k]
  (boolean ((set (keys m)) k)))

(defn map-kv [f m]
  (reduce-kv (fn [acc k v] (assoc acc k (f k v)))
             {} m))
(defn map-value [f m]
  (reduce-kv (fn [acc k v] (assoc acc k (f v)))
             {} m))

(defn find-next-by [pred xs]
  (loop [[x & xs] xs]
    (cond (pred x) (first xs)
          (nil? x) (throw (Exception. "not found"))
          true (recur xs))))
(defn find-next [x xs]
  (try (find-next-by #(= x %) xs)
       (catch Exception e (throw (Exception. (str "not found: " x))))))

(defn find-prev-by [pred xs]
  (loop [[x1 x2 & xs] (cons nil xs)]
    (cond (pred x2) x1
          (nil? x2) (throw (Exception. "not found"))
          true (recur (cons x2 xs)))))
(defn find-prev [x xs]
  (try (find-prev-by #(= x %) xs)
       (catch Exception e (throw (Exception. (str "not found: " x))))))

(defn remove-key-from-hashmap [^HashMap m pred]
  (let [it (-> m (.keySet) (.iterator))]
    (while (.hasNext it)
      (when (pred (.next it))
        (.remove it)))
    m))

(defn extract-from-hashmap [^HashMap m target-keys]
  (let [ret (HashMap.)]
    (doseq [k target-keys]
      (.put ret k (.get m k)))
    ret))

(defn merge-hashmap [^HashMap xs ys]
  (let [num-before (.size xs)]
    (doseq [k (keys ys)]
      (.put xs k (get ys k)))
    (assert (= (+ num-before (.size ys)) (.size xs))
            (str "contains duplicated key: " (keys ys))))
  xs)

(defn replace-element [^ArrayList arr-list old new]
  (let [idx (or (.indexOf arr-list old)
                (throw (Exception. (str "element not found: " old))))]
    (.set arr-list idx new)
    arr-list))

(defn swap-element [^ArrayList arr-list x y]
  (let [idx-x (or (.indexOf arr-list x)
                  (throw (Exception. (str "element not found: " x))))
        idx-y (or (.indexOf arr-list y)
                  (throw (Exception. (str "element not found: " y))))]
    (.set arr-list idx-x y)
    (.set arr-list idx-y x)
    arr-list))
