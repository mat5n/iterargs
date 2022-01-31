(ns iterargs.state
  (:require
   [clojure.core.async :refer [<! chan go-loop put!]]
   [iterargs.config :refer [cfg]]
   [iterargs.doc :as doc]
   [iterargs.history :as hist]
   [iterargs.util :as util])
  (:require-macros
   [iterargs.state :refer [wait-guard!]]))

;;;; State

(defonce guard (atom 0))

(defonce waiting (atom false))

(defonce trn-queue (chan))

(defonce guard-ch (chan))

(def init-state
  {:doc nil
   :part ::doc/odd :side ::doc/odd
   :split-pref? true :peek-back? false
   :width nil
   :hl nil :hl? {::doc/odd true ::doc/even true}
   :link nil})

(defonce state (atom init-state))

(defonce prev-state (atom nil))

(defonce transition-handlers (atom []))

(defonce history (atom (hist/bounded-history 32)))

(defonce ^:dynamic *replay* false)

;;;; Helpers

(defn call-transition-handlers! []
  (let [before @prev-state
        after @state]
    (doseq [handler @transition-handlers]
      (handler before after))))

(declare resize)

(def hist-ops {:undo hist/backward :redo hist/forward})

(defn clear-transients [state]
  (dissoc state :link :hl))

(defn transition! [trn]
  (reset! prev-state @state)
  (if (keyword? trn)
    (do (swap! history (hist-ops trn))
        (reset! state (resize (hist/now @history)))
        (binding [*replay* true]
          (call-transition-handlers!)))
    (do (swap! state (comp trn clear-transients))
        (swap! history conj @state)
        (call-transition-handlers!))))

(defn guard-up? []
  (< 0 @guard))

(defn guard-down? []
  (= 0 @guard))

(defn guard-up! []
  (swap! guard inc))

(defn guard-down! []
  (swap! guard dec)
  (when (and (guard-down?) @waiting)
    (put! guard-ch :ok)))

(defn follow-link-by-peeking? [state target-part]
  (and (= ::doc/back target-part)
       (not= ::doc/back (:part state))))

(defn can-split? [state]
  (and (<= (cfg :min-split-width) (:width state))
       (not (:split-pref? state))))

(defn follow-link-by-splitting? [state target-part]
  ;; TODO: get link as argument and figure source and target parts from that
  ;;       when no source, don't split
  (let [source-part (:part state)]
    (and (doc/argument-parts source-part)
         (= target-part (doc/opposite-side source-part))
         (can-split? state))))

;;;; API

;; TODO: 2nd arity with state
(defn elt-loc [elt-id]
  (-> @state :doc meta ::doc/locs (get elt-id)))

(defn guarded-timeout! [interval f]
  (guard-up!)
  (util/timeout! interval
    (fn []
      (f)
      (guard-down!))))

(defn register! [handler]
  (swap! transition-handlers conj handler))

(defn split-view?
  ([] (split-view? @state))
  ([state]
   (let [{:keys [part split-pref? width]} state]
     (and split-pref?
          (doc/argument-parts part)
          (<= (cfg :min-split-width) width)))))

(defn queue! [trn]
  (put! trn-queue trn))

(defn init! []
  (swap! state assoc
    :width (.-innerWidth js/window)
    :doc (doc/extract-document))
  (swap! history conj @state))

(defn transition-loop! []
  (go-loop []
    (when (guard-up?)
      (wait-guard!))
    (transition! (<! trn-queue))
    (recur)))

;;; Transitions

(defn goto-part
  "Switches to part `part`."
  [part]
  (fn [state]
    (cond-> state
      (doc/argument-parts part)
      (assoc :side part)
      (and (:peek-back? state) (= part ::doc/back))
      (assoc :peek-back? false)
      :always
      (assoc :part part))))

(defn set-split-pref
  "Sets split preference to `split-pref?` and optionally switches to `side`."
  ([split-pref?]
   (fn [state]
     (assoc state :split-pref? split-pref?)))
  ([split-pref? side]
   (fn [state]
     (-> ((set-split-pref split-pref?) state)
         ((goto-part side))))))

(defn toggle-split-pref [state]
  (update state :split-pref? not))

(defn resize
  "Sets width to `window.innerWidth`."
  [state]
  (assoc state :width (.-innerWidth js/window)))

(defn set-peek-back
  [peek-back?]
  (fn [state]
    (cond-> state
      (and peek-back? (= ::doc/back (:part state)))
      (assoc :part (:side state))
      :always
      (assoc :peek-back? peek-back?))))

(defn toggle-peek-back [state]
  ((set-peek-back (-> state :peek-back? not)) state))

(defn follow-link [link]
  (fn [state]
    (let [state* (assoc state :link link)
          target-part (-> link :target elt-loc)] ; TODO: pass state to elt-loc
      (cond (follow-link-by-peeking? state target-part)
            ((set-peek-back true) state*)
            (follow-link-by-splitting? state target-part) ; TODO: pass link
            ((set-split-pref true target-part) state*)
            :else
            ((goto-part target-part) state*)))))

(defn select-hl [hid target-side]
  (fn [state]
    (let [state* (assoc state :hl {:id hid :target-side target-side})]
      (if (can-split? state)
        ((set-split-pref true target-side) state*)
        ((goto-part target-side) state*)))))

(defn set-hl [side val]
  (fn [state]
    (assoc-in state [:hl? side] val)))

(defn toggle-hl [side]
  (fn [state]
    ((set-hl side (-> state :hl? side not)) state)))

(defn toggle-setting [setting]
  (fn [state]
    (update state setting not)))

(defn set-doc [doc]
  (fn [state]
    (assoc state :doc doc)))
