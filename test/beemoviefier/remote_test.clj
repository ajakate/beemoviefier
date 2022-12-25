(ns beemoviefier.remote-test
  (:require [clojure.test :refer [deftest is testing]]
            [beemoviefier.remote :as r]))

(deftest display-transfer-percentage
  (testing "should calculate the percentage correctly"
    (is (= (r/display-transfer-percentage {:offset 45867 :size 118334}) 38.8M))
    (is (= (r/display-transfer-percentage {:offset 270 :size 360}) 75M))))
