(ns orenolisp.view.ui.component.animations
  (:require [orenolisp.view.ui.fx-util :as fx])
  (:import (javafx.scene.paint Color)
           [javafx.animation Animation]
           [javafx.scene.effect Blend BlendMode ColorAdjust DropShadow InnerShadow Bloom
            Glow]))

(defn blend [xs]
  (let [[x1 x2 & rest] (reverse xs)
        initial (doto (Blend.)
                  (.setMode BlendMode/MULTIPLY)
                  (.setBottomInput x2)
                  (.setTopInput x1))]
    (reduce (fn [acc x] (doto (Blend.)
                          (.setMode BlendMode/MULTIPLY)
                          (.setBottomInput x)
                          (.setTopInput acc)))
            initial
            rest)))

(defn drop-shadow [r g b opacity offset-x offset-y radius spread]
  (doto (DropShadow.)
    (.setColor (Color. (double (/ r 256.0)) (double (/ g 256.0)) (double (/ b 256.0))
                       (double opacity)))
    (.setOffsetX (double offset-x))
    (.setOffsetY (double offset-y))
    (.setRadius (double radius))
    (.setSpread (double spread))))

(defn inner-shadow [r g b opacity radius choke]
  (doto (InnerShadow.)
    (.setColor (Color. (double (/ r 256.0)) (double (/ g 256.0)) (double (/ b 256.0))
                       (double opacity)))
    (.setRadius (double radius))
    (.setChoke (double choke))))

(defn dissapear [ui]
  (let [key-frames [(fx/->KeyFrame 0
                                   [(.opacityProperty ui) 1]
                                   [(.scaleXProperty ui) 1]
                                   [(.scaleYProperty ui) 1])
                    (fx/->KeyFrame 75
                                   [(.scaleXProperty ui) 1.3]
                                   [(.scaleYProperty ui) 1.15])
                    (fx/->KeyFrame 150
                                   [(.opacityProperty ui) 0]
                                   [(.scaleXProperty ui) 1.5]
                                   [(.scaleYProperty ui) 1.2])]]
    (fx/create-animation key-frames)))

(defn glow [ui]
  (if-let [effect (or (.getEffect ui) (let [g (Glow. 0.8)]
                                     (.setEffect ui g)
                                     g))]
    (fx/create-animation [(fx/->KeyFrame 0 [(.levelProperty effect) 0])
                          (fx/->KeyFrame 20 [(.levelProperty effect) 1])
                          (fx/->KeyFrame 200 [(.levelProperty effect) 0])])))

(defn glow-shadow [ui]
  (if-let [effect (some-> ui (.getEffect) (.getTopInput))]
    (fx/create-animation [(fx/->KeyFrame 0 [(.radiusProperty effect) 0])
                          (fx/->KeyFrame 20
                                         [(.radiusProperty effect) 10]
                                         [(.spreadProperty effect) 20])
                          (fx/->KeyFrame 200
                                         [(.radiusProperty effect) 20]
                                         [(.spreadProperty effect) 0])])))

(defn flash [ui good?]
  (let [org-effect (.getEffect ui)
        shadow (if good? (drop-shadow 255 255 255 1 0 0 1 1)
                   (drop-shadow 255 0 0 1 0 0 1 1))
        radius-p (.radiusProperty shadow)]
    (.setEffect ui shadow)
    (doto (fx/create-animation [(fx/->KeyFrame 0 [radius-p 0])
                                (fx/->KeyFrame 50 [radius-p 2])
                                (fx/->KeyFrame 100 [radius-p 0.8])
                                (fx/->KeyFrame 150 [radius-p 0])])
      (fx/on-animation-finished (fn [_]
                                  (.setEffect ui org-effect))))))

(defn- shadow-and-keyframes []
  (let [rs [10 20 40 60]
        shadows (map #(doto (DropShadow.)
                        (.setRadius %2)
                        (.setColor (Color/web %)))
                     ["#FFFFFF" "#FFFFFF" "#FFFFFF" "#228DFF" "#228DFF"]
                     rs)
        effects shadows
        frame-0 (mapcat (fn [eff r] [[(.heightProperty eff) r]
                                     [(.spreadProperty eff) 1]]) shadows (cons 5 rs))
        frame-1 (mapcat (fn [eff r] [[(.heightProperty eff) (* 2 r)]
                                     [(.spreadProperty eff) 0]]) shadows (cons 5 rs))
        frame-2 (map (fn [eff r] [(.radiusProperty eff) 0]) effects (cons 5 rs))
        ]
    [(blend effects) [(apply fx/->KeyFrame 0 frame-0)
                      (apply fx/->KeyFrame 100 frame-1)
                      (apply fx/->KeyFrame 150 frame-2)]]))

(defn- make-delay [delay-s key-frames]
  (map (fn [[x xs]] [(if (= x 0) x (+ x delay-s)) xs]) key-frames))

(defn white-in
  ([delay-s ui]
   (let [opacity-p (.opacityProperty ui)
         color (ColorAdjust.)
         brigt-p (.brightnessProperty color)
         [effect key-frames] (shadow-and-keyframes)]
     (.setEffect ui effect)
     (.setScaleX ui 1)
     (.setScaleY ui 1)
     (doto (fx/create-animation (->> (concat key-frames
                                             [(fx/->KeyFrame 0 [opacity-p 0]
                                                             [brigt-p 1])
                                              (fx/->KeyFrame 30
                                                             [opacity-p 1])
                                              (fx/->KeyFrame 100
                                                             [brigt-p 0.8]
                                                             [opacity-p 1])
                                              ;; (fx/->KeyFrame 150 [opacity-p 0.3])
                                              (fx/->KeyFrame 220 [brigt-p 0])])
                                     (make-delay delay-s)))
       (fx/on-animation-finished (fn [_]
                                   (.setEffect ui nil)))))))
(defn enphasize
  ([ui]
   (let [opacity-p (.opacityProperty ui)
         color (ColorAdjust.)
         brigt-p (.brightnessProperty color)
         [effect key-frames] (shadow-and-keyframes)
         org-effect (.getEffect ui)]
     (.setEffect ui effect)
     (doto (fx/create-animation (concat key-frames
                                        [(fx/->KeyFrame 0 [opacity-p 0]
                                                        [brigt-p 1])
                                         (fx/->KeyFrame 30
                                                        [opacity-p 1])
                                         (fx/->KeyFrame 100
                                                        [brigt-p 0.8]
                                                        [opacity-p 1])
                                         (fx/->KeyFrame 220 [brigt-p 0])]))
       (fx/on-animation-finished
        (fn [_]
          ;; fixme: ad-hoc implementation
          (when (not (nil? (.getEffect ui)))
            (.setEffect ui org-effect))))))))

(defn white-out [ui]
  (let [opacity-p (.opacityProperty ui)
        color (ColorAdjust.)
        brigt-p (.brightnessProperty color)
        scale-x (.scaleXProperty ui)
        scale-y (.scaleYProperty ui)]
    (.setEffect ui color)
    (doto (fx/create-animation [(fx/->KeyFrame 0
                                               [opacity-p 0.3]
                                               [brigt-p 0]
                                               [scale-x 1]
                                               [scale-y 1])
                                (fx/->KeyFrame 100
                                               [brigt-p 1]
                                               [opacity-p 1]
                                               [scale-y 1]
                                               [opacity-p 1])
                                (fx/->KeyFrame 150 [scale-x 1])
                                (fx/->KeyFrame 250
                                               [scale-x 0.5]
                                               [scale-y 0]
                                               [opacity-p 0])])
      (fx/on-animation-finished (fn [_]
                                  (.setEffect ui nil))))))

(defn mark []
  (blend [(drop-shadow 255 0 0 0.8 0 0 4 0.8)
          (drop-shadow 255 0 0 0.3 0 0 24 0.8)]))

(defn super-shadow [ui]
  (let [[effect key-frames] (shadow-and-keyframes)]
    (.setEffect ui effect)
    (doto (fx/create-animation key-frames)
      (fx/on-animation-finished (fn [_]
                                  (.setEffect ui nil))))))


(defn- shadow-and-keyframes-2 []
  (let [rs [10 20 40]
        shadows (map #(doto (DropShadow.)
                        (.setRadius %2)
                        (.setColor (Color/web %)))
                     ["#FFFFFF" "#FFFFFF" "#FFFFFF"]
                     rs)
        effects shadows
        frame-0 (mapcat (fn [eff r] [[(.heightProperty eff) r]
                                     [(.spreadProperty eff) 1]]) shadows (cons 5 rs))
        frame-1 (mapcat (fn [eff r] [[(.heightProperty eff) (* 2 r)]
                                     [(.spreadProperty eff) 0]]) shadows (cons 5 rs))
        frame-2 (map (fn [eff r] [(.radiusProperty eff) 0]) effects (cons 5 rs))
        ]
    [(blend effects) [(apply fx/->KeyFrame 0 frame-0)
                      (apply fx/->KeyFrame 150 frame-1)
                      (apply fx/->KeyFrame 300 frame-2)]]))
(defn zoom-in [ui]
  (let [[effect key-frames] (shadow-and-keyframes-2)
        color-adjust (ColorAdjust.)
        effect (blend [color-adjust effect])
        key-frames (concat key-frames
                           [(fx/->KeyFrame 0
                                           [(.brightnessProperty color-adjust) 1.0]
                                           [(.scaleXProperty ui) 1.0]
                                           [(.scaleYProperty ui) 1.0])
                            (fx/->KeyFrame 20
                                           [(.scaleXProperty ui) 1.05]
                                           [(.scaleYProperty ui) 1.02])
                            (fx/->KeyFrame 100
                                           [(.brightnessProperty color-adjust) 1.0]
                                           [(.scaleXProperty ui) 1]
                                           [(.scaleYProperty ui) 1])
                            (fx/->KeyFrame 300
                                           [(.brightnessProperty color-adjust) 0])])]
    (.setEffect ui effect)
    (doto (fx/create-animation key-frames)
      (fx/on-animation-finished (fn [_]
                                  (.setEffect ui nil))))))

(defn move-ui [ui from to]
  (let [px (.layoutXProperty ui)
        py (.layoutYProperty ui)
        mid (fn [a1 a2] (+ a1 (* 0.9 (- a2 a1))))
        key-frames [(fx/->KeyFrame 0
                                   [px (:x from)]
                                   [py (:y from)])
                    (fx/->KeyFrame 100
                                   [px (mid (:x from) (:x to))]
                                   [py (mid (:y from) (:y to))])
                    (fx/->KeyFrame 300
                                   [px (:x to)]
                                   [py (:y to)])]]
    (fx/create-animation key-frames)))
