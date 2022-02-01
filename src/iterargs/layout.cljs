(ns ^:figwheel-hooks iterargs.layout
  (:require
   [iterargs.animation :as anim]
   [iterargs.config :as config :refer [cfg]]
   [iterargs.doc :as doc]
   [iterargs.dom :as dom]
   [iterargs.gesture :as gesture]
   [iterargs.help :as help]
   [iterargs.highlight :as highlight]
   [iterargs.icon :as icon]
   [iterargs.link :as link]
   [iterargs.menu :as menu]
   [iterargs.menu-state :as menu-state]
   [iterargs.navbar :as navbar]
   [iterargs.state :as state]
   [iterargs.util :as util :refer [container-id dom-elt p]]
   [reagent.core :as r]
   [reagent.dom :as rd])
  (:require-macros
   [iterargs.multi :refer [defmethod+]]))

;;;; State

(defonce state (r/atom nil))

;;;; Visibility

(defmulti visible? identity)

(defmethod+ visible? [part]
  ::doc/argument-part
  (or (= part (:part @state))
      (state/split-view? @state))

  ::doc/support-part
  (= part (:part @state))

  ::doc/back
  (or (= part (:part @state))
      (:peek-back? @state)))

;;;; Components and their helpers

(defn container-classes [part]
  (doall (map p
    (if-let [anim (get-in @state [:anims part])]
      [:container :anim anim]
      (cond-> [:container]
        (not (visible? part))
        (conj :hidden))))))

(defn container [part]
  (let [[start end] (gesture/swipe-recogniser part)]
    (fn [part]
      [:div {:id (container-id part)
             :class @(r/track container-classes part)
             :on-touch-start start :on-touch-end end}
       [:div {:id (p part)
              :class (p :article)}]])))

(defn peek-back-close-listener []
  (state/queue! (state/set-peek-back false)))

(def peek-close-opts {:size 14 :thickness 2})

(defn controllers []
  (let [{:keys [size]} peek-close-opts]
    [:div {:id (p :peek-back-close)
           :style {:width size :height size}
           :on-click peek-back-close-listener}
     [icon/cross peek-close-opts]]))

(def main-class-predicates
  [[state/split-view? :split]
   [:peek-back? :peek]
   [:anims :anim-playing]
   [menu-state/fixed? :gap]])

(defn main-classes []
  (doall
   (for [[pred? class] main-class-predicates
         :when (pred? @state)]
     (p class))))

(defn main []
  [:main {:class @(r/track main-classes)}
   [:<>
    (for [part doc/parts]
      ^{:key part}
      [container part])]
   [controllers]
   [navbar/navbar]])

(defn root []
  [:<>
   [main]
   [menu/menu]
   [menu/menu-button]
   [help/help]])

;;;; Other fns

(defn insert-doc! []
  (let [doc (:doc @state)]
    (doseq [part doc/parts]
      (dom/set-html! (dom-elt part) (doc part)))))

(defn transition-handler! [before after]
  (if (not= (:doc before) (:doc after))
    (do (reset! state after)
        (insert-doc!))
    (when-let [anims (seq (anim/animations before after))]
      (swap! state assoc :anims (into {} anims))
      (state/guarded-timeout!
        (if (= 1 (count anims)) 500 1000)
        (fn []
          (reset! state after)
          (r/flush))))))

(defn resize-immediately? []
  (let [msw (cfg :min-split-width)
        w1 (:width @state/state)
        w2 (.-innerWidth js/window)]
    (or (and (>= w1 msw) (> msw w2))
        (and (< w1 msw) (<= msw w2)))))

(def resize-listener
  (util/suppress-bursts
    #(state/queue! state/resize)
    200
    resize-immediately?))

(defn add-resize-listener! []
  (.addEventListener js/window "resize" resize-listener))

(defn show-content! []
  (when-let [elt (dom-elt :loading)]
    (dom/destroy! elt))
  (dom/remove-attr! (dom-elt :doc) :style))

(defn mount-root! []
  (rd/render [root] (dom-elt :doc)))

(defn ^:after-load on-reload []
  (mount-root!)
  (insert-doc!)
  (link/reinit!)
  (highlight/reinit!)
  (config/call-hook-fns! :reload))

;;;; API

(defn init! []
  (reset! state @state/state)
  (mount-root!)
  (insert-doc!)
  (state/register! transition-handler!)
  (add-resize-listener!)
  (show-content!))
