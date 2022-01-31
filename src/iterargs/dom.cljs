(ns iterargs.dom
  "A minimal DOM manipulation library."
  (:require
   [clojure.string :as str]
   [goog.dom :as gdom]
   [goog.dom.classlist :as gcl]
   [goog.events :as gevt])
  (:require-macros
   [iterargs.dom :refer [doelts]]))

;;;; Helpers

(defn elt-seq [elts]
  (if (seqable? elts) elts (list elts)))

;;;; Queries

(defn by-id [elt-id]
  (gdom/getElement (name elt-id)))

(defn query
  ([sel]
   (array-seq (js/document.querySelectorAll sel)))
  ([elt sel]
   (array-seq (.querySelectorAll elt sel))))

(defn attr [elt aname]
  (.getAttribute elt (name aname)))

(defn html [elt]
  (.-innerHTML elt))

(defn ancestor? [elt1 elt2]
  (= elt1 (gdom/findCommonAncestor elt1 elt2)))

;;;; Manipulation

(defn add-class! [elts class]
  (doelts (gcl/add elt class)))

(defn remove-class! [elts class]
  (doelts (gcl/remove elt class)))

(defn set-classes! [elts classes]
  (let [class-str (if (coll? classes) (str/join " " classes) classes)]
    (doelts (gcl/set elt class-str))))

(defn remove-attr! [elts aname]
  (doelts (.removeAttribute elt (name aname))))

(defn set-text! [elts text]
  (doelts (gdom/setTextContent elt text)))

(defn set-html! [elts html]
  (doelts (set! (.-innerHTML elt) html)))

(defn destroy! [elts]
  (doelts (gdom/removeNode elt)))

;;;; Events

(defn listen! [elts type listener]
  (let [type (name type)]
    (doelts (gevt/listen elt type listener))))
