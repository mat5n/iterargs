(ns iterargs.gesture
  (:require
   [iterargs.doc :as doc]
   [iterargs.help-state :as help-state]
   [iterargs.menu-state :as menu-state]
   [iterargs.state :as state])
  (:require-macros
   [iterargs.multi :refer [defmethod+]]))

;;;; Gestures

(def contextual-gestures
  {[::doc/odd  ::swipe-right] ::swipe-inward
   [::doc/even ::swipe-left ] ::swipe-inward

   [::doc/odd  ::swipe-left ] ::swipe-outward
   [::doc/even ::swipe-right] ::swipe-outward})

(derive ::swipe-left ::swipe)
(derive ::swipe-right ::swipe)
(derive ::swipe-inward ::swipe)
(derive ::swipe-outward ::swipe)

;;;; Reactions to swipes based on context

(defmulti react!
  (fn [event]
    (let [[p g :as pg] ((juxt :part :gesture) event)]
      [p (get contextual-gestures pg g)])))

(defmethod+ react! [event]
  [::doc/argument-part ::swipe-outward]
  (let [opp-side (doc/opposite-side (:part event))]
    (state/queue!
      (if (state/split-view?)
        (state/set-split-pref false opp-side)
        (state/goto-part opp-side))))

  [::doc/argument-part ::swipe-inward]
  (state/queue!
    (if (state/split-view?)
      (state/set-split-pref false (:part event))
      (state/set-split-pref true)))

  [::doc/support-part ::swipe]
  (state/queue! (state/goto-part (:side @state/state)))

  [::doc/back ::swipe-left]
  (state/queue!
    (if (:peek-back? @state/state)
      (state/set-peek-back false)
      (state/goto-part (:side @state/state))))

  [::doc/back ::swipe-right]
  (state/queue!
    (state/goto-part
      (if (:peek-back? @state/state)
        ::doc/back
        (:side @state/state))))

  [:iterargs.help/help ::swipe]
  (help-state/toggle!)

  [:iterargs.menu/menu ::swipe]
  (menu-state/toggle!))

;;;; Swipe recogniser

(defn touch-point [evt]
  (let [touch (aget (.-changedTouches evt) 0)]
    [(.-clientX touch)
     (.-clientY touch)
     (js/Date.now)]))

(defn distance [start end]
  (let [[x1 y1 t1] start
        [x2 y2 t2] end]
    [(Math/abs (- x1 x2))
     (Math/abs (- y1 y2))
     (- t2 t1)]))

(defn angle [x y]
  (* (/ 180 Math/PI)
     (Math/atan (/ y x))))

(def min-dx 80)
(def max-dt 200)
(def max-angle 20)

(defn horizontal-swipe? [start end]
  (let [[dx dy dt] (distance start end)]
    (and (>= dx min-dx)
         (<= dt max-dt)
         (<= (angle dx dy) max-angle))))

(defn swipe-dir [start end]
  (when (horizontal-swipe? start end)
    (if (< (start 0) (end 0))
      ::swipe-right
      ::swipe-left)))

;;;; API

(defn swipe-recogniser
  "Returns a pair of functions that are to listen to touchstart and
  touchend events on the container of `part`. When a left or right
  swipe is recognised, performs the appropriate action depending on
  context."
  [part]
  (let [start (atom nil)]
    [(fn [evt]
       (reset! start (touch-point evt)))
     (fn [evt]
       (let [end (touch-point evt)]
         (when-let [dir (swipe-dir @start end)]
           (react! {:part part :gesture dir}))))]))
