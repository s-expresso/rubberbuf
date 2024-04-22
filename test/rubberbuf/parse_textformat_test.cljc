(ns rubberbuf.parse-textformat-test
  (:require [rubberbuf.parse-textformat :refer [parse]]
            [clojure.test :refer [is deftest run-tests]]))

(deftest test-parse-string
  (is (= (parse "xyz: 'abc'") {"xyz" "abc"}))
  (is (= (parse "xyz: \"abc\"") {"xyz" "abc"})))

(deftest test-parse-identifier
  (is (= (parse "xyz: inf") {"xyz" :inf}))
  (is (= (parse "xyz: -inf") {"xyz" :-inf}))
  (is (= (parse "xyz: id_can_be_anything") {"xyz" :id_can_be_anything})))

(deftest test-parse-float
  ; basic
  (is (= (parse "xyz: 0.45") {"xyz" 0.45}))
  (is (= (parse "xyz: -0.45") {"xyz" -0.45}))
  (is (= (parse "xyz: 123.45") {"xyz" 123.45}))
  (is (= (parse "xyz: -123.45") {"xyz" -123.45}))
  ;basic with trailing f/F
  (is (= (parse "xyz: 123.45f") {"xyz" 123.45}))
  (is (= (parse "xyz: 123.45F") {"xyz" 123.45}))
  (is (= (parse "xyz: -123.45f") {"xyz" -123.45}))
  (is (= (parse "xyz: -123.45F") {"xyz" -123.45}))
  ; exponent
  (is (= (parse "xyz: 123.45e1") {"xyz" 1234.5}))
  (is (= (parse "xyz: -123.45e1") {"xyz" -1234.5}))
  ; exponent with trailing f/F
  (is (= (parse "xyz: 123.45e1f") {"xyz" 1234.5}))
  (is (= (parse "xyz: 123.45e1F") {"xyz" 1234.5}))
  (is (= (parse "xyz: -123.45e1f") {"xyz" -1234.5}))
  (is (= (parse "xyz: -123.45e1F") {"xyz" -1234.5}))
  ; no dot
  (is (= (parse "xyz: 123e1") {"xyz" 1230.0}))
  (is (= (parse "xyz: -123e1") {"xyz" -1230.0}))
  ; no dot with trailing f/F
  (is (= (parse "xyz: 123e1f") {"xyz" 1230.0}))
  (is (= (parse "xyz: 123e1F") {"xyz" 1230.0}))
  (is (= (parse "xyz: -123e1f") {"xyz" -1230.0}))
  (is (= (parse "xyz: -123e1F") {"xyz" -1230.0}))
  ; leading dot
  (is (= (parse "xyz: .123") {"xyz" 0.123}))
  (is (= (parse "xyz: -.123") {"xyz" -0.123}))
  ; leading dot with trailing f/F
  (is (= (parse "xyz: .123f") {"xyz" 0.123}))
  (is (= (parse "xyz: .123F") {"xyz" 0.123}))
  (is (= (parse "xyz: -.123f") {"xyz" -0.123}))
  (is (= (parse "xyz: -.123F") {"xyz" -0.123})))

(deftest test-parse-int
  (is (= (parse "xyz: 0") {"xyz" 0}))
  (is (= (parse "xyz: 123") {"xyz" 123}))
  (is (= (parse "xyz: -123") {"xyz" -123})))

(deftest test-parse-oct
  (is (= (parse "xyz: 01") {"xyz" 1}))
  (is (= (parse "xyz: -01") {"xyz" -1}))
  (is (= (parse "xyz: 010") {"xyz" 8}))
  (is (= (parse "xyz: -010") {"xyz" -8}))
  (is (= (parse "xyz: 0100") {"xyz" 64}))
  (is (= (parse "xyz: -0100") {"xyz" -64}))
  (is (= (parse "xyz: 0111") {"xyz" 73}))
  (is (= (parse "xyz: -0111") {"xyz" -73}))
  (is (= (parse "xyz: 0345") {"xyz" (+ (* 3 64) (* 4 8) (* 5 1))}))
  (is (= (parse "xyz: -0345") {"xyz" (- (+ (* 3 64) (* 4 8) (* 5 1)))})))

(deftest test-parse-hex
  (is (= (parse "xyz: 0xabcd") {"xyz" 43981}))
  (is (= (parse "xyz: -0xabcd") {"xyz" -43981}))
  (is (= (parse "xyz: 0Xabcd") {"xyz" 43981}))
  (is (= (parse "xyz: -0Xabcd") {"xyz" -43981}))
  (is (= (parse "xyz: 0xABCD") {"xyz" 43981}))
  (is (= (parse "xyz: -0xABCD") {"xyz" -43981}))
  (is (= (parse "xyz: 0XABCD") {"xyz" 43981}))
  (is (= (parse "xyz: -0XABCD") {"xyz" -43981})))

(deftest test-parse-fieldname
  (is (= (parse "xyz: 1") {"xyz" 1}))
  (is (= (parse "[xyz]: 1") {"[xyz]" 1})) ; extension
  (is (= (parse "[xyz.com/xyz]: 1") {"[xyz.com/xyz]" 1}))) ; any

(deftest test-parse-message
  (is (= (parse "xyz: {abc: 1, def: 2}") {"xyz" {"abc" 1, "def" 2}}))
  (is (= (parse "xyz: {abc: {def: 2}}") {"xyz" {"abc" {"def" 2}}}))
  (is (= (parse "xyz: {abc: [1, 2, 3], def: [4, 5, 6]}") {"xyz" {"abc" [1 2 3] "def" [4 5 6]}}))
  (is (= (parse "xyz: {abc: [{abc: 1}], def: [1, 2, 3]}") {"xyz" {"abc" [{"abc" 1}]
                                                                  "def" [1 2 3]}})))

(deftest test-parse-list
  (is (= (parse "xyz: [1, 2, 3]") {"xyz" [1 2 3]}))
  (is (= (parse "xyz: {a: 1, b: 2, a: 3, a: 4}") {"xyz" {"a" [1 3 4], "b" 2}}))
  (is (= (parse "xyz: {a: 1, a: [2, 3]}") {"xyz" {"a" [1 2 3]}}))
  (is (= (parse "xyz: {a: [1, 2], a: 3}") {"xyz" {"a" [1 2 3]}}))
  (is (= (parse "xyz: {a: [1], a: [2, 3]}") {"xyz" {"a" [1 2 3]}})))
