(ns orenolisp.key-table
  (:require [orenolisp.model.forms :as form]
            [orenolisp.model.editor :as ed]
            [orenolisp.commands.commands :as cmd]
            [orenolisp.commands.text-commands :as tx]))

(def initial-keymap
  {{:char "<space>"} [cmd/open-initial-window
                      (cmd/add :child (form/input-ident))
                      cmd/switch-to-typing-mode]
   {:char \a :specials #{:ctrl :alt}} [cmd/switch-to-selecting-mode
                                       (cmd/move-most :parent)]})

(def global-keymap {})
(def node-selecting-keymap
  (merge
   global-keymap
   {{:char \j} (cmd/move :right)
    {:char \k} (cmd/move :left)
    {:char \l} (cmd/move :parent)
    {:char \[} (cmd/move :parent)
    {:char \a :specials #{:ctrl :alt}} (cmd/move-most :parent)
    {:char \d :specials #{:ctrl}} (cmd/delete)
    {:char \r} (cmd/raise)
    {:char \}}         (cmd/add-with-keep-position :parent (form/vector)) ; anim
    {:char \(}         (cmd/add-with-keep-position :parent (form/paren))  ; anim
    {:char "<space>"}  [(cmd/add :right (form/input-ident))
                        cmd/switch-to-typing-mode]
    {:char "<SPACE>"}  [(cmd/add :left (form/input-ident))
                        cmd/switch-to-typing-mode]
    {:char "<enter>"}  (cmd/add-with-keep-position :right (form/new-line))}))

(def paren-selecting-keymap
  (merge
   global-keymap
   node-selecting-keymap
   {{:char \a} (cmd/window-command #(ed/move % :child))
    {:char \e} (cmd/window-command #(-> % (ed/move :child) (ed/move-most :right)))
    {:char \i :specials #{:ctrl}} [(cmd/add :child (form/input-ident))
                                   cmd/switch-to-typing-mode]
    {:char \s} (cmd/window-command #(-> % (ed/move :child) (ed/move :right)))}))

(def ident-selecting-keymap
  (merge
   global-keymap
   node-selecting-keymap
   {{:char \i} [(cmd/edit #(tx/open-editor (:value %)))
                cmd/switch-to-typing-mode]}))

(def ident-typing-keymap
  (merge
   global-keymap
   {{:char \f :specials #{:ctrl}} (cmd/edit tx/forward-char)
    {:char \b :specials #{:ctrl}} (cmd/edit tx/backward-char)
    {:char \d :specials #{:ctrl}} (cmd/edit tx/delete-char)
    {:char \h :specials #{:ctrl}} (cmd/edit tx/delete-backward)
    {:char "<delete>"}            (cmd/edit tx/delete-char)
    {:char "<backspace>"}         (cmd/edit tx/delete-backward)
    {:char \a :specials #{:ctrl}} (cmd/edit tx/jump-head)
    {:char \e :specials #{:ctrl}} (cmd/edit tx/jump-tail)
    {:char "<esc>"}  [(cmd/edit tx/finish)
                      cmd/switch-to-selecting-mode]
    {:char \[}       [(cmd/edit tx/finish)
                      cmd/switch-to-selecting-mode]
    {:char "<space>"} [(cmd/edit tx/finish)
                       (cmd/add :right (form/input-ident))]}))

(def in-ugen-selecting-keymap
  (merge
   global-keymap
   node-selecting-keymap
   {}))

(def key-table {:in    {:selecting in-ugen-selecting-keymap}
                :newline {:selecting node-selecting-keymap}
                :paren {:selecting paren-selecting-keymap}
                :vector {:selecting paren-selecting-keymap}
                :ident {:selecting ident-selecting-keymap
                        :typing    ident-typing-keymap}
                nil {nil initial-keymap}})

(defn get-operation [{:keys [target-type node-type doing]} key tmp-keymap]
  (if tmp-keymap
    (if-let [op (get tmp-keymap key)]
      (flatten [op cmd/cancel-temporary-keymap])
      cmd/cancel-temporary-keymap)

    (or (get-in key-table [(or node-type target-type) doing key])
        (get-in key-table [target-type doing key])
        (get initial-keymap key))))
