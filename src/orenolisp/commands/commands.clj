(ns orenolisp.commands.commands
  (:require [orenolisp.util :as ut]
            [orenolisp.state :as st]
            [orenolisp.view.ui.fx-util :as fx]
            [orenolisp.model.editor :as ed]
            [orenolisp.model.conversion :as conv]
            [orenolisp.view.controller.expression-controller :as ec]
            [orenolisp.view.controller.window-controller :as wc]))

(defn open-new-window [state]
  (let [new-exp (ec/empty-expression)
        new-win (fx/run-now (wc/open-new-window (:exp-id new-exp)))]
    (-> state
        (update :windows #(assoc % (:exp-id new-exp) new-win))
        (update :expressions #(assoc % (:exp-id new-exp) new-exp))
        (assoc :current-exp-id (:exp-id new-exp)))))

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
        (ut/when-> modified? (assoc-in [:windows current-exp-id :context :modified?] true)))))

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
