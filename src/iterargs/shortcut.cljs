(ns iterargs.shortcut
  (:require
   [clojure.spec.alpha]
   [goog.events]
   [goog.ui.KeyboardShortcutHandler]
   [iterargs.doc :as doc]
   [iterargs.help-state :as help-state]
   [iterargs.menu-state :as menu-state]
   [iterargs.reload :as reload]
   [iterargs.state :as state])
  (:require-macros
   [iterargs.shortcut :refer [defkb]]))

;;;; Handler

(defonce shortcut-handler
  (goog.ui.KeyboardShortcutHandler. js/document))

;;;; Helpers

(defn kb-map [kbs]
  (into {}
    (for [[id keyseq handler] (partition 3 kbs)]
      [(name id) {:keyseq keyseq :handler handler}])))

(defn goto [part]
  (state/queue! (state/goto-part part)))

;;;; Key bindings

;; Will appear in Help in the order below

(defkb key-bindings
  ::toggle-help "H" help-state/toggle!
  ::goto-odd "O" #(goto ::doc/odd)
  ::goto-even "E" #(goto ::doc/even)
  ::goto-intro "I" #(goto ::doc/intro)
  ::goto-back "B" #(goto ::doc/back)
  ::toggle-peek-back "P" #(state/queue! state/toggle-peek-back)
  ::toggle-menu "M" menu-state/toggle!
  ::toggle-hl-odd "Shift+O" #(state/queue! (state/toggle-hl ::doc/odd))
  ::toggle-hl-even "Shift+E" #(state/queue! (state/toggle-hl ::doc/even))
  ::toggle-split-pref "S" #(state/queue! state/toggle-split-pref)
  ::toggle-fix-pref "F" #(swap! menu-state/fix-pref? not)
  ::undo "Z" #(state/queue! :undo)
  ::redo "X" #(state/queue! :redo)
  ::reload "R" reload/reload!)

(def key-binding-map (kb-map key-bindings))

;;;; API

(defn init! []
  (doseq [[id {:keys [keyseq]}] key-binding-map]
    (.registerShortcut shortcut-handler id keyseq))
  (goog.events/listen
    shortcut-handler
    goog.ui.KeyboardShortcutHandler.EventType.SHORTCUT_TRIGGERED
    (fn [evt]
      (let [handler (get-in key-binding-map [(.-identifier evt) :handler])]
        (handler)))))
