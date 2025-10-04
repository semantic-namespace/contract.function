(ns semantic-namespace.contract.function.queries
  (:require [semantic-namespace.contract.function :as f]
            [clojure.string]
            [semantic-namespace.compound.identity :as identity]
            [clojure.set :as set]))

;; Utility
(defn all-functions []
  (identity/find-with :semantic-namespace/function))

(defn function-contract [id]
  (f/fetch id))

(defn producers-of
  "All function identities that produce the given key."
  [k]
  (->> (all-functions)
       (filter (fn [[id _]]
                 (contains? (set (:function/response (function-contract id))) k)))
       (map first)))

(defn consumers-of
  "All function identities that consume the given key."
  [k]
  (->> (all-functions)
       (filter (fn [[id _]]
                 (contains? (set (:function/context (function-contract id))) k)))
       (map first)))

(defn missing-producers
  "Keys that are consumed but never produced."
  []
  (let [consumed (->> (all-functions)
                      (mapcat (fn [[id _]]
                                (:function/context (function-contract id))))
                      set)
        produced (->> (all-functions)
                      (mapcat (fn [[id _]]
                                (:function/response (function-contract id))))
                      set)]
    (set/difference consumed produced)))

(defn unused-outputs
  "Keys that are produced but never consumed."
  []
  (let [consumed (->> (all-functions)
                      (mapcat (fn [[id _]]
                                (:function/context (function-contract id))))
                      set)
        produced (->> (all-functions)
                      (mapcat (fn [[id _]]
                                (:function/response (function-contract id))))
                      set)]
    (set/difference produced consumed)))

(defn minimal-context
  "Return the declared context of a function identity."
  [fn-id]
  (set (:function/context (function-contract fn-id))))

(defn domain-coupling
  "Map domain -> set of domains it depends on via dataflow."
  []
  (->> (all-functions)
       (reduce
        (fn [acc [id _]]
          (let [contract (function-contract id)
                domain   (some #(when (clojure.string/starts-with? (str %) ":domain/")
                                  %)
                               id)
                deps     (:function/context contract)
                producing-fns (->> (all-functions)
                                   (filter (fn [[other-id _]]
                                             (let [other (function-contract other-id)]
                                               (some (set deps) (:function/response other)))))
                                   (map first))
                dep-domains   (->> producing-fns
                                   (mapcat (fn [fid]
                                             (filter #(clojure.string/starts-with? (str %) ":domain/")
                                                     fid)))
                                   set)]
            (assoc acc domain dep-domains)))
        {})))
