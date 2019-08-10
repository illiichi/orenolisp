(ns orenolisp.sc.util
  (:use [overtone.core]))

(defmacro if->> [pred f f2 v]
  `(if ~pred (~f ~v) (~f2 ~v)))

(defmacro when->> [pred f v]
  (if (list? f)
    `(if ~pred ~(concat f [v]) ~v)

    `(if ~pred (~f ~v) ~v)))

(defn shuffle-n [n xs]
  (shuffle (take n xs)))

(def up-to-32
  {:default {:count 0
             :ll2 0
             :u1 0
             :u2 0
             :u3 0
             :u4 0}
   :update-func (fn [{:keys [count] :as state}]
                  (-> state
                      (update :count #(mod (inc %) 32))
                      (assoc :ll2 (bit-and count 2r0011))
                      (assoc :u1 (bit-test count 0))
                      (assoc :u2 (bit-test count 1))
                      (assoc :u3 (bit-test count 2))
                      (assoc :u4 (bit-test count 3))))})

(defn map-square [from to x]
  (let [height (- to from)]
    (+ from (* (Math/pow (mod x 1) 2) height))))

(defn count-up
  ([from to] {:default {:count from}
              :update-func (fn [{:keys [count] :as state}]
                             (-> state (update :count #(+ (mod (- (inc %) from) (- to from))
                                                          from))))}))

(def step-to-32
  {:default {:count 0
             :ll2 0
             :u3 0
             :u4 0
             :direction 1}
   :update-func (fn [{:keys [count direction] :as state}]
                  (let [next-count (+ count direction)
                        next-direction (if (#{0 31} next-count) (* -1 direction) direction)
                        ]
                    (-> state
                        (assoc :count next-count)
                        (assoc :direction next-direction)
                        (assoc :ll2 (bit-and count 2r0011))
                        (assoc :u3 (bit-test count 2))
                        (assoc :u4 (bit-test count 3)))))})
(defn transpose [xss]
  (apply map list xss))

(defn lower-bound [in]
  (case (first in)
    latch:ar (lower-bound (second in))
    sin-osc -1
    sin-osc:kr -1
    lf-tri -1
    lf-tri:kr -1
    lf-saw -1
    lf-saw:kr -1
    lf-cub    -1
    lf-cub:kr -1
    white-noise -1
    lf-noise0 -1
    lf-noise1 -1
    lf-noise2 -1
    lf-noise0:kr -1
    lf-noise1:kr -1
    lf-noise2:kr -1
    lf-pulse 0
    lf-pulse:kr 0
    0))

(defmacro rg-lin [in lo hi]
  `(lin-lin ~in ~(lower-bound in) 1 ~lo ~hi))
(defmacro rg-exp [in lo hi]
  `(lin-exp ~in ~(lower-bound in) 1 ~lo ~hi))

(defmacro dq [trig arr] `(demand ~trig 0 (dseq ~arr INF)))
(defmacro dq:kr [trig arr] `(demand:kr ~trig 0 (dseq ~arr INF)))
(defmacro dt [dur arr]
  (if (coll? dur)
    `(duty:ar (dseq ~dur INF) 0 (dseq ~arr INF))
    `(duty:ar ~dur 0 (dseq ~arr INF))))

(defmacro dt:kr [dur arr]
  (if (coll? dur)
    `(duty:kr (dseq ~dur INF) 0 (dseq ~arr INF))
    `(duty:kr ~dur 0 (dseq ~arr INF))))

(defmacro switch [trig a b]
  `(let [t# ~trig]
     (~'+
      (~'* t# ~a)
      (~'* (~'- 1 t#) ~b))))

(defmacro pattern [trig ptn]
  `(~'* ~trig (demand ~trig 0 (dseq (flatten ~ptn) INF))))

(defmacro eff [rate body]
  `(~'+ 1 (~'* ~rate (~'- ~body 1) )))

(defmacro throttle
  ([trig count] `(stepper ~trig 0 0 ~count))
  ([trig count phase] `(~'- (stepper ~trig 0 0 ~count) ~phase)))


(defcgen sin-r [freq {:default 440}
                min-value {:default 0}
                max-value {:default 1}]
  (:ar (lin-lin (sin-osc:ar freq (* 2 Math/PI (rand)))
                -1 1 min-value max-value))
  (:kr (lin-lin (sin-osc:kr freq (* 2 Math/PI (rand)))
                -1 1 min-value max-value)))
(defcgen sin-rex [freq {:default 440}
                  min-value {:default 0}
                  max-value {:default 1}]
  (:ar (lin-exp (sin-osc:ar freq (* 2 Math/PI (rand)))
                -1 1 min-value max-value))
  (:kr (lin-exp (sin-osc:kr freq (* 2 Math/PI (rand)))
                -1 1 min-value max-value)))

(defmacro reduce->
  [initial f & colls] (if (= (count colls) 1)
                        `(reduce ~f ~initial ~(first colls))
                        `(reduce ~f ~initial (transpose ~(vec colls)))))

(defmacro switch->
  [in b eff] `(switch ~b (-> ~in ~eff) ~in))

(defmacro eff->
  [org b another] `(eff ~b (-> ~org ~another)))

(defn m-map [f & xs]
  (let [result (apply map f xs)
        is-single (map? (first result))]
    (if is-single
      (mix result)
      (map mix result))))

(defmacro s-map [f xs]
  (list 'splay:ar ('map f xs)))

(defn n-range [min max num]
  (range min max (/ (- max min) num)))

(defcgen rotation [pos {:default 0} source {:default 0}]
  (:ar (let [[x y] source] (rotate2 x y pos))))

(defmacro rotate-> [snd pos]
  `(let [[snd1# snd2#] ~snd] (rotate2 snd1# snd2# ~pos)))

(defmacro detune-rotate [pos f]
  (let [d 0.01]
    `(let [body# (fn [~'dr] ~f)]
       (rotate2 (body# (- 1 ~d))
                (body# (+ 1 ~d))
                ~pos))))

(defmacro tap-tap [node-id body]
  `(let [snd# ~body]
         (overtone.sc.cgens.tap/tap ~(str "n-" node-id) 12 snd#)
         snd#))

(defmacro tap-line [node-id from to dur exp?]
  `(tap-tap ~node-id ~(list (if exp? 'x-line:kr 'line:kr) from to dur)))

(defmacro c-saw [freq phase c-min c-max min max step]
  `(-> (lf-saw:kr ~freq ~phase)
       (clip ~c-min ~c-max)
       (lin-lin ~c-min ~c-max ~min ~max)
       (round ~step)))
