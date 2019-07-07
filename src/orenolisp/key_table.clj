(ns orenolisp.key-table
  (:require [orenolisp.commands.commands :as cmd]))

(def initial-keymap
  {{:char "<space>"} cmd/open-initial-window})

(def key-table {nil {nil initial-keymap}})

(defn get-operation [{:keys [target-type node-type doing]} key tmp-keymap]
  (if tmp-keymap
    (if-let [op (get tmp-keymap key)]
      (flatten [op cmd/cancel-temporary-keymap])
      cmd/cancel-temporary-keymap)
    (or (get-in key-table [(or node-type target-type) doing key])
        (get-in key-table [target-type doing key]))))
