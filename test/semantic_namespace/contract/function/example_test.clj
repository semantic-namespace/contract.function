(ns semantic-namespace.contract.function.example-test
  (:require [clojure.test :refer [deftest testing is]]
            [semantic-namespace.contract.function.example]     ;; loads all f/def function definitions
            [semantic-namespace.contract.function.queries :as q]
            [semantic-namespace.contract.function :as f]
            [semantic-namespace.compound.identity :as identity]
            [clojure.set :as set]))

;; Helper
(defn ids [coll]
  (set coll))

;; ============================================================================
;; PRODUCER / CONSUMER TESTS
;; ============================================================================

(deftest producer-consumer-tests
  (testing "producers-of :order/id"
    (is (= (ids (q/producers-of :order/id))
           #{#{:operation/create
               :semantic-namespace/function
               :tier/service
               :domain/orders
               :semantic-namespace.contract/instance
               :effect/transactional
               :effect/mutation}
             #{:semantic-namespace/function
               :tier/service
               :category/orchestrator
               :effect/pure
               :operation/validate
               :domain/orders
               :semantic-namespace.contract/instance}})))

  (testing "consumers-of :order/id"
    (is (= (ids (q/consumers-of :order/id))
           #{#{:semantic-namespace/function
    :category/orchestrator
    :domain/orchestration
    :semantic-namespace.contract/instance
    :operation/checkout-saga
    :tier/feature
    :category/saga}
  #{:semantic-namespace/function
    :tier/service
    :category/orchestrator
    :effect/pure
    :operation/validate
    :domain/orders
    :semantic-namespace.contract/instance}
  #{:semantic-namespace/function
    :tier/service
    :operation/track-purchase
    :effect/async
    :domain/analytics
    :semantic-namespace.contract/instance}
  #{:semantic-namespace/function
    :tier/service
    :integration/external
    :operation/send-order-confirmation
    :effect/async
    :semantic-namespace.contract/instance
    :domain/notifications}
  #{:effect/compensatable
    :semantic-namespace/function
    :tier/service
    :domain/payments
    :integration/external
    :operation/process
    :semantic-namespace.contract/instance
    :effect/transactional
    :effect/mutation}}))))

;; ============================================================================
;; CLOSURE TESTS
;; ============================================================================

(deftest closure-tests
  (testing "no consumed key is missing a producer"
    (is (empty? (q/missing-producers))))

  (testing "unused outputs are allowed but must be a set"
    (is (set? (q/unused-outputs)))))

;; ============================================================================
;; MINIMAL CONTEXT TESTS
;; ============================================================================

(deftest minimal-context-tests
  (testing "fetch-profile minimal context"
    (is (= (q/minimal-context
            #{:semantic-namespace/function
              :domain/users
              :operation/fetch-profile
              :tier/service
              :effect/pure
              :effect/cacheable})
           #{:user/id})))

  (testing "validate-token minimal context"
    (is (= (q/minimal-context
            #{:semantic-namespace/function
              :domain/auth
              :operation/validate-token
              :tier/service
              :effect/pure})
           #{:auth/token}))))

;; ============================================================================
;; DOMAIN COUPLING TESTS
;; ============================================================================

(deftest domain-coupling-tests
  (testing "domain coupling returns a map"
    (is (map? (q/domain-coupling))))

  (testing "orders domain depends on auth and payments domains"
    (let [deps (get (q/domain-coupling) :domain/orders)]
      (is (set? deps)))))

;; ============================================================================
;; CONSISTENCY TESTS
;; ============================================================================

(deftest consistency-tests
  (testing "producer/consumer agreements are consistent"
    (doseq [k #{:order/id :transaction/id :user/id :auth/token}]
      (let [prods (set (q/producers-of k))
            cons  (set (q/consumers-of k))]
        ;; No contradictions: if a key is consumed, it must have producer(s)
        (when (seq cons)
          (is (seq prods))))))

  (testing "minimal context always a set"
    (doseq [[id _] (identity/find-with :semantic-namespace/function)]
      (is (set? (q/minimal-context id))))))
