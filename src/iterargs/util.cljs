(ns iterargs.util
  (:require
   [iterargs.dom :as dom]))

(defn prefix-id [& id-parts]
  (apply str "ia-" (map name id-parts)))

(def p prefix-id) 

(def dom-elt (comp dom/by-id p))

(defn container-id [part]
  (p part "-container"))

(def container (comp dom/by-id container-id))

(defn timeout! [interval f]
  (.setTimeout js/window f interval))

(defn suppress-bursts
  ([f interval]
   (suppress-bursts f interval (constantly false)))
  ([f interval except?]
   (let [timeout (atom nil)]
     (fn []
       (if (except?)
         (f)
         (do (when @timeout
               (.clearTimeout js/window @timeout))
             (reset! timeout
               (.setTimeout js/window
                 (fn []
                   (f)
                   (reset! timeout nil))
                 interval))))))))

(defn dangerous
  ([comp content]
   (dangerous comp nil content))
  ([comp props content]
   [comp (assoc props :dangerouslySetInnerHTML {:__html content})]))
