(ns semantic-namespace.contract.function.ontology
  (:require [clojure.spec.alpha :as s]
            [semantic-namespace.contract.docs :as docs]))

;; ----------------------------------------------------------------------------
;; Ontology sets
;; ----------------------------------------------------------------------------

(def operations
  #{:operation/create
    :operation/get
    :operation/list
    :operation/update
    :operation/delete
    :operation/validate
    :operation/process
    :operation/search
    :operation/compute
    :operation/dispatch})

(s/def :operation/type operations)

(def categories
  #{:category/query
    :category/command
    :category/computation
    :category/projection
    :category/orchestration
    :category/integration})

(s/def :category/type categories)

(def effects
  #{:effect/pure
    :effect/read
    :effect/write
    :effect/mutation
    :effect/transactional
    :effect/cacheable
    :effect/async
    :effect/notify
    :effect/compensatable
    :effect/compensation-handler})

(s/def :effect/type effects)

;; ----------------------------------------------------------------------------
;; Ontology docs
;; ----------------------------------------------------------------------------

;; operations
(docs/def :operation/create   "Creates a new domain entity.")
(docs/def :operation/get      "Retrieves a single entity.")
(docs/def :operation/list     "Retrieves multiple entities.")
(docs/def :operation/update   "Modifies an existing entity.")
(docs/def :operation/delete   "Removes an existing entity.")
(docs/def :operation/validate "Checks correctness or legitimacy of input or state.")
(docs/def :operation/process  "Runs a structured transformation pipeline.")
(docs/def :operation/search   "Finds entities that match given criteria.")
(docs/def :operation/compute  "Performs deterministic computation.")
(docs/def :operation/dispatch "Routes work to an internal handler.")

;; categories
(docs/def :category/query         "Read-only access to domain information.")
(docs/def :category/command       "State-changing operation.")
(docs/def :category/computation   "Pure computational logic.")
(docs/def :category/projection    "Derived or reshaped domain representation.")
(docs/def :category/orchestration "Coordinates multiple operations or services.")
(docs/def :category/integration   "Interacts with an external boundary or system.")

;; effects
(docs/def :effect/pure                 "Produces results without external side effects.")
(docs/def :effect/read                 "Reads external state without modifying it.")
(docs/def :effect/write                "Persists data changes.")
(docs/def :effect/mutation             "Changes domain state.")
(docs/def :effect/transactional        "Executes with atomic transactional guarantees.")
(docs/def :effect/cacheable            "Produces stable results suitable for caching.")
(docs/def :effect/async                "Executes asynchronously.")
(docs/def :effect/notify               "Emits a notification or signal.")
(docs/def :effect/compensatable        "Allows rolling back its effects.")
(docs/def :effect/compensation-handler "Reverts effects of another operation.")
