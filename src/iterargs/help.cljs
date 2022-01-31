(ns iterargs.help
  (:require
   [clojure.string :as str]
   [iterargs.gesture :as gesture]
   [iterargs.help-state :as help-state]
   [iterargs.i18n :refer [t]]
   [iterargs.icon :as icon]
   [iterargs.shortcut :as shortcut]
   [iterargs.util :as util :refer [p]]
   [medley.core :refer [indexed]]))

(defn keyseq [s]
  (let [keys (str/split s #"\+")]
    [:span {:class (p :keyseq)}
     [:kbd (first keys)]
     (for [[i k] (indexed (next keys))]
       ^{:key i}
       [:<>
        [:span {:class (p :plus)} " + "]
        [:kbd k]])]))

(defn shortcut-table []
  [:table
   [:tbody
    (for [[id kb _] (partition 3 shortcut/key-bindings)]
      ^{:key id}
      [:tr
       [:td [keyseq kb]]
       [:td ":"]
       [:td (t :help :keys :shortcuts id)]])]])

(defn version []
  [:p {:id (p :version)}
   [:a {:href (t :help :version :link)
        :target "_blank"}
    (t :help :version :text)]])

(defn help-content []
  (let [[start end] (gesture/swipe-recogniser ::help)]
    (fn []
      [:div {:id (p :help)
             :on-touch-start start :on-touch-end end}
       [:h2 (t :help :gestures :heading)]
       [:p (t :help :gestures :text)]
       [:h2 (t :help :keys :heading)]
       [shortcut-table]
       [version]])))

(def help-button-opts {:size 20 :thickness 2})

(defn help-button []
  (let [{:keys [size]} help-button-opts]
    [:div {:id (p :help-button)
           :style {:width size :height size}
           :on-click help-state/toggle!}
     [icon/cross help-button-opts]]))

(defn help []
  [:div {:id (p :help-container)
         :class (when @help-state/open? (p :open))}
   [help-content]
   [help-button]])
