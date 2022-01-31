(ns iterargs.multi)

(defmacro defmethod+ [mm-name params & dval-body-pairs]
  `(do ~@(for [[dval body] (partition 2 dval-body-pairs)]
           `(defmethod ~mm-name ~dval
              ~params
              ~body))))
