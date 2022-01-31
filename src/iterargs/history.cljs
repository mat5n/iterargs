(ns iterargs.history)

(defprotocol History
  "A history is a sequence of states combined with a pointer that can be
  moved backward and forward along the sequence."
  (forward [this] "Moves to the next state in history.")
  (backward [this] "Moves to the previous state in history.")
  (now [this] "Returns the current state."))

(deftype BoundlessHistory [states i m]
  ICollection
  (-conj [this state]
    (if-not states
      (BoundlessHistory. [state] 0 0)
      (let [i* (inc i)]
        (BoundlessHistory. (assoc states i* state) i* i*))))

  History
  (backward [this]
    (if (or (nil? states) (= i 0))
      this
      (BoundlessHistory. states (dec i) m)))
  (forward [this]
    (if (or (nil? states) (= i m))
      this
      (BoundlessHistory. states (inc i) m)))
  (now [this]
    (when states
      (states i))))

(defn boundless-history []
  (BoundlessHistory. nil -1 -1))


(deftype BoundedHistory [buf i h t incmod decmod]
  ICollection
  (-conj [this state]
    (if-not buf
      (BoundedHistory. [state] 0 0 0 incmod decmod)
      (let [i* (incmod i)]
        (BoundedHistory. (assoc buf i* state) i* i* (if (= t i*) (incmod t) t)
                         incmod decmod))))

  History
  (backward [this]
    (if (or (nil? buf) (= i t))
      this
      (BoundedHistory. buf (decmod i) h t incmod decmod)))
  (forward [this]
    (if (or (nil? buf) (= i h))
      this
      (BoundedHistory. buf (incmod i) h t incmod decmod)))
  (now [this]
    (when buf
      (buf i))))

(defn bounded-history [max-size]
  (assert (and (int? max-size) (< 0 max-size)))
  (BoundedHistory. nil -1 -1 -1
                   #(mod (inc %) max-size)
                   #(mod (dec %) max-size)))
