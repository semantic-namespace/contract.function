(ns semantic-namespace.contract.function.axioms
  (:require [semantic-namespace.contract.function :as f]
            [semantic-namespace.compound.identity :as identity]
            [semantic-namespace.contract.function.queries :as q]
            [clojure.string :as str]
            [clojure.set :as set]))

(defn all-function-ids []
  (q/all-functions))

(defn- tier-level [kw]
  (case kw
    :tier/foundation 0
    :tier/service    1
    :tier/feature    2
    :tier/api        3
    10))

(defn function-tier [fid]
  (first (filter #(clojure.string/starts-with? (str %) ":tier/")
                 fid)))

(defn function-domains [fid]
  (filter #(clojure.string/starts-with? (str %) ":domain/")
          fid))

(defn axiom-functions-have-context-or-response []
  (every?
   (fn [fid]
     (let [c (:function/context (q/function-contract fid))
           r (:function/response (q/function-contract fid))]
       (or (seq c) (seq r))))
   (all-function-ids)))

(defn axiom-data-flow-completeness []
  (empty? (q/missing-producers)))

(defn axiom-no-cycles []
  #_(let [graph (q/function-dependency-graph)]
    (not-any?
     (fn [[fid deps]]
       (some #(contains? (get graph % #{}) fid) deps))
     graph)))

(defn axiom-tier-hierarchy []
  #_(let [graph (q/function-dependency-graph)]
    (every?
     (fn [fid]
       (let [t         (function-tier fid)
             t-level   (tier-level t)
             deps      (get graph fid)
             dep-levels (map (comp tier-level function-tier) deps)]
         (every? #(<= % t-level) dep-levels)))
     (all-function-ids))))

(defn axiom-mutations-need-auth-context []
  #_(let [mutations (q/mutation-functions)
        auth-keys #{:auth/token :auth/authorized? :user/roles}]
    (every?
     (fn [fid]
       (let [ctx (set (q/function-context fid))]
         (or (empty? (set/intersection ctx auth-keys))
             (seq (set/intersection ctx auth-keys)))))
     mutations)))

(defn axiom-payments-isolation []
  #_(let [payments (filter #(some #{:domain/payments} %) (all-function-ids))
        forbidden #{:cart/items :products/list :user/preferences}]
    (every?
     (fn [fid]
       (let [ctx (set (q/function-context fid))]
         (empty? (set/intersection ctx forbidden))))
     payments)))

(defn validate-all []
  {:functions-have-context-or-response (axiom-functions-have-context-or-response)
   :data-flow-complete                 (axiom-data-flow-completeness)
   :no-cycles                          (axiom-no-cycles)
   :tier-hierarchy                     (axiom-tier-hierarchy)
   :mutations-auth                     (axiom-mutations-need-auth-context)
   :payments-isolated                  (axiom-payments-isolation)})

(comment
  (validate-all))
