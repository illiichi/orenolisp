(ns orenolisp.commands.commands
  (:require [orenolisp.util :as ut]
            [orenolisp.state :as st]
            [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.model.editor :as ed]
            [orenolisp.model.forms :as form]
            [orenolisp.model.conversion :as conv]
            [orenolisp.view.controller.expression-controller :as ec]
            [orenolisp.view.controller.window-controller :as wc]))

(defn set-temporary-keymap [description keymap]
  (fn [state] (st/temporary-keymap state description keymap)))

(defn cancel-temporary-keymap [state]
  (st/temporary-keymap state nil nil))

(defn- with-current-window [{:keys [current-exp-id windows expressions] :as state} modified? f]
  (let [prev-exp (get expressions current-exp-id)
        new-exp (-> prev-exp
                    (ec/apply-step-function f))
        new-window (-> (get windows current-exp-id)
                       (wc/update-window (:editor prev-exp)
                                         (:editor new-exp)))]
    (-> state
        (assoc-in [:expressions current-exp-id] new-exp)
        (assoc-in [:windows current-exp-id] new-window)
        (ut/when-> modified?
                   (assoc-in [:windows current-exp-id :context :modified?] true)))))

(defn window-command [f]
  (fn [state] (with-current-window state true f)))
(defn window-command-pure [f]
  (fn [state] (with-current-window state false f)))

(defn add [direction form]
  (window-command #(ed/add % direction form)))

(defn with-keep-position [f]
  (window-command
   #(let [node-id (ed/get-id % :self)]
      (some-> % f (ed/jump node-id)))))

(defn add-with-keep-position [direction form]
  (with-keep-position #(ed/add % direction form)))

(defn move [direction]
  (window-command-pure #(ed/move % direction)))
(defn move-most [direction]
  (window-command-pure #(ed/move-most % direction)))
(defn edit [f]
  (window-command #(ed/edit % f)))
(defn delete []
  (window-command #(ed/delete %)))
(defn raise []
  (window-command #(ed/transport % :self (ed/get-id % :parent))))

(defn switch-to-typing-mode [state]
  (st/update-current-context state #(assoc % :doing :typing)))
(defn switch-to-selecting-mode [state]
  (st/update-current-context state #(assoc % :doing :selecting)))

(defn complete [table]
  (window-command
   (fn [editor]
     (let [v (some->> (ed/get-content editor) :value)]
       (when-let [sexp (->> v (get table))]
         (->> (conv/convert-sexp->editor sexp)
              (ed/add-editor editor :self)))))))

(defn update-in-ugen-layer-id [find-f]
  (fn [state]
    (with-current-window state true
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
    (number? v) (let [digit (Math/pow 10 (- (int (Math/log10 v)) n))]
                  (f v (if (>= digit 1) (int digit) digit)))
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

(defn- open-window [state {:keys [exp-id] :as expression} current-layer-no new-layer-no]
  (let [new-win (fx/run-now (wc/open-new-window exp-id current-layer-no new-layer-no))]
    (-> state
        (update :windows #(assoc % exp-id new-win))
        (update :expressions #(assoc % exp-id expression))
        (assoc :current-exp-id exp-id))))

(defn open-new-window [state]
  (open-window state (ec/empty-expression) 0 0))

(defn refresh [{:keys [current-exp-id] :as state}]
  (update-in state [:windows current-exp-id]
             #(wc/refresh % (st/current-expression state))))

(defn extract-as-in-ugen [rate]
  (fn [state]
    (let [copied-editor (-> (st/current-editor state)
                            conv/sub-editor (ed/move-most :parent))
          new-exp (ec/new-expression copied-editor)
          current-layer-no (-> (st/current-window state)
                               (get-in [:layout :layer-no]))]
      (-> (with-current-window state true
            (fn [editor]
              (ed/add editor :self (form/in-ugen rate (:exp-id new-exp)))))
          (open-window new-exp current-layer-no (inc current-layer-no))
          refresh))))

(defn move-window [find-f]
  (fn [state]
    (let [window (st/current-window state)
          current-layer-no (get-in window [:layout :layer-no])
          next-id (->> (keys (:windows state))
                       (find-f (:exp-id window)))]
      (when next-id
        (wc/focus (get-in state [:windows next-id]) current-layer-no)
        (assoc state :current-exp-id next-id)))))
