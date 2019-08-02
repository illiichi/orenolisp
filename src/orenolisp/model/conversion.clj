(ns orenolisp.model.conversion
  (:require [orenolisp.util :as u]
            [orenolisp.model.tree :as tr]
            [orenolisp.model.editor :as ed]
            [orenolisp.model.forms :as forms]
            [clojure.core.match :refer [match]]))

(defprotocol IConversion
  (sexp->node [this sexp] "return [node children-sexp]")
  (node->sexp [this node children]))

(defrecord CommonConversion []
  IConversion
  (sexp->node [this sexp]
    (cond (vector? sexp) [(forms/vector) sexp]
          (map? sexp) (throw (Exception. "map not suported"))
          (seq? sexp) [(forms/paren) sexp]
          (= '___ sexp) [(forms/input-ident) [] true]
          true [(forms/ident (pr-str sexp)) []]))
  (node->sexp [this node children]
    (case (:type node)
      :paren (apply list children)
      :vector (apply vector children)
      :map (apply array-map children)
      :newline :newline
      :ident (read-string (:value node)))))

(defrecord InConversion []
  IConversion
  (sexp->node [this sexp]
    (match sexp
           (['in (['l4/sound-bus exp-id :out-bus] :seq) 2] :seq)
           [{:type :in :rate :audio :exp-id (name exp-id)} []]
           (['in:kr (['l4/control-bus exp-id :out-bus] :seq) 1] :seq)
           [{:type :in :rate :control :exp-id (name exp-id)} []]
           _ nil))
  (node->sexp [this {:keys [rate exp-id] :as node} children]
    (when (= (:type node) :in)
      (let [[in-func bus-func num-chan] (case rate
                                 :audio ['in 'l4/sound-bus 2]
                                 :control ['in:kr 'l4/control-bus 1]
                                 (throw (Exception. (str "unknown rate: " rate))))]
        (list in-func (list bus-func (keyword exp-id) :out-bus) num-chan)))))

(defrecord GaugeConversion []
  IConversion
  (sexp->node [this sexp]
    (match sexp
           (['u/tap-line node-id from to dur exp?] :seq)
           [{:type :gauge :exp? exp? :node-id node-id} [from to dur]]
           _ nil))
  (node->sexp [this {:keys [node-id exp?] :as node} children]
    (when (= (:type node) :gauge)
      (assert (= (count children) 3) (str "wrong children for guage:" children))
      (let [[from to dur] children]
        (list 'u/tap-line node-id from to dur exp?)))))

(def conversions (seq [(->InConversion) (->GaugeConversion) (->CommonConversion)]))
(defn- find-first-non-nil [f xs]
  (reduce (fn [_ x] (when-let [y (f x)]
                      (reduced y))) nil xs))
(defn- apply-conversions-sexp->node [sexp]
  (find-first-non-nil (fn [conv] (sexp->node conv sexp)) conversions))

(defn- apply-conversions-node->sexp [node children]
  (find-first-non-nil (fn [conv] (node->sexp conv node children)) conversions))

(defn- convert-node->sexp [editor node-id]
  (let [node (ed/get-content editor node-id)
        children (->> (tr/get-children (:tree editor) node-id)
                      (map (partial convert-node->sexp editor))
                      (filter #(not= :newline %)))]
    (apply-conversions-node->sexp node children)))

(defn convert-editor->sexp
  ([{:keys [tree] :as editor}]
   (convert-editor->sexp editor (tr/find-root tree)))
  ([editor root-id]
   (convert-node->sexp editor root-id)))

(defn convert-sexp->editor
  ([sexp] (convert-sexp->editor (ed/new-editor) nil sexp))
  ([editor sexp] (convert-sexp->editor editor nil sexp))
  ([editor parent-id sexp]
   (if-let [[node children placeholder?] (apply-conversions-sexp->node sexp)]
     (let [next-editor (-> editor
                           (ed/add parent-id :child node)
                           (u/when-> placeholder? (ed/add-as-multiple-cursors)))]
       (reduce #(convert-sexp->editor %1 (ed/get-id next-editor :self) %2)
               next-editor children))
     (throw (Exception. (str "no conversion found: " sexp))))))

(defn sub-editor
  ([editor] (sub-editor editor (ed/get-id editor :self)))
  ([editor node-id]
   (-> editor
       (convert-node->sexp node-id)
       convert-sexp->editor)))
