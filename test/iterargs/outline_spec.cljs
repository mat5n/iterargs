(ns iterargs.outline-spec
  (:require [iterargs.outline :as outline]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test.check]
            [clojure.test.check.properties]
            [clojure.spec.test.alpha :as stest]))

(s/def ::id string?)

(s/def ::level (s/int-in 1 7))

(s/def ::text string?)

(s/def ::heading (s/keys :req-un [::id ::level ::text]))

(defn rand-range [a b]
  (+ a (rand-int (inc (- b a)))))

(defn valid-next-level [lev]
  (rand-range 2 (min 6 (inc lev))))

(defn valid-progression []
  (iterate valid-next-level 1))

(defn valid-levels [headings]
  (map #(assoc %1 :level %2) headings (valid-progression)))

(s/def ::headings
  (s/with-gen
    (s/& (s/+ ::heading) outline/valid-progression?)
    #(gen/fmap valid-levels (s/gen (s/+ ::heading)))))

(s/def ::node (s/keys :req-un [::id ::level ::text] :opt-un [::children]))

(defn level? [nodes]
  (let [lev (-> nodes first :level)]
    (every? #(= lev (:level %)) nodes)))

(s/def ::node-list (s/& (s/+ ::node) level?))

(s/def ::children ::node-list)

(defn tree->list [node]
  (cons (dissoc node :children)
        (mapcat tree->list (:children node))))

(s/fdef iterargs.outline/heading-tree
  :args (s/cat :headings (s/spec ::headings))
  :ret ::node
  :fn #(= (-> % :args :headings) (tree->list (:ret %))))

(def opts {:clojure.spec.test.check/opts {:num-tests 20}})

(defn check []
  (stest/check 'iterargs.outline/heading-tree opts))
