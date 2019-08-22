(ns orenolisp.sc.builder
  (:require [orenolisp.model.conversion :as conv]
            [llll.macro.defsound :as ld]
            [llll.macro.control :as lc]))

(def default-option {:swap-option {:switch-dur 8
                                   :fade-out-dur 64}
                     :period 8})

(defn- exp-id->sym [exp-id]
  (symbol exp-id))

(defn- exp-id->keyword [exp-id]
  (keyword exp-id))

(defn- build-option [sc-option]
  (merge default-option sc-option))

(defn exp->sexp [{:keys [exp-id editor sc-option]} ]
  (let [sexp (conv/convert-editor->sexp editor)]
    `(ld/defsound ~(exp-id->sym exp-id) ~(build-option sc-option)
       ~(case (:rate sc-option)
          :audio
          `(let [snd# ~sexp]
             (overtone.sc.cgens.tap/tap "out" 12 (~'a2k (~'overtone.core/amplitude:ar snd#)))
             snd#)
          :control
          `(let [snd# ~sexp]
             (overtone.sc.cgens.tap/tap "out" 12 snd#)
             snd#)))))

(defn exp->control-vol [{:keys [exp-id]} param]
  `(lc/control ~(exp-id->keyword exp-id) :vol ~param))
