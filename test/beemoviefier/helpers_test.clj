(ns beemoviefier.helpers-test
  (:require [clojure.test :refer [deftest is testing]]
            [beemoviefier.helpers :as h]))

(deftest round
  (testing "pi, round down"
    (is (= (h/round 3 3.1415) 3.142M)))
  (testing "random number round up"
    (is (= (h/round 1 874.9992) 875M))))

(deftest timestamp-out-file
  (testing "should make a nice filename"
    (is (re-matches
         #"out_\d{4}_\d{2}_\d{2}_\d{2}_\d{2}_\d{2}_\d{6}.mp4"
         (h/timestamp-out-file)))))
