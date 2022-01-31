(ns iterargs.outline-test
  (:require [iterargs.outline :as outline]
            [iterargs.doc :as doc]
            [cljs.test :refer-macros [deftest is are]]))

(defn hdom-elt [tag]
  {:tag tag})

(deftest heading-level
  (are [tag level]
      (= level (outline/heading-level (hdom-elt tag)))
    :h1 1 :h2 2 :h3 3 :h4 4 :h5 5 :h6 6))

(deftest stringify
  (are [html text]
      (= text (-> html doc/htmlfrag->helt outline/stringify))

    "<h2>goo <b><i>foo</i>bar</b> zoo</h2>"
    "goo foobar zoo"

    "<h1>plain text</h1>"
    "plain text"

    "<h4></h4>"
    ""
    ))

(defn headings [levels]
  (map (fn [lev] {:level lev}) levels))

(deftest valid-progression
  (are [levels valid?]
      (= valid? (outline/valid-progression? (headings levels)))

    [1 2 2 3 3 2 3 2 3 3 4 4 3 3 4 5 2 2] true
    [1 2 2 3 3 2 1 2 2 3 3 4 4 3 3 4 5 2 2] false
    [1 2 2 4 3 2 3 2 3 3 4 4 3 3 4 4 2 2] false
    [1 2 3 4 5 6 5 4 3 2 2] true
    [1 2 3 3 5 6 5 4 3 2 2] false
    ))

(deftest heading-tree
  (are [levels tree]
      (= tree (outline/heading-tree (headings levels)))

    [1 2 2 3 4 3 3 2]
    '{:level 1, :children ({:level 2} {:level 2, :children ({:level 3, :children ({:level 4})} {:level 3} {:level 3})} {:level 2})}
    ))
