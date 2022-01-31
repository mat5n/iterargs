(ns iterargs.icon
  (:require
   [iterargs.util :refer [p]]))

(defn polygon [points]
  [:polygon {:points points}])

(defn cross [opts]
  (let [{s :size t :thickness} opts
        a (/ t (Math/sqrt 2))
        b (- s a)]
    [:svg {:width s :height s :class [(p :icon) (p :cross)]}
     [polygon [a 0, s b, b s, 0 a]]
     [polygon [b 0, s a, a s, 0 b]]]))

(defn rect [x y w h]
  [:rect {:x x :y y :width w :height h}])

(defn burger [opts]
  (let [{w :width h :height t :thickness} opts]
    [:svg {:width w :height h :class [(p :icon) (p :burger)]}
     [rect 0 0 w t]
     [rect 0 (- (/ h 2) (/ t 2)) w t]
     [rect 0 (- h t) w t]]))
