(ns com.example.components.datomic
  (:require
    [com.fulcrologic.rad.database-adapters.db-adapter :as dba]
    [com.fulcrologic.rad.database-adapters.protocols :as dbp]
    [com.fulcrologic.rad.database-adapters.datomic :as datomic-adapter]
    [com.fulcrologic.guardrails.core :refer [>defn =>]]
    [com.example.schema :refer [prior-schema latest-schema]]
    [taoensso.timbre :as log]
    [datomic.api :as d]
    [datomock.core :refer [mock-conn]]
    [mount.core :refer [defstate]]
    [taoensso.timbre :as log]
    [com.example.components.config :refer [config]]
    [clojure.string :as str]))

(defn config->datomic-url [config]
  (let [db-config (get config ::config)
        {:postgres/keys [user host port password database]
         datomic-db     :datomic/database} db-config
        url       (str "datomic:sql://" datomic-db "?jdbc:postgresql://" host (when port (str ":" port)) "/"
                    database "?user=" user "&password=" password)]
    url))

;; TODO: This can become part of library once the pattern is well-established
(defn start-database
  []
  (log/info "Starting Datomic")
  (let [url                    (config->datomic-url config)
        ;; TODO: Better system of dealing with migrations
        created?               (d/create-database url)
        mocking-required?      (boolean (System/getProperty "force.mocked.connection"))
        mocking-ok?            (or mocking-required?
                                 (boolean (System/getProperty "allow.mocked.connection")))
        use-mocked-connection? mocking-required?
        conn                   (if use-mocked-connection?
                                 (mock-conn (d/db (d/connect url)))
                                 (d/connect url))
        adapter                (datomic-adapter/->DatomicAdapter :production conn)
        migration              (dbp/diff->migration adapter prior-schema latest-schema)]
    (log/warn "Datomic URL: " url)
    (when created?
      (log/info "New Datomic database created: " :main))
    (when use-mocked-connection?
      (log/warn "USING A MOCKED CONNECTION. No changes to the database will persist.")
      (when-not mocking-ok?
        (throw (ex-info "REFUSING TO START a database that has SNAPSHOT migrations. Please set allow.mocked.connection JVM property if you want to allow this." {}))))
    (log/info "Running Migrations")
    (try
      (d/transact conn migration)
      (catch Exception e
        (log/error "Database migration failed:" {:exception e})
        (throw e)))
    {:connection    conn
     ::dba/adapters {:production adapter}}))

(defstate ^{:on-reload :noop} production-database
  :start
  (start-database))