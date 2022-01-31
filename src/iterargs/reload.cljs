(ns iterargs.reload
  (:require
   [goog.net.XhrIo]
   [iterargs.config :as config]
   [iterargs.doc :as doc]
   [iterargs.error :as error]
   [iterargs.state :as state]))

;;;; Helpers

(defn handle-response! [resp]
  (try
    (let [d (doc/html->document resp)]
      (println "iterargs.reload: ...document fetched, queueing transition")
      (state/queue! (state/set-doc d)))
    (catch ExceptionInfo e
      (error/handle-reload-error! e))))

(defn request-doc! [callback]
  (.send goog.net.XhrIo js/window.location.pathname
    (fn [event]
      (callback (.getResponseText (.-target event))))))

(defn transition-handler! [before after]
  (when (not= (:doc before) (:doc after))
    (config/call-hook-fns! :reload)))

;;;; API

(defn reload! []
  (println "iterargs.reload: fetching document...")
  (request-doc! handle-response!))

(defn init! []
  (state/register! transition-handler!))
