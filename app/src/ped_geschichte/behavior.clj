(ns ^:shared ped-geschichte.behavior
    (:require [clojure.string :as string]
              [geschichte.repo :as repo]
              [io.pedestal.app.util.platform :as p]
              [io.pedestal.app.messages :as msg]
              [io.pedestal.app :as app]))

;; While creating new behavior, write tests to confirm that it is
;; correct. For examples of various kinds of tests, see
;; test/ped_geschichte/behavior-test.clj.

;; TODO
;; - implement a simple toolbar
;; - show repo meta-data

(defn set-value-transform [old-value message]
  (:value message))

;; Emitters

(defn init-main [_]
  [{:geschichte
    {:staged {}
     :repo {}
     :commit {}
     :form
     {:select
      {:transforms
       {:set-value [{msg/topic [:staged] (msg/param :value) {}}]
        :set-repo [{msg/topic [:repo] (msg/param :value) {}}]
        :commit [{msg/topic [:commit]}]}}}}}])


(defn commit [old {:keys [repo value commit meta]}]
  (if-not (or (= (:value old) value)
              (= (:ts old) commit)
              (nil? repo))
    (assoc (repo/commit repo
                        meta
                        (if (:head meta) #{(:head meta)} #{})
                        value)
      :ts commit)
    old))


(defn transact-to-kv [trans]
  [{msg/topic [:transact-to-kv] :transact trans}])

(defn commit-to-kv [{:keys [meta value repo] :as trans}]
  (when (and meta value repo)
    (transact-to-kv (assoc trans :puts [[(:head meta) value]
                                        [repo meta]]))))

(def example-app
  ;; There are currently 2 versions (formats) for dataflow
  ;; description: the original version (version 1) and the current
  ;; version (version 2). If the version is not specified, the
  ;; description will be assumed to be version 1 and an attempt
  ;; will be made to convert it to version 2.
  {:version 2
   :transform [[:set-repo [:repo] set-value-transform]
               [:set-value [:staged] set-value-transform]
               [:commit [:commit] p/date] ; UUID?
               [:set-meta [:meta] set-value-transform]]
   :derive #{[{[:repo] :repo
               [:commit] :commit
               [:staged] :value
               [:meta] :meta} [:trans-comm] commit :map]}
   :effect #{[#{[:trans-comm]} commit-to-kv :single-val]}
   :emit [{:init init-main}
          [#{[:staged]
             [:repo]
             [:commit]
             [:trans-comm]
             [:meta]} (app/default-emitter [:geschichte])]]})


;; Once this behavior works, run the Data UI and record
;; rendering data which can be used while working on a custom
;; renderer. Rendering involves making a template:
;;
;; app/templates/ped-geschichte.html
;;
;; slicing the template into pieces you can use:
;;
;; app/src/ped_geschichte/html_templates.cljs
;;
;; and then writing the rendering code:
;;
;; app/src/ped_geschichte/rendering.cljs
