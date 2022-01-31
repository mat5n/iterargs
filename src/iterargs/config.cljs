(ns iterargs.config
  (:require
   [clojure.spec.alpha :as s]
   [iterargs.error :refer [throw+]]
   [medley.core :refer [deep-merge]]))

;;;; Config spec

(s/def ::fn-list (s/+ fn?))

(s/def ::init ::fn-list)

(s/def ::reload ::fn-list)

(s/def ::hooks (s/keys :opt-un [::init ::reload]))

(s/def ::gen string?)

(s/def ::min-split-width pos-int?)

(s/def ::config (s/keys :req-un [::gen ::min-split-width] :opt-un [::hooks]))

;;;; Constants and state

(def defaults
  {:gen "pandoc"
   :min-split-width 1000})

(defonce config (atom nil))

;;;; Private fns

(defn convert-config []
  (try
    (let [c (js->clj js/iterargsConfig :keywordize-keys true)]
      (println "iterargs.config: user config found")
      c)
    (catch js/Error err
      (println "iterargs.config: user config not found")
      {})))

;;;; API

(defn cfg [& path]
  (get-in @config path))

(defn call-hook-fns! [hook]
  (doseq [f (cfg :hooks hook)]
    (f)))

(defn init! []
  (let [c (deep-merge defaults (convert-config))]
    (if (s/valid? ::config c)
      (reset! config c)
      (throw+ :invalid-config (s/explain-str ::config c)))))
