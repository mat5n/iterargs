(ns iterargs.i18n
  (:require
   [clojure.spec.alpha :as s]
   [iterargs.error :refer [throw+]])
  (:require-macros
   [iterargs.i18n :refer [defmap]]))

;;;; Texts spec

;;; Parts

(defmap ::long-and-short [::long ::short])

(defmap ::parts [:doc/intro :doc/odd :doc/even :doc/back] ::long-and-short)

;;; Menu

(defmap ::headings [::navigation ::highlights ::settings])

(defmap ::switches [::split-pref ::fix-pref])

(s/def ::menu (s/keys :req-un [::headings ::switches]))

;;; Help

(defmap ::gestures [::heading ::text])

(s/def ::keys (s/keys :req-un [::heading :iterargs.shortcut/shortcuts]))

(defmap ::version [::text ::link])

(s/def ::help (s/keys :req-un [::heading ::keys ::gestures ::version]))

;;; Errors

(defmap ::errors [::broken-link])

;;; Misc

(defmap ::misc [::endnotes])

;;; Texts

(s/def ::texts (s/keys :req-un [::parts ::menu ::help ::errors ::misc]))

;;;; State

(defonce texts (atom nil))

;;;; Private fns

(defn convert-texts []
  (try
    (js->clj js/iterargsTexts :keywordize-keys true)
    (catch js/Error err
      (throw+ :texts-missing))))

(defn drop-namespace [kw]
  (keyword (name kw)))

;;;; API

(defn t [& path]
  (or (get-in @texts (map drop-namespace path))
      (assert false (str "No such text: " path))))

(defn init! []
  (let [txt (convert-texts)]
    (if (s/valid? ::texts txt)
      (reset! texts txt)
      (throw+ :invalid-texts (s/explain-str ::texts txt)))))
