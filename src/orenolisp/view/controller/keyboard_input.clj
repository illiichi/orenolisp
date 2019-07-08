(ns orenolisp.view.controller.keyboard-input
  (:require [clojure.core.async :as async]))

(defn convert->display-name-for-ctrl [character]
  (letfn [(convert->char [ascii]
            {:can-type? false
             :char (char (+ (int \a) -1 ascii))})
          (not-typable [s] {:char s :can-type? false})]
    (let [ascii (int character)]
            (case ascii
              (8 9 13) (convert->char ascii)
              27 (not-typable "<esc>")
              32 (not-typable "<space>")
              127 (not-typable "<delete>")
              (if (<= ascii 26) ; Ctrl + ascii 
                (convert->char ascii)
                {:can-type? true
                 :char character})))))
(defn convert->display-name [character]
  (letfn [(convert->char [ascii]
            {:can-type? false
             :char (char (+ (int \a) -1 ascii))})
          (not-typable [s] {:char s :can-type? false})]
    (let [ascii (int character)]
            (case ascii
              13 (not-typable "<enter>")
              9 (not-typable "<tab>")
              8 (not-typable "<backspace>")
              27 (not-typable "<esc>")
              32 (not-typable "<space>")
              127 (not-typable "<delete>")
              (if (<= ascii 26) ; Ctrl + ascii 
                (convert->char ascii)
                {:can-type? true
                 :char character})))))

(defn convert-key [key]
  (let [ret (->> key .getCharacter last)
        ret (if (.isControlDown key)
              (convert->display-name-for-ctrl ret)
              (convert->display-name ret))
        ret (if (and (.isShiftDown key) (not (:can-type? ret)))
              (update ret :char clojure.string/upper-case)
              ret)
        specials (->> [[:ctrl (.isControlDown key)]
                       [:alt (.isAltDown key)]
                       [:super (.isMetaDown key)]]
                      (filter second)
                      (map first)
                      set)]
    (if (empty? specials) ret
        (assoc ret :specials specials))))

(defn- handle-key-event [key]
  {:type :keyboard :key (convert-key key)})

(def keyboard-ch (async/chan 1 (map handle-key-event)))

