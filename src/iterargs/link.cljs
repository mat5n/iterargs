(ns iterargs.link
  (:require
   [clojure.string :as str]
   [goog.style]
   [iterargs.dom :as dom]
   [iterargs.i18n :refer [t]]
   [iterargs.state :as state]
   [iterargs.util :as util :refer [p]]))

(defn local-links []
  (dom/query "a[href^=\"#\"]"))

(defn external-links []
  (dom/query "a[href*=\"//\"]"))

(defn all-links []
  (dom/query "a[href]"))

(defn link-ids [elt]
  {:source (dom/attr elt :id)
   :target (subs (dom/attr elt :href) 1)})

(defn broken-link-msg [link]
  (str (t :errors :broken-link) ": " (:target link)))

(defn broken? [link]
  (-> link :target state/elt-loc not))

(defn local-listener! [evt]
  (let [link (link-ids (.-currentTarget evt))]
    (if (broken? link)
      (js/alert (broken-link-msg link))
      (state/queue! (state/follow-link link))))
  (.preventDefault evt)
  (.stopPropagation evt))

(defn external-listener! [evt]
  (.stopPropagation evt))

(defn on-mouse-over! [evt]
  (dom/add-class! (.-currentTarget evt) (p :hover))
  (.stopPropagation evt))

(defn on-mouse-out! [evt]
  (dom/remove-class! (.-currentTarget evt) (p :hover))
  (.stopPropagation evt))

(defn clear-hover-classes! []
  (dom/remove-class! (local-links) (p :hover)))

(defn add-event-listeners! []
  (dom/listen! (all-links) :mouseover on-mouse-over!)
  (dom/listen! (all-links) :mouseout on-mouse-out!)
  (dom/listen! (external-links) :click external-listener!)
  (dom/listen! (local-links) :click local-listener!))

(defn elt-container [elt-id]
  (util/container (state/elt-loc elt-id)))

(defn broken-links []
  (->> (local-links)
       (map link-ids)
       (filter broken?)
       (map :target)))

(defn log-broken-links! []
  (when-let [bls (seq (broken-links))]
    (println "iterargs.link: there are broken internal links:"
             (str/join ", " bls))))

(defn flash-elt! [elt-id]
  (let [elt (dom/by-id elt-id)]
    (dom/add-class! elt (p :flash))
    (util/timeout! 2000 #(dom/remove-class! elt (p :flash)))
    (doseq [delay [0 500]]
      (util/timeout! delay
        #(goog.style/scrollIntoContainerView elt (elt-container elt-id) true)))))

(defn transition-handler! [before after]
  (when (not= (:doc before) (:doc after))
    (log-broken-links!)
    (add-event-listeners!))
  (when-let [link (:link after)]
    (flash-elt! (:target link))
    (when (and state/*replay* (:source link))
      (flash-elt! (:source link)))))

(defn init! []
  (log-broken-links!)
  (add-event-listeners!)
  (state/register! transition-handler!))
