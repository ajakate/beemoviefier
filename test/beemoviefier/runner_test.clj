(ns beemoviefier.runner-test
  (:require [clojure.test :refer [deftest is testing]]
            [beemoviefier.runner :as r]))

(deftest get-speeds-for-index
  (testing "should work for 2x inc rate"
    (is (= (r/get-speeds-for-index 0 2M nil) {:audio '("2.0") :video "0.50"}))
    (is (= (r/get-speeds-for-index 1 2M nil) {:audio '("4.0") :video "0.250"}))
    (is (= (r/get-speeds-for-index 2 2M nil) {:audio '("8.0") :video "0.1250"})))
  (testing "it should max the audio at 200x"
    (is (= (r/get-speeds-for-index 0 7M nil) {:audio '("7.0") :video "0.1428570"}))
    (is (= (r/get-speeds-for-index 1 7M nil) {:audio '("49.0"), :video "0.02040820"}))
    (is (= (r/get-speeds-for-index 2 7M 200M) {:audio '("100.0" "2.0"), :video "0.0050"}))))

(deftest format-audio-string
  (testing "should parse correctly"
    (is (= (r/format-audio-string '("100.0" "34.0")) ",atempo=100.0,atempo=34.0"))
    (is (= (r/format-audio-string '("2.0")) ",atempo=2.0"))))
