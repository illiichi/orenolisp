(ns orenolisp.model.forms
  (:require [orenolisp.commands.text-commands :as tx])
  (:refer-clojure :exclude [vector]))

(defn paren [] {:type :paren})
(defn vector [] {:type :vector})
(defn new-line [] {:type :newline})
(defn ident [text] {:type :ident :value text})
(defn input-ident [] (tx/open-editor ""))
(defn in-ugen [rate exp-id] {:type :in :rate rate :exp-id exp-id})
