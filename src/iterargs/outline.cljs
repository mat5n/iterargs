(ns iterargs.outline
  (:require
   [hickory.select :as sel]))

;;;; Helpers

(def heading-sel
  (apply sel/or
         (map #(sel/tag (str "h" %))
              (range 1 7))))

(defn heading-level [helt]
  (-> helt :tag name (get 1) js/parseInt))

(defn strings [helt]
  (if (string? helt)
    [helt]
    (mapcat strings (:content helt))))

(defn stringify [helt]
  (or (-> helt :attrs :alt)
      (apply str (strings helt))))

(defn helt->heading [helt]
  {:id (-> helt :attrs :id)
   :level (heading-level helt)
   :text (stringify helt)})

(defn extract-headings [helts]
  (->> (mapcat (partial sel/select heading-sel) helts)
       (map helt->heading)))

(defn valid-progression? [headings]
  (let [levels (map :level headings)]
    (and (= 1 (first levels))
         (every? #(<= 2 % 6) (rest levels))
         (every? (fn [[a b]] (>= (inc a) b))
                 (partition 2 1 levels)))))

(declare children)

(defn node [[head & tail]]
  (let [[nodes ntail] (children tail (inc (:level head)))]
    (if (seq nodes)
      [(assoc head :children nodes) ntail]
      [head tail])))

(defn children [headings level]
  (loop [[head & tail :as elts] headings nodes []]
    (if (= level (:level head))
      (let [[n ntail] (node elts)]
        (recur ntail (conj nodes n)))
      [nodes elts])))

(defn heading-tree [headings]
  (first (node headings)))

(defn print-progression-warning! [side]
  (println "iterargs.outline: invalid heading progression in"
           (str (name side))))

;;;; API

(defn outline
  ([parts]
   (into {}
     (for [[part helts] parts :let [ol (outline part helts)] :when ol]
       [part ol])))
  ([part helts]
   (let [headings (extract-headings helts)]
     (if (valid-progression? headings)
       (heading-tree headings)
       (do (print-progression-warning! part) nil)))))
