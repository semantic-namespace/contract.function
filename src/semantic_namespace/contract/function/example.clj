(ns semantic-namespace.contract.function.example
  (:require [semantic-namespace.contract.function :as f]
            [semantic-namespace.contract.function.ontology :as o]
            [semantic-namespace.contract.ontology :as co]
            [semantic-namespace.contract.function.queries :as q]
            [semantic-namespace.contract.docs]
            [semantic-namespace.compound.identity :as i]
            [clojure.string :as str]))

;; ============================================================================
;; AUTH DOMAIN
;; ============================================================================
(binding [i/env-id #{:domain/auth :tier/service}]

  ;; authenticate credentials → token
  (f/def #{:operation/validate :category/command :effect/mutation}
    {:function/context  [:auth/credentials]
     :function/response [:auth/token :user/id :user/roles]
     :function/impl
     (fn [{:keys [:auth/credentials] :as ctx}]
       (let [creds (:auth/credentials ctx)]
         {:auth/token (str "jwt-" (hash creds))
          :user/id    (str "user-" (hash creds))
          :user/roles #{:role/user}}))})

  ;; validate token
  (f/def #{:operation/validate :category/query :effect/pure}
    {:function/context  [:auth/token]
     :function/response [:auth/valid? :user/id]
     :function/impl
     (fn [{:keys [:auth/token] :as ctx}]
       (let [token (:auth/token ctx)
             valid? (and token (str/starts-with? token "jwt-"))]
         {:auth/valid? valid?
          :user/id     (when valid? (subs token 4))}))})

  ;; authorize user for permission
  (f/def #{:operation/validate :category/query :effect/pure}
    {:function/context  [:user/id :user/roles :auth/required-permission]
     :function/response [:auth/authorized?]
     :function/impl
     (fn [{:keys [:user/roles :auth/required-permission] :as ctx}]
       (let [roles (:user/roles ctx)
             perm  (:auth/required-permission ctx)]
         {:auth/authorized?
          (or (contains? roles :role/admin)
              (contains? roles perm))}))}))

;; ============================================================================
;; USERS DOMAIN
;; ============================================================================
(binding [i/env-id #{:domain/users :tier/service}]

  ;; fetch user profile
  (f/def #{:operation/get :category/query :effect/cacheable}
    {:function/context  [:user/id]
     :function/response [:user/id :user/email :user/preferences :user/created-at]
     :function/impl
     (fn [{:keys [:user/id] :as ctx}]
       (let [id (:user/id ctx)]
         {:user/id          id
          :user/email       (str id "@example.com")
          :user/preferences {:theme "dark"}
          :user/created-at  1234567890}))})

  ;; update user preferences
  (f/def #{:operation/update :category/command :effect/mutation}
    {:function/context  [:user/id :user/preferences]
     :function/response [:user/id :user/preferences :entity/updated-at]
     :function/impl
     (fn [{:keys [:user/id :user/preferences] :as ctx}]
       (let [id           (:user/id ctx)
             preferences  (:user/preferences ctx)]
         {:user/id           id
          :user/preferences  preferences
          :entity/updated-at (System/currentTimeMillis)}))}))

;; ============================================================================
;; PRODUCTS DOMAIN
;; ============================================================================
(binding [i/env-id #{:domain/products :tier/service}]

  ;; list products by category
  (f/def #{:operation/list :category/query :effect/cacheable}
    {:function/context  [:filter/category]
     :function/response [:products/list :pagination/total-count]
     :function/impl
     (fn [{:keys [:filter/category] :as ctx}]
       (let [category (:filter/category ctx)]
         {:products/list
          [{:product/id "p1" :product/price 99.99 :product/category category}
           {:product/id "p2" :product/price 149.99 :product/category category}]
          :pagination/total-count 2}))})

  ;; validate product availability
  (f/def #{:operation/validate :category/query :effect/pure}
    {:function/context  [:product/id :requested/quantity]
     :function/response [:product/id :inventory/available? :inventory/quantity]
     :function/impl
     (fn [{:keys [:product/id :requested/quantity] :as ctx}]
       (let [id       (:product/id ctx)
             quantity (:requested/quantity ctx)]
         {:product/id           id
          :inventory/available? (<= quantity 100)
          :inventory/quantity   100}))}))

;; ============================================================================
;; ORDERS DOMAIN
;; ============================================================================
(binding [i/env-id #{:domain/orders :tier/service}]

  ;; create order
  (f/def #{:operation/create :category/command :effect/transactional}
    {:function/context  [:user/id :cart/items :shipping/address]
     :function/response [:order/id :order/status :order/total :order/user-id]
     :function/impl
     (fn [{:keys [:user/id :cart/items] :as ctx}]
       (let [user-id (:user/id ctx)
             items   (:cart/items ctx)
             total   (reduce + (map :item/price items))]
         {:order/id      (str "order-" (random-uuid))
          :order/status  :order.status/pending
          :order/user-id user-id
          :order/total   total}))})

  ;; validate order end-to-end (orchestration)
  (f/def #{:operation/validate :category/orchestration :effect/pure}
    {:function/context  [:order/id :inventory/available? :payment/valid? :auth/authorized?]
     :function/response [:order/id :validation/passed? :validation/errors]
     :function/impl
     (fn [{:keys [:order/id
                  :inventory/available?
                  :payment/valid?
                  :auth/authorized?] :as ctx}]
       (let [order-id  (:order/id ctx)
             available (:inventory/available? ctx)
             valid     (:payment/valid? ctx)
             authorized (:auth/authorized? ctx)
             errors    (cond-> []
                         (not available)  (conj :error/inventory-unavailable)
                         (not valid)      (conj :error/payment-invalid)
                         (not authorized) (conj :error/unauthorized))]
         {:order/id           order-id
          :validation/passed? (empty? errors)
          :validation/errors  errors}))}))

;; ============================================================================
;; PAYMENTS DOMAIN
;; ============================================================================
(binding [i/env-id #{:domain/payments :tier/service}]

  ;; validate payment method
  (f/def #{:operation/validate :category/integration :effect/pure}
    {:function/context  [:payment/method :payment/amount]
     :function/response [:payment/valid? :payment/processor :risk/score]
     :function/impl
     (fn [{:keys [:payment/method :payment/amount] :as ctx}]
       {:payment/valid?    true
        :payment/processor :payment.processor/stripe
        :risk/score        0.2})})

  ;; process payment
  (f/def #{:operation/process :category/integration :effect/transactional}
    {:function/context  [:order/id :order/total :payment/method]
     :function/response [:transaction/id :transaction/status :transaction/amount]
     :function/impl
     (fn [{:keys [:order/total] :as ctx}]
       (let [total (:order/total ctx)]
         {:transaction/id     (str "txn-" (random-uuid))
          :transaction/status :transaction.status/completed
          :transaction/amount total}))})

  ;; refund (compensation handler)
  (f/def #{:operation/update :category/command :effect/compensation-handler}
    {:function/context  [:transaction/id :refund/reason]
     :function/response [:refund/id :refund/status :refund/processed-at]
     :function/impl
     (fn [{:keys [:transaction/id] :as ctx}]
       {:refund/id           (str "refund-" (random-uuid))
        :refund/status       :refund.status/processed
        :refund/processed-at (System/currentTimeMillis)})}))

;; ============================================================================
;; NOTIFICATIONS DOMAIN
;; ============================================================================
(binding [i/env-id #{:domain/notifications :tier/service}]

  ;; send order confirmation
  (f/def #{:operation/dispatch :category/integration :effect/async}
    {:function/context  [:user/email :order/id :order/total]
     :function/response [:notification/id :notification/status]
     :function/impl
     (fn [{:keys [:user/email :order/id] :as ctx}]
       {:notification/id     (str "notif-" (random-uuid))
        :notification/status :notification.status/queued})}))

;; ============================================================================
;; ANALYTICS DOMAIN
;; ============================================================================
(binding [i/env-id #{:domain/analytics :tier/service}]

  ;; track purchase event
  (f/def #{:operation/process :category/computation :effect/async}
    {:function/context  [:user/id :order/id :transaction/id :products/list]
     :function/response [:event/id :event/type :event/timestamp]
     :function/impl
     (fn [_]
       {:event/id        (str "evt-" (random-uuid))
        :event/type      :event.type/purchase
        :event/timestamp (System/currentTimeMillis)})}))


;; ============================================================================
;; ORCHESTRATION DOMAIN
;; ============================================================================
(binding [i/env-id #{:domain/orchestration :tier/feature}]

  ;; checkout saga orchestration
  (f/def #{:operation/process :category/orchestration :effect/async}
    {:function/context  [:auth/token
                         :user/id
                         :products/list
                         :inventory/available?
                         :payment/valid?
                         :order/id
                         :transaction/id
                         :notification/id]
     :function/response [:saga/id :saga/status :saga/steps]
     :function/impl
     (fn [_]
       {:saga/id     (str "saga-" (random-uuid))
        :saga/status :saga.status/completed
        :saga/steps  [:auth :products :inventory :payments :orders :notifications]})}))

(def domains #{:domain/analytics
               :domain/auth
               :domain/notifications
               :domain/orchestration
               :domain/orders
               :domain/payments
               :domain/products
               :domain/users})


;; These are axioms and/or meaningful data  represenations

(->> (mapv (fn [[k v]] (-> k
                           (disj :semantic-namespace/function)
                           (disj :semantic-namespace.contract/instance))) (q/all-functions))
     (filter (partial not= #{:semantic-namespace.contract/type}))
;;     (group-by #(domains (some domains % )))
     (group-by #(co/tiers (some co/tiers % )))
     #_(mapv (fn [x] (filter (fn [k]
                             ;;(domains k)
                             (not (or (o/categories k)
                                      (o/effects k)
                                      (domains k)
                                      (co/tiers k)
                                      (o/operations k)))) x)))
     )


