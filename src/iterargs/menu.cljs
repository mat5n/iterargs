(ns iterargs.menu
  (:require
   [iterargs.doc :as doc]
   [iterargs.gesture :as gesture]
   [iterargs.help-state :as help-state]
   [iterargs.i18n :refer [t]]
   [iterargs.icon :as icon]
   [iterargs.menu-state :as menu-state :refer [state]]
   [iterargs.state :as state]
   [iterargs.util :as util :refer [p]]
   [reagent.core :as r]))

;;;; Cursors

(defonce part (r/cursor state [:part]))
(defonce doc (r/cursor state [:doc]))
(defonce hl-on (r/cursor state [:hl?]))
(defonce split-pref? (r/cursor state [:split-pref?]))

;;;; Components

(def menu-button-opts {:width 14 :height 14 :thickness 2})

(defn menu-button []
  (let [{:keys [width height]} menu-button-opts]
    [:div {:id (p :menu-button)
           :style {:width width :height height}
           :class (when @menu-state/open? (p :open))
           :on-click menu-state/toggle!}
     [icon/burger menu-button-opts]]))

(defn section-attrs [id]
  {:id (p id "-section") :class (p :menu-section)})

(defn heading [heading-id]
  [:div {:class (p :menu-heading)}
   (t :menu :headings heading-id)])

(defn nav-handler [part node sel?]
  (when (not sel?)
    (if (and node (< 1 (:level node)))
      #(state/queue! (state/follow-link {:target (:id node)}))
      #(state/queue! (state/goto-part part)))))

(defn nav-text [part node sel?]
  [:div {:class [(p :nav-text) (p :level (str (:level node)))]
         :on-click (nav-handler part node sel?)}
   (if (and node (< 1 (:level node)))
     (:text node)
     (t :parts part :long))])

(defn nav-button [expand?]
  [:div {:class (p :nav-button)
         :on-click #(swap! expand? not)}
   (if @expand? "−" "+")])

(defn nav-parent [part node sel? expand?]
  [:div {:class [(p :menu-item) (when sel? (p :nav-on))]}
   [nav-text part node sel?]
   (when (:children node)
     [nav-button expand?])])

(declare nav-item)

(defn nav-children [part nodes]
  [:<>
   (for [node nodes]
     ^{:key (:id node)}
     [nav-item part node false])])

(defn nav-item [part node sel?]
  (let [expand? (r/atom false)]
    (fn [part node sel?]
      [:<>
       [nav-parent part node sel? expand?]
       (when @expand?
         [nav-children part (:children node)])])))

(defn help []
  [:div {:class (p :menu-item)}
   [:div {:class (p :nav-text)
          :on-click help-state/toggle!}
    (t :help :heading)]])

(defn navigation []
  [:div (section-attrs :nav)
   [heading :navigation]
   (let [pt* @part
         outline (-> @doc meta ::doc/outline)]
     [:<>
      (for [pt doc/parts]
        ^{:key pt}
        [nav-item pt (outline pt) (= pt pt*)])])
   [help]])

(defn switch []
  [:div {:class (p :switch)}
   [:span {:class (p :slider)}]])

(defn hl-ctrl-id [side]
  (p side "-hl-ctrl"))

(defn ctrl-classes [on?]
  [(p :menu-item) (when on? (p :ctrl-on))])

(defn hl-ctrl [side on?]
  [:div {:id (hl-ctrl-id side)
         :class (ctrl-classes on?)
         :on-click #(state/queue! (state/toggle-hl side))}
   (t :parts side :long)
   [switch]])

(defn highlights []
  [:div (section-attrs :hl)
   [heading :highlights]
   (let [hl? @hl-on]
     (for [side doc/argument-parts]
       ^{:key side}
       [hl-ctrl side (hl? side)]))])

(defn split-setting []
  [:div {:class (ctrl-classes @split-pref?)
         :on-click #(state/queue! (state/toggle-setting :split-pref?))}
   (t :menu :switches :split-pref)
   [switch]])

(defn fix-setting []
  [:div {:class (ctrl-classes @menu-state/fix-pref?)
         :on-click #(swap! menu-state/fix-pref? not)}
   (t :menu :switches :fix-pref)
   [switch]])

(defn settings []
  [:div (section-attrs :set)
   [heading :settings]
   [split-setting]
   [fix-setting]])

(defn menu-classes []
  (cond-> [(p :container)]
    @menu-state/open? (conj (p :open))
    (menu-state/sliding?) (conj (p :sliding))
    @menu-state/fix-pref? (conj (p :fixed))))

(defn menu []
  (let [[start end] (gesture/swipe-recogniser ::menu)]
    (fn []
      [:div {:id (p ::menu)
             :class @(r/track menu-classes)
             :on-touch-start start :on-touch-end end}
       [navigation]
       [highlights]
       [settings]])))

;;;; Transition handler

(defn transition-handler! [before after]
  (reset! state after))

;;;; API

(defn init! []
  (reset! state @state/state)
  (state/register! transition-handler!))
