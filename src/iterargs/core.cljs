(ns iterargs.core
  (:require
   [iterargs.config]
   [iterargs.error]
   [iterargs.highlight]
   [iterargs.i18n]
   [iterargs.layout]
   [iterargs.link]
   [iterargs.menu]
   [iterargs.navbar]
   [iterargs.reload]
   [iterargs.shortcut]
   [iterargs.state]))

(enable-console-print!)

(defn initialise-modules! []
  (iterargs.config/init!)
  (iterargs.i18n/init!)
  (iterargs.state/init!)
  (iterargs.menu/init!)
  (iterargs.navbar/init!)
  (iterargs.layout/init!)
  (iterargs.link/init!)
  (iterargs.highlight/init!)
  (iterargs.shortcut/init!)
  (iterargs.reload/init!))

(defn init! []
  (try
    (println "iterargs.core: begin initialising...")
    (initialise-modules!)
    (iterargs.config/call-hook-fns! :init)
    (println "iterargs.core: ...initialising done")
    (iterargs.state/transition-loop!)
    (catch ExceptionInfo e
      (iterargs.error/handle-init-error! e))))

(defonce init-call (init!))
