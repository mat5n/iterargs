(ns iterargs.error
  (:require
   [iterargs.dom :as dom]
   [iterargs.util :as util :refer [dangerous dom-elt p]]
   [reagent.dom :as rd]))

;;;; Messages

(def messages
  {:doc-missing
   ["Document div missing"
    "The document has to be in a div element with the id <code>ia-doc</code>."]
   :parts-missing
   ["Parts of document missing"
    "The document has to have four first-level headings."]
   :texts-missing
   ["Texts missing"
    (str "The various bits of text shown in the UI have to be specified "
         "in the variable <code>iterargsTexts</code>.")]
   :invalid-config
   ["Invalid config"
    "Config did not pass validation. See helpful details below."]
   :invalid-texts
   ["Invalid texts"
    "Texts did not pass validation. See helpful details below."]})

;;;; Components

(defn error-view [e]
  (let [[head msg] (-> e ex-data :type messages)]
    [:div {:id (p :error) :class (p :article)}
     [:h1 "Error"]
     (dangerous :h2 head)
     (dangerous :p msg)
     (when-let [details (-> e ex-data :details)]
       [:<>
        [:h2 "Details"]
        [:pre details]])]))

;;;; Other fns

(defn insert-error-container! []
  (dom/set-html!
    (dom/query "body")
    "<div id=\"ia-error-container\" class=\"ia-container\"></div>"))

;;;; API

(defn handle-init-error! [e]
  (insert-error-container!)
  (rd/render [error-view e] (dom-elt :error-container)))

(defn handle-reload-error! [e]
  (js/alert (str "Error: " (-> e ex-data :type messages first))))

(defn throw+
  ([err-type]
   (throw+ err-type nil))
  ([err-type details]
   (let [data (cond-> {:type err-type}
                details
                (assoc :details details))]
     (throw (ex-info (str "Error: " (name err-type)) data)))))
