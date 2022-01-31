(ns iterargs.preproc
  (:require
   [clojure.zip :as zip]
   [hickory.select :as sel]
   [hickory.zip :refer [hickory-zip]]
   [iterargs.config :refer [cfg]]
   [iterargs.util :refer [p]]))

(defn select-and-edit [helt selector edit]
  (loop [loc (hickory-zip helt)]
    (if-let [sel-loc (sel/select-next-loc selector loc)]
      (recur (-> sel-loc (zip/edit edit) zip/next))
      (zip/root loc))))

(defn internal-link? [url]
  (= "#" (first url)))

(def external-link? (comp not internal-link?))

(def ext-link-sel
  (sel/and (sel/tag :a)
           (sel/attr :href external-link?)))

(defn add-target [helt]
  (->> (assoc (:attrs helt) :target "_blank" :rel "noopener noreferrer")
       (assoc helt :attrs)))

(defn add-target-to-external-links [helt]
  (select-and-edit helt ext-link-sel add-target))

(def int-link-wo-id-sel
  (sel/and (sel/tag :a)
           (sel/attr :href internal-link?)
           (sel/not (sel/attr :id))))

(defn add-id [helt id]
  (assoc-in helt [:attrs :id] id))

(defn link-id-maker []
  (let [n (atom 0)]
    (fn []
      (swap! n inc)
      (p "ref" (str @n)))))

(defn add-id-to-internal-links [helt]
  (let [make-link-id (link-id-maker)]
    (select-and-edit helt int-link-wo-id-sel #(add-id % (make-link-id)))))

(defn common [helt]
  (-> (add-target-to-external-links helt)
      (add-id-to-internal-links)))

(defmulti specific (fn [helt gen] gen))

(defmethod specific :default
  [helt gen]
  helt)

;;;; API

(defn preprocess [helt]
  (-> (common helt)
      (specific (cfg :gen))))
