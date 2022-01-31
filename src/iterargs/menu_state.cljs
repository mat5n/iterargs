(ns iterargs.menu-state
  (:require
   [iterargs.util :as util]
   [reagent.core :as r]))

(def slide-duration 500)

(defonce state (r/atom nil))

(defonce open? (r/atom false))
(defonce slide-count (r/atom 0))
(defonce fix-pref? (r/atom false))

;;;; API

(defn toggle! []
  (swap! slide-count inc)
  (util/timeout! 0
    (fn []
      (r/flush)
      (swap! open? not)
      (util/timeout! slide-duration
        (fn [] (swap! slide-count dec))))))

(defn sliding? []
  (< 0 @slide-count))

(defn fixed? []
  (deref fix-pref?)
  (and @open? @fix-pref?))
