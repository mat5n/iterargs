(ns iterargs.animation
  (:require
   [iterargs.doc :as doc]
   [iterargs.state :as state :refer [split-view?]])
  (:require-macros
   [iterargs.multi :refer [defmethod+]]))

;;;; Animation hierarchy

(derive ::in-half ::in)
(derive ::in-full ::in)
(derive ::out-half ::out)
(derive ::out-full ::out)

;;;; Constants (+ helpers to compute them)

(defn symmetric-closure [m]
  (reduce (fn [m [x y]] (assoc m y x)) m m))

(def reverse-anim
  (symmetric-closure
    {::in ::out ::in-half ::out-half ::in-full ::out-full
     ::half-to-full ::full-to-half
     ::peek-in ::peek-out ::peek-to-full ::full-to-peek}))

(defn add-reverse [anims]
  (concat anims (map reverse-anim anims)))

(def arg-part-anims
  (add-reverse [::in-half ::in-full ::half-to-full]))

(def sup-part-anims [::in ::out])

(def peek-back-anims (add-reverse [::peek-in ::peek-to-full]))

(defn pairs [coll1 coll2]
  (for [x coll1 y coll2]
    [x y]))

(def part-anim-combos
  (concat (pairs doc/argument-parts arg-part-anims)
          (pairs doc/support-parts sup-part-anims)
          (pairs [::doc/back] peek-back-anims)))

;;;; Animation predicate

(def ^:dynamic *time-reversal* false)

(defmulti run-anim?
  (fn [part anim before after]
    [part anim]))

(defmethod+ run-anim? [part anim before after]
  [::doc/argument-part ::in]
  (let [sva? (:split-view? after)]
    (and (or sva? (= part (:part after)))
         (not= part (:part before))
         (not (:split-view? before))
         (if (= anim ::in-half)
           sva?
           (not sva?))))

  [::doc/argument-part ::half-to-full]
  (and (:split-view? before)
       (not (:split-view? after))
       (= part (:part after)))

  [::doc/support-part ::in]
  (and (= part (:part after))
       (not= part (:part before))
       (or (not= part ::doc/back)
           (not (:peek-back? before))))

  [::doc/back ::peek-in]
  (and (:peek-back? after)
       (not (:peek-back? before))
       (not (= part (:part before))))

  [::doc/back ::peek-to-full]
  (and (:peek-back? before)
       (= part (:part after)))

  :default
  ;; test for reverse animation in reverse time unless already in
  ;; reverse time in which case signal error
  (do (assert (not *time-reversal*) (str "run-anim?: no rule for: " part ", " anim))
      (binding [*time-reversal* true]
        (run-anim? part (reverse-anim anim) after before))))

;;;; Relevant state

(def relevant-keys [:part :peek-back?])

(defn relevant-state [state]
  (-> (select-keys state relevant-keys)
      (assoc :split-view? (split-view? state))))

;;;; API

(defn animations [before after]
  (let [before* (relevant-state before)
        after* (relevant-state after)]
    (when-not (= before* after*)
      (filter (fn [[part anim]] (run-anim? part anim before* after*))
              part-anim-combos))))
