(ns iterargs.navbar
  (:require
   [iterargs.i18n :refer [t]]
   [iterargs.state :as state]
   [iterargs.util :refer [dangerous p]]
   [reagent.core :as r]))

(defonce part (r/atom nil))

(defn back-symbol []
  [dangerous :span {:class (p :back-symbol)}
   "&langle;"])

(defn text [length]
  [:span {:id (p :navtext "-" length)
          :class (p :navtext)}
   (t :parts @part length)])

(defn navbar []
  [:div {:id (p :navbar)
         :on-click #(state/queue! :undo)}
   [back-symbol]
   [text :long]
   [text :short]])

(defn transition-handler! [before after]
  (reset! part (:part after)))

(defn init! []
  (reset! part (:part @state/state))
  (state/register! transition-handler!))
