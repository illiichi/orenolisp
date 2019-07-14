(ns orenolisp.sc.builder
  (:require [orenolisp.model.conversion :as conv]
            [llll.macro.defsound :as ld]
            [llll.macro.control :as lc]))

(def default-option {:swap-option {:switch-dur 8
                                   :fade-out-dur 64}
                     :period 32})

(defn- exp-id->sym [exp-id]
  (symbol exp-id))

(defn- exp-id->keyword [exp-id]
  (keyword exp-id))

(defn- build-option [sc-option]
  (merge default-option sc-option))

(defn exp->sexp [{:keys [exp-id editor sc-option]} ]
  (let [sexp (conv/convert-editor->sexp editor)]
    `(ld/defsound ~(exp-id->sym exp-id) ~(build-option sc-option)
       (let [snd# ~sexp]
         (overtone.sc.cgens.tap/tap "out" 12 (~'overtone.core/amplitude:kr snd#))
         snd#))))

(defn exp->control-vol [{:keys [exp-id]} param]
  `(lc/control ~(exp-id->keyword exp-id) :vol ~param))
