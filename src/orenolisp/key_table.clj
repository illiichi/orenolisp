(ns orenolisp.key-table
  (:require [orenolisp.util :as ut]
            [orenolisp.model.forms :as form]
            [orenolisp.model.editor :as ed]
            [orenolisp.view.ui.component.animations :as anim]
            [orenolisp.commands.commands :as cmd]
            [orenolisp.commands.text-commands :as tx]
            [orenolisp.commands.transforms :as trans]
            [orenolisp.watcher.watchers :as watchers]))

(def clear-keymap
  {{:char \r} cmd/refresh
   {:char \c} cmd/stop-sound})

(def initial-keymap
  {{:char "<space>"} [cmd/open-new-window
                      (cmd/add :child (form/input-ident))
                      cmd/switch-to-typing-mode]
   {:char \a :specials #{:ctrl :alt}} [cmd/switch-to-selecting-mode
                                       (cmd/move-most :parent)]})

(def layer-keymap
  {{:char \n} [cmd/open-new-window
               (cmd/add :child (form/input-ident))
               cmd/switch-to-typing-mode]
   {:char \w} (cmd/widen-window 100)
   {:char \W} (cmd/widen-window -100)
   {:char \h} cmd/fit-window-height})

(def global-keymap
  {{:char \h :specials #{:alt}} cmd/evaluate
   {:char \h :specials #{:alt :ctrl}} cmd/evaluate-all-modified
   {:char \l :specials #{:super}} (cmd/set-temporary-keymap "layer"
                                                            layer-keymap)
   {:char \c :specials #{:ctrl}} (cmd/set-temporary-keymap "clear"
                                                           clear-keymap)})
(def node-selecting-keymap
  (merge
   global-keymap
   {{:char \j :specials #{:ctrl :alt}} (cmd/move-window ut/find-next)
    {:char \k :specials #{:ctrl :alt}} (cmd/move-window ut/find-prev)
    {:char \j} (cmd/move :right)
    {:char \k} (cmd/move :left)
    {:char \l} (cmd/move :parent)
    {:char \[} (cmd/move :parent)
    {:char \J} (cmd/swap :right)
    {:char \K} (cmd/swap :left)
    {:char \a :specials #{:ctrl :alt}} (cmd/move-most :parent)
    {:char \d :specials #{:ctrl}} (cmd/delete)
    {:char "<space>" :specials #{:ctrl}} (cmd/window-command ed/toggle-mark)
    {:char \r} (cmd/raise)
    {:char \<} (cmd/slurp)
    {:char \>} (cmd/burf)
    {:char \}}         [(cmd/excursion #(ed/add % :parent (form/vector)))
                        (cmd/animate :parent anim/zoom-in)]
    {:char \(}         [(cmd/excursion #(ed/add % :parent (form/paren)))
                        (cmd/animate :parent anim/zoom-in)]
    {:char "<space>"}  [(cmd/add :right (form/input-ident))
                        cmd/switch-to-typing-mode]
    {:char "<SPACE>"}  [(cmd/add :left (form/input-ident))
                        cmd/switch-to-typing-mode]
    {:char "<space>" :specials #{:super}} (cmd/duplicate)
    {:char "<enter>"}  (cmd/excursion #(ed/add % :right (form/new-line)))}))

(def candidate-table
  [["sin-osc" "saw" "pulse" "blip"]
   ["white-noise" "pink-noise" "brown-noise" "gray-noise"]
   ["lf-cub:kr" "lf-pulse:kr" "lf-saw:kr" "lf-tri:kr"]
   ["lf-cub" "lf-pulse" "lf-saw" "lf-tri"]
   ["lf-noise0" "lf-noise1" "lf-noise2"]
   ["u/rg-lin" "u/rg-exp"]
   ["line" "x-line" "line:kr" "x-line:kr"]
   ["u/sin-r" "u/sin-rex"]
   ["lpf" "hpf" "rlpf" "rhpf"]])

(defn- next-candidate [current]
  (if-let [xs (->> candidate-table (filter #((set %) current)) first)]
    (->> (cycle xs) (drop-while #(not= current %)) rest first)
    current))

(def extraction-keymap
  {{:char \a} (cmd/extract-as-in-ugen :audio)
   {:char \k} (cmd/extract-as-in-ugen :control)})
(def transformation-keymap
  {{:char \m} [(cmd/window-command trans/wrap-by-map)
               cmd/switch-to-typing-mode]
   {:char \r} (cmd/window-command trans/wrap-by-reduce)
   {:char \t} (cmd/window-command trans/threading)
   {:char \s} (cmd/window-command trans/append-splay-tanh)
   {:char \b} [(cmd/window-command trans/let-binding)
               cmd/switch-to-typing-mode]})

(def transformation-ident-keymap
  {{:char \r} (cmd/window-command trans/wrap-by-range)
   {:char \l} [(cmd/window-command trans/wrap-by-line)
               (cmd/register-watcher watchers/create-gauge-watcher)]})

(def paren-selecting-keymap
  (merge
   global-keymap
   node-selecting-keymap
   {{:char \a} (cmd/window-command #(ed/move % :child))
    {:char \e} (cmd/window-command #(-> % (ed/move :child) (ed/move-most :right)))
    {:char \e :specials #{:alt}} (cmd/set-temporary-keymap "extraction"
                                                           extraction-keymap)
    {:char \t :specials #{:alt}} (cmd/set-temporary-keymap "transformation"
                                                           transformation-keymap)
    {:char \n :specials #{:alt}} (cmd/excursion
                                   (fn [editor]
                                     (-> editor
                                         (ed/move :child)
                                         (ed/edit #(update % :value next-candidate)))))
    {:char \i :specials #{:ctrl}} [(cmd/add :child (form/input-ident))
                                   cmd/switch-to-typing-mode]
    {:char \s} (cmd/window-command #(-> % (ed/move :child) (ed/move :right)))}))

(def ident-selecting-keymap
  (merge
   global-keymap
   node-selecting-keymap
   {{:char \i} [(cmd/edit #(tx/open-editor (:value %)))
                cmd/switch-to-typing-mode]
    {:char \t :specials #{:alt}} (cmd/set-temporary-keymap "transformation"
                                                           transformation-ident-keymap)
    {:char \n :specials #{:alt}} (cmd/edit #(update % :value next-candidate))
    {:char \d} (cmd/calcurate-value (partial * 2))
    {:char \c} (cmd/calcurate-value (partial * 1/2))
    {:char \D} (cmd/calcurate-value (partial * 10))
    {:char \C} (cmd/calcurate-value (partial * 0.1))
    {:char \a} (cmd/calcurate-n-digit 0 +)
    {:char \z} (cmd/calcurate-n-digit 0 -)
    {:char \s} (cmd/calcurate-n-digit 1 +)
    {:char \x} (cmd/calcurate-n-digit 1 -)}))

(def completion-table
  {"s" 'sin-osc
   "us" 'u/sin-r
   "w" 'white-noise
   "i" 'impulse
   "ln" 'lf-noise0
   "ur" 'u/rotate->
   "cl" 'clip:ar
   "uf" 'u/eff
   "lp" 'lf-pulse
   "rz" 'ringz
   "i*" '(iterate (fn [x] (* x)) 1)
   "rd" '(u/reduce-> (fn [acc x] (+ acc)) [])
   "fr" '(free-verb 1 1)
   "in" '(in (l4/sound-bus :exp-1 :out-bus) 2)
   "ep" '(env-gen (env-perc 0.05 0.5))
   "eg" '(env-gen (envelope [] []))
   "cheat" '(-> (map (fn [x y]
                       (* (rhpf (white-noise) (* 100 y) 0.4) 256
                          (env-gen (env-perc 1e-6 0.01) (impulse x))))
                     (take 4 (cycle [1 2 3 5 7 9]))
                     (reductions * 1 (cycle [5/2 3/2 4/5 5/11])))
               splay
               (u/reduce-> (fn [acc [x y z]]
                             (+ acc (* (lf-pulse y 0 z) (ringz acc x 0.001))))
                           [[8000 1/8 1/2] [20 1/12 1/16] [300 1.1 1/8]])
               (u/reduce-> free-verb [0.1 0.4 0.2])
               (g-verb 1.1)
               tanh (* 1/2))
   "cheat3" '(-> (map (fn [freq]
                        (-> (sin-osc freq)
                            (clip:ar -1 (u/sin-r 0.18 -1/2 1))
                            (round (u/sin-r 0.2 0.01 0.02))
                            (* (u/sin-r (u/rg-lin (lf-noise1 1.0) 0.04 0.16) 1/2 4))))
                      [220 80 30 120 6000])
                 (splay)
                 (u/rotate-> (sin-osc 0.08))
                 (hpf 80)
                 (u/reduce-> (fn [acc m] (free-verb acc m 1)) [0.1 0.6 0.5])
                 (* (u/rg-lin (lf-cub:kr 0.01) 4 16)) leak-dc tanh)
   "cheat2" '(-> (* (saw 80)
                    (lf-pulse 1 [0.0 1/2] 1/8))
                 (* 4)
                 (free-verb 0.8 1)
                 (tanh) (* 1/8))})

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
    {:char "<tab>"}  [(cmd/complete completion-table)
                      cmd/switch-to-selecting-mode]
    {:char "<esc>"}  [(cmd/edit tx/finish)
                      cmd/switch-to-selecting-mode]
    {:char \[}       [(cmd/edit tx/finish)
                      cmd/switch-to-selecting-mode]
    {:char \(}  (cmd/add-with-keep-position :parent (form/paren))
    {:char \}}  (cmd/add-with-keep-position :parent (form/vector))
    {:char "<space>" :specials #{:super}} [(cmd/edit tx/finish)
                                           (cmd/duplicate)
                                           (cmd/edit #(tx/open-editor (:value %)))
                                           cmd/switch-to-typing-mode]
    {:char "<space>"} [(cmd/edit tx/finish)
                       (cmd/add :right (form/input-ident))]}))

(def in-ugen-selecting-keymap
  (merge
   global-keymap
   node-selecting-keymap
   {{:char \o} cmd/open-window-from-in-ugen
    {:char \n :specials #{:alt}} (cmd/update-in-ugen-layer-id  ut/find-next-by)
    {:char \p :specials #{:alt}} (cmd/update-in-ugen-layer-id  ut/find-prev-by)}))

(defn- toggle-gauge [content]
  (update content :exp? not))

(def gauge-selecting-keymap
  (merge
   global-keymap
   paren-selecting-keymap
   {{:char \n :specials #{:alt}} (cmd/edit toggle-gauge)}))

(def key-table {:in    {:selecting in-ugen-selecting-keymap}
                :gauge  {:selecting gauge-selecting-keymap}
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
