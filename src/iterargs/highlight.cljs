(ns iterargs.highlight
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [goog.style]
   [iterargs.doc :as doc]
   [iterargs.dom :as dom]
   [iterargs.link :as link]
   [iterargs.state :as state]
   [iterargs.util :as util :refer [p dom-elt]]))

(defn side-fn [odd-val even-val]
  (fn [side]
    (if (= side ::doc/odd)
      odd-val
      even-val)))

(def hl-attr (side-fn "data-hl-odd" "data-hl-even"))
(def hl-sel  (side-fn "[data-hl-odd]" "[data-hl-even]"))

(defn side-class-fn [class]
  (side-fn (p class "-odd") (p class "-even")))

(def hl-on-class  (side-class-fn :hl-on))
(def hl-hov-class (side-class-fn :hl-hov))
(def hl-sel-class (side-class-fn :hl-sel))

(defn label [hl-elt owner]
  (dom/attr hl-elt (hl-attr owner)))

(defn hl-on? [owner]
  (get-in @state/state [:hl? owner]))

;; Even takes precedence in double highlights (= elements that have
;; been highlighted by both Odd and Even)

(defn hl-id
  ([hl-elt]
   (or (hl-id hl-elt ::doc/even)
       (hl-id hl-elt ::doc/odd)))
  ([hl-elt owner]
   (when-let [labl (label hl-elt owner)]
     (when (hl-on? owner)
       {:owner owner :label labl}))))

(defn all-hl-elts []
  (dom/query "[data-hl-odd], [data-hl-even]"))

(defn hl-elts-by-hid-sel [hid]
  (str "[" (-> hid :owner hl-attr) "=\"" (:label hid) "\"]"))

(defn hl-elts-by-hid
  ([hid]
   (dom/query (hl-elts-by-hid-sel hid)))
  ([hid side]
   (dom/query (util/container side) (hl-elts-by-hid-sel hid))))

(defn hl-elts-by-owner
  ([owner]
   (dom/query (hl-sel owner)))
  ([owner side]
   (dom/query (util/container side) (hl-sel owner))))

(defn part-of? [part elt]
  (dom/ancestor? (util/container part) elt))

(defn event-side [evt]
  (let [elt (.-currentTarget evt)]
    (cond (part-of? ::doc/odd elt)  ::doc/odd
          (part-of? ::doc/even elt) ::doc/even
          :else (assert false "Highlight event occurred where it shouldn't have"))))

(defn click-listener! [evt]
  (when-let [hid (hl-id (.-currentTarget evt))]
    (state/queue!
      (state/select-hl hid (-> evt event-side doc/opposite-side)))
    (.preventDefault evt)
    (.stopPropagation evt)))

(defn on-mouse-over! [evt]
  (when-let [hid (hl-id (.-currentTarget evt))]
    (dom/add-class! (hl-elts-by-hid hid) (hl-hov-class (:owner hid)))
    (.stopPropagation evt)))

(defn on-mouse-out! [evt]
  (when-let [hid (hl-id (.-currentTarget evt))]
    (dom/remove-class! (hl-elts-by-hid hid) (hl-hov-class (:owner hid)))
    (.stopPropagation evt)))

(defn add-event-listeners! []
  (dom/listen! (all-hl-elts) :mouseover on-mouse-over!)
  (dom/listen! (all-hl-elts) :mouseout on-mouse-out!)
  (dom/listen! (all-hl-elts) :click click-listener!))

(defn scroll-many-into-view! [elts parent center?]
  (goog.style/scrollIntoContainerView (last elts) parent center?)
  (goog.style/scrollIntoContainerView (first elts) parent))

(defn scroll-into-view! [elt parent]
  (goog.style/scrollIntoContainerView elt parent true))

(defn scroll-to-highlights! [hid target-side]
  (doseq [side doc/argument-parts delay [0 500]]
    (when (or state/*replay* (= side target-side))
      (util/timeout! delay
        #(scroll-into-view!
           (first (hl-elts-by-hid hid side))
           (util/container side))))))

(defn turn-hl-on? [owner before after]
  (and (or (not before)
           (not (-> before :hl? owner)))
       (-> after :hl? owner)))

(defn turn-hl-off? [owner before after]
  (turn-hl-on? owner after before))

(defn clear-hover-classes! []
  (link/clear-hover-classes!)
  (doseq [owner doc/argument-parts]
    (dom/remove-class!
      (hl-elts-by-owner owner)
      (hl-hov-class owner))))

(defn update-classes! [before after]
  (when-let [{:keys [id]} (:hl after)]
    (let [elts (hl-elts-by-hid id)
          sel-class (hl-sel-class (:owner id))]
      (dom/add-class! elts sel-class)
      (util/timeout! 2000 #(dom/remove-class! elts sel-class))))
  (doseq [owner doc/argument-parts]
    (cond (turn-hl-on? owner before after)
          (do (clear-hover-classes!)
              (dom/add-class! (dom-elt :doc) (hl-on-class owner)))
          (turn-hl-off? owner before after)
          (do (clear-hover-classes!)
              (dom/remove-class! (dom-elt :doc) (hl-on-class owner))))))

(defn stale-hl-summary [stale-hls owner]
  (str (if (= owner ::doc/odd) "Odd" "Even")
       " has stale highlights: "
       (str/join ", " stale-hls)))

(defn symmetric-difference [a b]
  (set/difference
    (set/union a b)
    (set/intersection a b)))

(defn hl-labels [owner side]
  (->> (hl-elts-by-owner owner side)
       (map #(label % owner))
       (set)))

(defn remove-stale-highlights!
  ([owner]
   (let [odd (hl-labels owner ::odd)
         even (hl-labels owner ::even)
         stale (symmetric-difference odd even)]
     (when (seq stale)
       (println "iterargs.highlight:" (stale-hl-summary stale owner))
       (doseq [label stale
               :let [hid {:owner owner :label label}]]
         (dom/remove-attr! (hl-elts-by-hid hid) (hl-attr owner))))))
  ([]
   (doseq [owner doc/argument-parts]
     (remove-stale-highlights! owner))))

(defn transition-handler! [before after]
  (if (= (:doc before) (:doc after))
    (update-classes! before after)
    (do (remove-stale-highlights!)
        (update-classes! before after)
        (add-event-listeners!)))
  (when (:hl after)
    (scroll-to-highlights!
      (-> after :hl :id)
      (-> after :hl :target-side))))

(defn init! []
  (remove-stale-highlights!)
  (add-event-listeners!)
  (state/register! transition-handler!)
  (update-classes! nil @state/state))
