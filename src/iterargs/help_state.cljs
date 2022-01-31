(ns iterargs.help-state
  (:require
   [reagent.core :as r]))

(def open? (r/atom false))

;;;; API

(defn toggle! []
  (swap! open? not))
