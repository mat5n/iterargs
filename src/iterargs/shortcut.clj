(ns iterargs.shortcut)

(defmacro defkb [var-name & forms]
  (let [ids (map first (partition 3 forms))]
    `(do (def ~var-name [~@forms])
         ~@(for [id ids]
             `(clojure.spec.alpha/def ~id string?))
         (clojure.spec.alpha/def ::shortcuts
           (clojure.spec.alpha/keys :req-un ~ids)))))
