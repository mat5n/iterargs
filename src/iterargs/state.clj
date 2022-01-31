(ns iterargs.state)

(defmacro wait-guard! []
  `(do (reset! waiting true)
       (clojure.core.async/<! guard-ch)
       (reset! waiting false)))
