(ns iterargs.help-state
  (:require
   [reagent.core :as r]))

(defonce open? (r/atom false))

;;;; API

(defn toggle! []
  (swap! open? not))
