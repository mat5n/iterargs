(ns iterargs.pandoc
  (:require
   [clojure.zip :as zip]
   [hickory.select :as sel]
   [hickory.zip :refer [hickory-zip]]
   [iterargs.i18n :refer [t]]
   [iterargs.preproc :as preproc :refer [select-and-edit]]
   [iterargs.util :refer [p]]))

(def footnote-sel (sel/and (sel/tag :section) (sel/class :footnotes)))

(def footnote-hr-sel
  (sel/child footnote-sel (sel/tag :hr)))

(defn hr->heading [helt]
  (assoc helt
         :attrs {:id (p :endnotes)}
         :tag :h2
         :content [(t :misc :endnotes)]))

(defn replace-endnote-rule-with-heading [helt]
  (select-and-edit helt footnote-hr-sel hr->heading))

(defn next-loc [loc sel]
  (sel/select-next-loc sel loc))

(defn ordered-locs [helt sel1 sel2]
  (when-let [loc1 (next-loc (hickory-zip helt) sel1)]
    (when-let [loc2 (next-loc (zip/next loc1) sel2)]
      [loc1 loc2])))

(defn swap-elements [helt sel1 sel2]
  (if-let [[loc1 loc2] (ordered-locs helt sel1 sel2)]
    (-> (zip/replace loc1 (zip/node loc2))
        (zip/next)
        (next-loc sel2)
        (zip/replace (zip/node loc1))
        (zip/root))
    helt))

(def ref-sel (sel/and (sel/tag :div) (sel/id :refs)))

(defn swap-endnotes-and-references [helt]
  (swap-elements helt ref-sel footnote-sel))

(defmethod preproc/specific "pandoc"
  [helt gen]
  (-> (replace-endnote-rule-with-heading helt)
      (swap-endnotes-and-references)))
