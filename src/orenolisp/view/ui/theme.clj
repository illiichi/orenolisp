(ns orenolisp.view.ui.theme
  (:import (javafx.scene.text Text Font)
           (javafx.scene.paint Color)))

(defn opacity [color alpha]
  (Color. (.getRed color) (.getGreen color) (.getBlue color) alpha))

(def label-font (Font/font "file:resources/Arvo-Regular.ttf" 20.0))
(def in-ugen-font (Font/loadFont "file:resources/Orbitron-Medium.ttf" 16.0))
(def log-font (Font/loadFont "file:resources/SpecialElite-Regular.ttf" 14.0))
(def typing-log-font (Font/loadFont "file:resources/Orbitron-Medium.ttf" 15.0))
(def small-label-font (Font/loadFont "file:resources/Roboto-Light.ttf" 16.0))
(def context-font (Font/loadFont "file:resources/Orbitron-Bold.ttf" 30.0))
(def meter-font (Font/loadFont "file:resources/Roboto-Light.ttf" 12.0))
(def meter-type-font (Font/loadFont "file:resources/Orbitron-Bold.ttf" 10.0))
(def window-indicator-large-font (Font/loadFont "file:resources/Orbitron-Bold.ttf" 18.0))
(def window-indicator-font       (Font/loadFont "file:resources/Orbitron-Regular.ttf" 12.0))


(defn- font-size [font]
  (-> (doto (Text.)
        (.setFont font)
        (.setText "A"))                 ; font はmonospaceと仮定
      .getLayoutBounds
      (as-> b
          [(.getWidth b) (.getHeight b)])))

(def ^:const label-font-width
  (->> label-font font-size first))
(def ^:const label-font-height
  (->> label-font font-size second))
(def ^:const in-ugen-font-width
  (->> in-ugen-font font-size first))
(def ^:const in-ugen-font-height
  (->> in-ugen-font font-size second))

(def style-primary-color "#00CCFF")
(def primary-color (Color/web style-primary-color))

(def unfocus-text-color (Color/web "#CCCCFF"))
(def focus-text-color (Color/web "#FFFFFF"))
(def mark-text-color Color/BLACK)
(def mark-text-backcolor (Color/web "#AAAAFFAA"))
(def caret-color Color/WHITE)
(def empty-caret-color Color/RED)
(def gauge-backcolor (Color/web "#AACCFF33"))
(def style-log-color "#88AAAA")
(def mark-paren-backcolor (Color/web "#8888FF22"))
(def focus-paren-color Color/WHITE)
(def focus-paren-border-color (Color/web "#AAAAAAAA"))
(def focus-vector-color (Color/web "#FFFFFF"))
(def unfocus-vector-color (Color/web "#8888AA88"))

(def window-border-color-audio primary-color)
(def window-border-color-control (Color/web "#EEAAFF"))
(def window-backcolor (Color/web "#001122AA"))
(def window-title-color Color/BLACK)
(def window-border-color-stop (Color/web "#333333"))
(def window-indicator-text-color Color/WHITE)
