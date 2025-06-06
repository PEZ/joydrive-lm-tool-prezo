(ns test.db
  (:require [cljs.test]))

(def default-db {:running nil
                 :pass 0
                 :fail 0
                 :error 0
                 :failures []
                 :errors []})

(def !state (atom default-db))

