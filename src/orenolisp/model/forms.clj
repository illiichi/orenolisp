(ns orenolisp.model.forms
  (:refer-clojure :exclude [vector]))

(defn paren [] {:type :paren})
(defn vector [] {:type :vector})
(defn new-line [] {:type :new-line})
(defn ident [text] {:type :ident :value text})
(defn in-ugen [rate exp-id] {:type :in :rate rate :exp-id exp-id})
