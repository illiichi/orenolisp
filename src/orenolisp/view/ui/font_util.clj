(ns orenolisp.view.ui.font-util
  (:import (javafx.scene.text Text Font)))

;; this value should be same as style.css
(def LABEL-FONT (Font/font "file:resources/Arvo-Regular.ttf" 20.0))
(def PORTAL-FONT (Font/font "file:resources/Scifi Adventure.otf" 22.0))
(def LOG-FONT (Font/loadFont "file:resources/Orbitron-Medium.ttf" 12.0))
(def TYPING-LOG-FONT (Font/loadFont "file:resources/Orbitron-Medium.ttf" 15.0))
(def SMALL-LABEL-FONT (Font/loadFont "file:resources/Roboto-Light.ttf" 16.0))
(def CONTEXT-FONT (Font/loadFont "file:resources/Orbitron-Bold.ttf" 30.0))
(def METER-FONT (Font/loadFont "file:resources/Roboto-Light.ttf" 12.0))
(def METER-TYPE-FONT (Font/loadFont "file:resources/Orbitron-Bold.ttf" 10.0))


(defn- font-size [font]
  (-> (doto (Text.)
        (.setFont font)
        (.setText "A"))                 ; font はmonospaceと仮定
      .getLayoutBounds
      (as-> b
          [(.getWidth b) (.getHeight b)])))

(def ^:const LABEL-FONT-WIDTH
  (->> LABEL-FONT font-size first))
(def ^:const LABEL-FONT-HEIGHT
  (->> LABEL-FONT font-size second))
(def ^:const PORTAL-FONT-WIDTH
  (->> PORTAL-FONT font-size first))
(def ^:const PORTAL-FONT-HEIGHT
  (->> PORTAL-FONT font-size second))
