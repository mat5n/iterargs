(ns iterargs.history-test
  (:require [iterargs.history :as hist]
            [cljs.test :refer-macros [deftest is are]]))

(deftest boundless-history
  (let [h0 (hist/boundless-history)
        h1 (into h0 [1 2 3 4])
        h2 (-> h1 hist/backward hist/backward)
        h3 (conj h2 5)]
    (is (nil? (-> h0 hist/now)))
    (is (= 4 (-> h1 hist/now)))
    (is (= 2 (-> h2 hist/now)))
    (is (= 1 (-> h2 hist/backward hist/now)))
    (is (= 1 (-> h2 hist/backward hist/backward hist/now)))
    (is (= 3 (-> h2 hist/forward hist/now)))
    (is (= 5 (-> h3 hist/now)))
    (is (= 5 (-> h3 hist/forward hist/now)))))

(deftest bounded-history
  (let [h0 (hist/bounded-history 3)
        h1 (into h0 [1 2 3 4])
        h2 (-> h1 hist/backward hist/backward)
        h3 (conj h2 5)]
    (is (nil? (-> h0 hist/now)))
    (is (= 4 (-> h1 hist/now)))
    (is (= 2 (-> h2 hist/now)))
    (is (= 2 (-> h2 hist/backward hist/now)))
    (is (= 2 (-> h2 hist/backward hist/backward hist/now)))
    (is (= 3 (-> h2 hist/forward hist/now)))
    (is (= 5 (-> h3 hist/now)))
    (is (= 5 (-> h3 hist/forward hist/now)))))

(deftest bounded-history-of-size-1
  (let [h0 (hist/bounded-history 1)
        h1 (into h0 [1 2 3 4])
        h2 (-> h1 hist/backward)
        h3 (conj h2 5)]
    (is (nil? (-> h0 hist/now)))
    (is (= 4 (-> h1 hist/now)))
    (is (= 4 (-> h2 hist/now)))
    (is (= 4 (-> h2 hist/backward hist/now)))
    (is (= 4 (-> h2 hist/forward hist/now)))
    (is (= 5 (-> h3 hist/now)))
    (is (= 5 (-> h3 hist/forward hist/now)))))
