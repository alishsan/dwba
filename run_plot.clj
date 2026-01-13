#!/usr/bin/env lein-exec

(require '[clj-xchart.core :as xchart])

;; Load the data and plotting code from plot_data.clj
;; This file can be run with: lein exec run_plot.clj

;; Copy the data and plotting code here, or better yet, let's just fix the namespace issue
;; Actually, let's use a different approach - load it as a script

(load-file "plot_data.clj")

