(ns iterargs.dom)

(defmacro doelts [& body]
  `(doseq [~'elt (elt-seq ~'elts)]
     ~@body))
