(ns orenolisp.commands.commands
  (:require [orenolisp.util :as ut]
            [orenolisp.state :as st]
            [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.model.editor :as ed]
            [orenolisp.model.forms :as form]
            [orenolisp.model.conversion :as conv]
            [orenolisp.view.controller.expression-controller :as ec]
            [orenolisp.view.controller.window-controller :as wc]
            [orenolisp.view.ui.component.viewport :as viewport]
            [orenolisp.sc.eval :as sc]
            [orenolisp.sc.builder :as sb]
            [orenolisp.watcher.engine :as we]
            [orenolisp.view.ui.component.animations :as anim]
            [orenolisp.view.ui.component.logscreen :as log]
            [orenolisp.watcher.volume-watcher :as volume-watcher])
  (:refer-clojure :exclude [slurp]))

(defn set-temporary-keymap [description keymap]
  (fn [state] (st/temporary-keymap state description keymap)))

(defn cancel-temporary-keymap [state]
  (st/temporary-keymap state nil nil))


(defn- with-window [{:keys [windows expressions] :as state} exp-id f]
  (let [prev-exp (get expressions exp-id)
        new-exp (-> prev-exp
                    (ec/apply-step-function f))
        new-window (-> (get windows exp-id)
                       (wc/update-window (:editor prev-exp)
                                         (:editor new-exp)))
        new-windows (wc/arrange-window-position windows exp-id new-window)]
    (-> state
        (assoc-in [:expressions exp-id] new-exp)
        (assoc :windows new-windows))))

(defn- with-current-window [{:keys [current-exp-id] :as state} f]
  (with-window state current-exp-id f))

(defn window-command [f]
  (fn [state] (with-current-window state f)))

(defn add [direction form]
  (window-command #(ed/add % direction form)))

(defn excursion
  ([f]
   (window-command (fn [editor]
                     (let [org-node-id (ed/get-id editor :self)]
                       (-> editor f (ed/try-jump org-node-id))))))
  ([exp-id node-id f]
   (fn [state]
     (with-window state exp-id (fn [editor]
                     (let [org-node-id (ed/get-id editor :self)]
                       (-> editor
                           (ed/jump node-id)
                           f
                           (ed/try-jump org-node-id))))))))

(defn add-with-keep-position [direction form]
  (excursion #(ed/add % direction form)))

(defn switch-to-typing-mode [state]
  (st/update-current-context state #(assoc % :doing :typing)))
(defn switch-to-selecting-mode [state]
  (let [editor (st/current-editor state)]
    (if (ed/multiple-cursors-activated? editor)
      (-> state
          (st/clear-other-cursors)
          (st/update-current-context
           #(-> %
                (assoc :doing :selecting)
                (assoc :node-type (:type (ed/get-content editor))))))
      (-> state
          (st/update-current-context #(assoc % :doing :selecting))))))

(defn move [direction]
  (window-command #(ed/move % direction)))
(defn move-most [direction]
  (window-command #(ed/move-most % direction)))
(defn edit [f]
  (window-command #(ed/edit % f)))
(defn delete []
  (fn [state]
    (if (ed/root? (st/current-editor state))
      (-> state
          (with-current-window #(ed/add % :self (form/input-ident)))
          (switch-to-typing-mode))
      (with-current-window state #(ed/delete %)))))
(defn raise []
  (window-command #(ed/transport % :self (ed/get-id % :parent))))
(defn slurp []
  (window-command (fn [editor]
                    (if-let [[_ target-id] (ed/get-ids editor [:left :parent])]
                      (ed/transport editor :child target-id)
                      editor))))
(defn burf []
  (window-command (fn [editor]
                    (if-let [target-id (ed/get-id editor :parent)]
                      (ed/transport editor :right target-id)
                      editor))))
(defn swap [direction]
  (window-command (fn [editor]
                    (if-let [target-id (ed/get-id editor direction)]
                      (ed/transport editor direction target-id)
                      editor))))

(defn complete [table]
  (window-command
   (fn [editor]
     (let [v (some->> (ed/get-content editor) :value)]
       (when-let [sexp (->> v (get table))]
         (log/writeln "completed: " sexp)
         (->> (conv/convert-sexp->editor sexp)
              (ed/add-editor editor :self)))))))

(defn update-in-ugen-layer-id [find-f]
  (fn [state]
    (with-current-window state
      (fn [editor]
        (ed/edit editor
                 (fn [{current :exp-id :as m}]
                   (if-let [next-win (try (some-> (find-f #(= current (:exp-id %))
                                                          (-> state :windows vals)))
                                          (catch Exception e
                                            (first (-> state :windows vals))))]
                     (assoc m
                            :exp-id (:exp-id next-win)
                            :rate (or (-> next-win :sc-option :rate) :audio))
                     m)))))))

(defn- add-digit [n v f]
  (cond
    (= v 0) (f 0 (Math/pow 10 (* -1 n)))
    (number? v) (let [big-v (bigdec v)
                      digit (Math/pow 10 (- (int (Math/floor (Math/log10 v))) n))]
                  (if (and (>= digit 1) (= (.scale big-v) 0)) (f v (int digit))
                      (double (f big-v (bigdec digit)))))
    true nil))

(defn calcurate-n-digit [n f]
  (edit (fn [{:keys [value] :as m}]
          (let [v (if (string? value) (read-string value) value)]
            (if-let [v (add-digit n v f)]
              (assoc m :value (str v)) m)))))

(defn calcurate-value [f]
  (edit (fn [{:keys [value] :as m}]
          (let [v (if (string? value) (read-string value) value)]
            (if (number? v)
              (assoc m :value (str (f v)))
              m)))))

(defn duplicate []
  (window-command
   (fn [editor]
     (let [copied (-> (conv/sub-editor editor)
                      (ed/move-most :parent))]
       (-> editor
           (ed/add-editor :right copied))))))

(defn animate [direction animation-func]
  (fn [state]
    (let [target-id (ed/get-id (st/current-editor state) direction)
          ui (st/get-ui state target-id)]
      (.play (animation-func ui)))
    state))

(defn- open-window [state {:keys [exp-id] :as expression} new-layout]
  (log/writeln "open new window:" exp-id)
  (let [current-window (st/current-window state)
        current-layer-no (or (some-> current-window
                                     (get-in [:layout :layer-no]))
                             0)
        new-win (wc/new-window exp-id new-layout)]
    (volume-watcher/register expression)
    (wc/unfocus current-window)
    (wc/focus new-win current-layer-no)
    (-> state
        (update :windows #(assoc % exp-id new-win))
        (update :expressions #(assoc % exp-id expression))
        (assoc :current-exp-id exp-id))))

(def prepared-locations (atom (cycle [[0.4 0.3 0.15 0.3]
                                      ;; [0.5 0.8 0.725 0.5]
                                      ;; [0.5 0.8 0.175 0.5]
                                      ])))
(defn- pop-location []
  (let [args (apply viewport/location-by-ratio (first (swap! prepared-locations rest)))]
    (-> (apply wc/->layout 0 args)
        wc/convert-to-inner-layout)))

(defn- create-next-of-layout [layout]
  (update-in layout [:position :x] #(+ % (get-in layout [:size :w]) 45)))
(defn- create-below-of-layout [layout]
  (update-in layout [:position :y] #(+ % (get-in layout [:size :h]) 45)))

(defn open-new-window [state]
  (let [new-exp (ec/empty-expression)
        next-layout (or (some-> (st/current-window state)
                                :layout
                                create-next-of-layout)
                        (pop-location))]
    (sc/set-volume new-exp 1)
    (open-window state new-exp next-layout)))

(defn refresh [{:keys [current-exp-id] :as state}]
  (update-in state [:windows current-exp-id]
             #(wc/refresh % (st/current-expression state))))

(defn- create-sc-option [rate layer-no]
  {:rate rate :layer-no layer-no})

(defn create-new-layout [container-ui current-ui next-layer-no]
  (let [position (wc/->Position (+ (.getLayoutX container-ui) (.getLayoutX current-ui))
                                (+ (.getLayoutY container-ui) (.getLayoutY current-ui)))
        size (wc/->Size (.getWidth current-ui)
                        (.getHeight current-ui))]
    (wc/->Layout position size next-layer-no)))

(defn extract-as-in-ugen [rate]
  (fn [state]
    (let [copied-editor (-> (st/current-editor state)
                            conv/sub-editor (ed/move-most :parent))
          window (st/current-window state)
          current-layer-no (-> window (get-in [:layout :layer-no]))
          next-layer-no (inc current-layer-no)
          new-exp (ec/new-expression copied-editor (create-sc-option rate next-layer-no))
          new-layout (create-new-layout (:win-ui window)
                                        (st/current-ui state) next-layer-no)]
      (sc/set-volume new-exp 1)
      (-> (with-current-window state
            (fn [editor]
              (ed/add editor :self (form/in-ugen rate (:exp-id new-exp)))))
          (open-window new-exp new-layout)
          refresh))))

(defn copy-window [state]
  (let [org-window (st/current-window state)
        org-expression (st/current-expression state)
        copied-editor (-> (st/current-editor state) ed/copy)
        new-exp (ec/new-expression copied-editor (:sc-option org-expression))
        new-layout (create-below-of-layout (:layout org-window))]
    (sc/set-volume new-exp 1)
    (-> state
        (open-window new-exp new-layout)
        (update :windows #(wc/arrange-window-position % (:exp-id new-exp)))
        refresh)))

(defn move-window [find-f]
  (fn [state]
    (let [window (st/current-window state)
          current-layer-no (get-in window [:layout :layer-no])
          next-id (->> (keys (:windows state))
                       (find-f (:exp-id window)))]
      (when next-id
        (wc/focus (get-in state [:windows next-id]) current-layer-no)
        (wc/unfocus window)
        (assoc state :current-exp-id next-id)))))

(defmacro no-exception? [& body]
  `(try ~@body
       true
       (catch Throwable e#
         (println "error: " (or (.getMessage e#) e#)
                  "(" (some-> e# (.getCause) (.getMessage)) ")")
         (log/writeln "error: " (or (.getMessage e#) e#)
                      "(" (some-> e# (.getCause) (.getMessage)) ")")
         false)))

(defn- register-watchers [{:keys [exp-id] :as expression} watcher-gens]
  (doseq [[node-id watcher-gen] watcher-gens]
    (let [watcher (watcher-gen expression node-id)]
      (we/register exp-id node-id (comp watcher sc/get-node-value)))))

(defn- evaluate-exp [state exp-id]
  (let [window (get-in state [:windows exp-id])
        ui (wc/get-frame-ui window)
        expression (get-in state [:expressions exp-id])
        result (no-exception? (-> expression sb/exp->sexp sc/doit))]
    (.play (anim/flash ui result))
    (register-watchers expression (:watcher-gens window))
    (if result
      (assoc-in state [:windows exp-id :context :modified?] false)
      state)))

(defn evaluate [state]
  (->> (:current-exp-id state)
       (evaluate-exp state)))

(defn evaluate-all-modified [state]
  (let [modified-exp-ids (->> (:windows state)
                              vals
                              (filter #(get-in % [:context :modified?]))
                              (map :exp-id))]
    (reduce evaluate-exp state modified-exp-ids)))

(defn- change-window-layout [f]
  (fn [state]
    (let [window (st/current-window state)
          new-window (f state window)]
      (-> state
          (update :windows
                  #(wc/arrange-window-position % (:exp-id window) new-window))))))

(defn widen-window [dw]
  (change-window-layout
   (fn [state w]
     (let [new-width (+ dw (get-in w [:layout :size :w]))]
       (wc/layout (st/current-editor state) w new-width)))))

(defn fit-window-height []
  (change-window-layout
   (fn [state w]
     (wc/layout (st/current-editor state)
                w
                (get-in w [:layout :size :w]) true))))

(defn half-window-height []
  (change-window-layout
   (fn [state w]
     (wc/update-window-size w (update (get-in w [:layout :size])
                                      :h #(/ % 2))))))


(defn open-window-from-in-ugen [state]
  (when-let [next-exp-id (:exp-id (st/current-content state))]
    ((move-window (fn [_ _] next-exp-id)) state)))

(defn register-watcher [watcher-gen]
  (fn [state]
    (let [[exp-id node-id] (st/current-id state)]
      (assoc-in state [:windows exp-id :watcher-gens node-id] watcher-gen))))

(defn edit-attributes [exp-id node-id f]
  (fn [state]
    (let [editor (get-in state [:expressions exp-id :editor])]
    (-> state
        (update-in [:windows exp-id] #(wc/update-node-attributes % editor node-id f))))))

(defn stop-sound [state]
  (let [exp-id (:current-exp-id state)]
    (sc/stop-sound exp-id))
  state)

(defn log [message]
  (fn [state]
    (log/writeln message)
    state))
