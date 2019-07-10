(ns orenolisp.commands.text-commands)

(defprotocol IEdit
  (insert-char [this c])
  (delete-char [this])
  (delete-backward [this])
  (forward-char [this])
  (backward-char [this])
  (jump-head [this])
  (jump-tail [this])
  (finish [this]))

(defrecord TextEditor [type value position]
  IEdit
  (insert-char [this c]
    (let [pre  (subs value 0 position)
          post (subs value position)]
      (assoc this
             :value (str pre c post)
             :position (+ position (count c)))))
  (delete-char [this]
    (let [pre  (subs value 0 position)
          post (subs value (min (count value) (inc position)))]
      (assoc this
             :value (str pre post)
             :position position)))
  (delete-backward [this]
    (let [new-position (max 0 (dec position))
          pre  (subs value 0 new-position)
          post (subs value position)]
      (assoc this
             :value (str pre post)
             :position new-position)))
  (forward-char [this]
    (update this :position #(min (count value) (inc %))))
  (backward-char [this]
    (update this :position #(max 0 (dec %))))
  (jump-head [this]
    (assoc this :position 0))
  (jump-tail [this]
    (assoc this :position (count value)))
  (finish [this]
    (dissoc this :position)))

(defn open-editor [text]
  (->TextEditor :ident text (count text)))
