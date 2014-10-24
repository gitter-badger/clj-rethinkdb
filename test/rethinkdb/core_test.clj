(ns rethinkdb.core-test
  (:require [clj-time.core :as t]
            [clojure.test :refer :all]
            [rethinkdb.core :refer :all]
            [rethinkdb.query :as r]))

(def test-db "test")

(defn clear-db [test-fn]
  (let [conn (connect)
        db-list (-> (r/db-list) (r/run conn))]
    (if (some #{test-db} db-list)
      (-> (r/db-drop test-db) (r/run conn)))
    (r/run (r/db-create test-db) conn)
    (close conn))
  (test-fn))

(defmacro with-test-db [body]
  `(-> (r/db test-db)
       ~body
       (r/run ~'conn)))

(deftest core-test
  (let [conn (connect)]
    (testing "table management"
      (with-test-db
        (r/table-create "pokemons")))
    #_(testing "inserting and retrieving documents"
      (println (-> (r/db test-db)
          (r/table "dc_universe")
          (r/insert {:hero "Batman"
                     :gadgets ["Batarangs" "Batclaw" "Batrope"]
                     :real_name "Bruce Wayne"}
                    :return_changes true)
          (r/run conn)))
      (-> (r/db test-db)
          (r/table "dc_universe")
          (r/insert [{:hero "Superman"
                      :real_name "Clark Kent"}
                     {:hero "Nightwing"
                      :real_name "Dick Grayson"}])
          (r/run conn))
      (is (= 3 (-> (r/db test-db) (r/table "dc_universe") (r/count) (r/run conn))))
      (is (= (sort-by #(% :hero) [{:hero "Batman"} {:hero "Nightwing"} {:hero "Superman"}])
             (sort-by #(% :hero)
                      (-> (r/db test-db)
                          (r/table "dc_universe")
                          (r/pluck :hero)
                          (r/run conn)))))
      (is (= 1 (count (-> (r/db test-db)
                          (r/table "dc_universe")
                          (r/filter
                            (r/lambda [row]
                              (r/eq (r/get-field row "hero") "Superman")))
                          (r/run conn)))))
      (is (= 2 (count (-> (r/db test-db)
                          (r/table "dc_universe")
                          (r/filter
                            (r/lambda [row]
                                      (r/not (r/eq (r/get-field row "hero") "Superman"))))
                          (r/run conn)))))
      (is (= 1 (count (-> (r/db test-db)
                          (r/table "dc_universe")
                          (r/limit 1)
                          (r/run conn)))))
      (is (= 3 (count (-> (r/db test-db)
                          (r/table "dc_universe")
                          (r/filter
                            (r/lambda [row]
                              (r/has-fields row "hero")))
                          (r/run conn))))))
    #_(testing "updating"
      (println (-> (r/db test-db)
                   (r/table "dc_universe")
                   (r/filter
                     (r/lambda [row]
                       (r/eq (r/get-field row :hero) "Batman")))
                   (r/update {:created_at (t/date-time 1939 5 1)} :return_changes true)
                   (r/run conn))))
    #_(testing "mapping"
      (is (= (set ["Superman" "Nightwing" "Batman"])
             (set (-> (r/db test-db)
                      (r/table "dc_universe")
                      (r/map
                        (r/lambda [row]
                          (r/get-field row "hero")))
                      (r/run conn))))))
    #_(testing "dates and times"
      (let [date1 (t/date-time 1986 11 27)
            date2 (t/date-time 1986 11 27 12 30 00)]
        (-> (r/db test-db)
            (r/table "dc_universe")
            (r/insert {:name "Erik"
                       :birthdate date1})
            (r/run conn))
        (println (-> (r/db test-db)
                     (r/table "dc_universe")
                     (r/has-fields :birthdate)
                     (r/run conn)))))
    (close conn)))

(use-fixtures :once clear-db)
