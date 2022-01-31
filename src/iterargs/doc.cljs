(ns iterargs.doc
  (:require
   [hickory.core :as hick]
   [hickory.render :refer [hickory-to-html]]
   [hickory.select :as sel]
   [iterargs.dom :as dom]
   [iterargs.error :refer [throw+]]
   [iterargs.outline :refer [outline]]
   [iterargs.pandoc]
   [iterargs.preproc :refer [preprocess]]
   [iterargs.util :refer [dom-elt p]]
   [medley.core :refer [map-vals]]))

;;;; Document hierarchy

(derive ::argument-part ::part)
(derive ::support-part ::part)

(derive ::odd ::argument-part)
(derive ::even ::argument-part)

(derive ::intro ::support-part)
(derive ::back ::support-part)

;;; Some handy precomputed vecs & sets

(def parts [::intro ::odd ::even ::back])

(defn category [cat]
  (set (filter #(isa? % cat) parts)))

(def support-parts (category ::support-part))

(def argument-parts (category ::argument-part))

;;;; Helpers

(defn htmlfrag->helt [html]
  (-> (hick/parse-fragment html)
      (first)
      (hick/as-hickory)))

(defn html->helt [html]
  (hick/as-hickory (hick/parse html)))

(defn doc-elt [helt]
  (or (first (sel/select (sel/id (p :doc)) helt))
      (throw+ :doc-missing)))

(defn helts->html [helts]
  (apply str (map hickory-to-html helts)))

(defn extract-doc-html []
  (if-let [elt (dom-elt :doc)]
    (str "<div>" (dom/html elt) "</div>")
    (throw+ :doc-missing)))

(defn h1? [node]
  (and (map? node)
       (= (:type node) :element)
       (= (:tag node) :h1)))

(def conjv (fnil conj []))

(defn group-by+ [f g init coll]
  (second
    (reduce
      (fn [[state m] elt]
        (let [state* (f state elt)
              k (g state*)]
          [state* (update m k conjv elt)]))
      [init {}]
      coll)))

(defn helt->parts [helt]
  (let [parts* (vec (concat [::bin] parts [::bin]))
        max-part-num (dec (count parts*))]
    (group-by+
      (fn [part-num elt]
        (if (and (h1? elt) (< part-num max-part-num))
          (inc part-num)
          part-num))
      parts*
      0
      (:content helt))))

(defn ids [helt]
  (map (comp :id :attrs)
       (sel/select (sel/attr :id) helt)))

(defn locations [parts]
  (apply merge
    (for [[part-name helts] parts]
      (zipmap (mapcat ids helts)
              (repeat part-name)))))

(defn helt->doc [helt]
  (let [pts (-> (preprocess helt)
                (helt->parts)
                (dissoc ::bin))]
    (if (= 4 (count pts))
      (with-meta (map-vals helts->html pts)
        {::outline (outline pts)
         ::locs (locations pts)})
      (throw+ :parts-missing))))

;;;; API

(defn opposite-side [side]
  (if (= side ::odd) ::even ::odd))

(defn extract-document []
  (-> (extract-doc-html) htmlfrag->helt helt->doc))

(defn html->document [html]
  (-> html html->helt doc-elt helt->doc))
