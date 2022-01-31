(ns iterargs.i18n)

(defmacro defmap
  ([map keys]
   `(defmap ~map ~keys string?))
  ([map keys spec]
   `(do ~@(for [k keys]
            `(clojure.spec.alpha/def ~k ~spec))
        (clojure.spec.alpha/def
          ~map
          (clojure.spec.alpha/keys :req-un ~keys)))))
