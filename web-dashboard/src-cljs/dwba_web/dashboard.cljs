(ns dwba-web.dashboard
  (:require [goog.dom :as dom]
            [goog.events :as events]
            [goog.dom.forms :as forms]
            [clojure.string :as str]))

;; External JavaScript libraries (Plotly) - accessed via js/ namespace
(def plotly js/Plotly)

;; Dashboard state
(defonce dashboard-state (atom {:api-base ""
                                :current-data nil
                                :active-tab :phase}))

;; Helper functions
(defn get-element [id]
  (dom/getElement id))

(defn get-value [id]
  (forms/getValue (get-element id)))

(defn set-value! [id value]
  (forms/setValue (get-element id) value))

(defn set-text! [id text]
  (set! (.-textContent (get-element id)) text))

(defn get-float [id]
  (js/parseFloat (get-value id)))

(defn get-int [id]
  (js/parseInt (get-value id)))

(defn parse-comma-separated [s]
  (->> (str/split s #",")
       (map str/trim)
       (filter (complement str/blank?))))

;; Event listeners
(defn initialize-event-listeners []
  ;; Parameter sliders
  (doseq [param ["V0" "R0" "a0" "radius"]]
    (let [slider (get-element param)
          value-display (get-element (str param "-value"))]
      (events/listen slider "input"
        (fn [e]
          (let [value (js/parseFloat (.-value (.-target e)))
                unit (if (= param "V0") "MeV" "fm")]
            (set-text! (str param "-value") (str value " " unit)))))))
  ;; Tab selection: track which results tab is active
  (doseq [[tab-id tab-key] [["phase-tab" :phase]
                            ["rmatrix-tab" :rmatrix]
                            ["potential-tab" :potential]
                            ["cross-section-tab" :cross-section]
                            ["elastic-tab" :elastic]
                            ["inelastic-tab" :inelastic]
                            ["transfer-tab" :transfer]
                            ["dashboard-tab" :dashboard]]]
    (when-let [el (get-element tab-id)]
      (events/listen el "click"
        (fn [_]
          (swap! dashboard-state assoc :active-tab tab-key)))))
  
  ;; Calculate button
  (let [calc-btn (get-element "calculate-btn")]
    (events/listen calc-btn "click" calculate-dwba))
  
  ;; Reset button
  (let [reset-btn (get-element "reset-btn")]
    (when reset-btn
      (events/listen reset-btn "click" reset-parameters))))

;; Load default parameters
(defn load-default-parameters []
  (let [api-base (:api-base @dashboard-state)]
    (-> (js/fetch (str api-base "/api/parameters"))
        (.then (fn [response] (.json response)))
        (.then (fn [data]
                 (when (aget data "default_parameters")
                   (set-parameters (js->clj (aget data "default_parameters") :keywordize-keys true)))))
        (.catch (fn [error]
                  (js/console.error "Error loading default parameters:" error))))))

;; Set parameters
(defn set-parameters [params]
  (set-value! "V0" (:V0 params))
  (set-value! "R0" (:R0 params))
  (set-value! "a0" (:a0 params))
  (set-value! "radius" (:radius params))
  (set-value! "energy-range" (str/join "," (:energies params)))
  (set-value! "L-values" (str/join "," (or (:L_values params) (:L-values params))))
  
  (when (:E_ex params) (set-value! "E_ex" (:E_ex params)))
  (when (:lambda params) (set-value! "lambda" (:lambda params)))
  (when (:beta params) (set-value! "beta" (:beta params)))
  (when (:reaction_type params) (set-value! "reaction_type" (:reaction_type params)))
  
  ;; Update slider displays
  (set-text! "V0-value" (str (:V0 params) " MeV"))
  (set-text! "R0-value" (str (:R0 params) " fm"))
  (set-text! "a0-value" (str (:a0 params) " fm"))
  (set-text! "radius-value" (str (:radius params) " fm")))

;; Get parameters (keys must match API: :L_values not :L-values)
(defn get-parameters []
  {:V0 (get-float "V0")
   :R0 (get-float "R0")
   :a0 (get-float "a0")
   :radius (get-float "radius")
   :energies (parse-comma-separated (get-value "energy-range"))
   :L_values (parse-comma-separated (get-value "L-values"))
   :E_ex (get-float "E_ex")
   :lambda (get-int "lambda")
   :beta (get-float "beta")
   :reaction_type (get-value "reaction_type")})

;; Show status message
(defn show-status [message type]
  (let [status-div (get-element "status-messages")
        alert-class (case type
                      "error" "error"
                      "success" "success"
                      "alert-info")
        icon-class (case type
                     "error" "exclamation-triangle"
                     "success" "check-circle"
                     "info-circle")]
    (set! (.-innerHTML status-div)
          (str "<div class=\"" alert-class "\">"
               "<i class=\"fas fa-" icon-class "\"></i> "
               message
               "</div>"))
    ;; Auto-hide after 5 seconds
    (js/setTimeout #(set! (.-innerHTML status-div) "") 5000)))

;; Calculate DWBA
(defn calculate-dwba []
  (let [start-time (js/Date.now)
        calculate-btn (get-element "calculate-btn")
        api-base (:api-base @dashboard-state)
        active-tab (:active-tab @dashboard-state)]
    ;; Show loading state
    (set! (.-disabled calculate-btn) true)
    (set! (.-innerHTML calculate-btn) "<i class=\"fas fa-spinner fa-spin\"></i> Calculating...")
    (show-status "Performing DWBA calculations..." "info")
    
    (let [params (get-parameters)]
      ;; Validate parameters that are common to all calculations
      (if (or (empty? (:energies params)) (empty? (:L_values params)))
        (do
          (show-status "Error: Please provide valid energy range and angular momenta" "error")
          (set! (.-disabled calculate-btn) false)
          (set! (.-innerHTML calculate-btn) "<i class=\"fas fa-calculator\"></i> Calculate DWBA"))
        ;; Dispatch to the appropriate endpoint based on the active tab
        (case active-tab
          ;; Core phase-shift / R-matrix / potentials / total cross sections
          (:phase :rmatrix :potential :cross-section :dashboard)
          (-> (js/fetch (str api-base "/api/calculate")
                        (clj->js {:method "POST"
                                  :headers {"Content-Type" "application/json"}
                                  :body (js/JSON.stringify (clj->js params))}))
              (.then (fn [r] (.json r)))
              (.then (fn [basic-result]
                       (if (aget basic-result "success")
                         (let [basic-data (js->clj (aget basic-result "data") :keywordize-keys true)
                               calculation-time (- (js/Date.now) start-time)]
                           (swap! dashboard-state assoc :current-data basic-data)
                           (js/console.log "Calculate result: success" (clj->js (count (:phase_shifts basic-data))))
                           (update-all-plots)
                           (update-dashboard-stats calculation-time)
                           (show-status (str "Core DWBA calculations completed in " calculation-time "ms") "success"))
                         (show-status (str "Error: " (or (aget basic-result "error") "Calculation failed")) "error")))
              (.catch (fn [error]
                        (js/console.error "Calculation error:" error)
                        (show-status (str "Error: " (.-message error)) "error")))
              (.finally (fn []
                          (set! (.-disabled calculate-btn) false)
                          (set! (.-innerHTML calculate-btn) "<i class=\"fas fa-calculator\"></i> Calculate DWBA"))))

          ;; Elastic scattering only
          :elastic
          (-> (js/fetch (str api-base "/api/elastic")
                        (clj->js {:method "POST"
                                  :headers {"Content-Type" "application/json"}
                                  :body (js/JSON.stringify (clj->js params))}))
              (.then (fn [r] (.json r)))
              (.then (fn [result]
                       (if (aget result "success")
                         (let [elastic-data (js->clj (aget result "data" "elastic") :keywordize-keys true)
                               calculation-time (- (js/Date.now) start-time)
                               current-data (:current-data @dashboard-state)
                               merged (merge (or current-data {}) {:elastic elastic-data})]
                           (swap! dashboard-state assoc :current-data merged)
                           (update-all-plots)
                           (show-status (str "Elastic scattering calculated in " calculation-time "ms") "success"))
                         (show-status (str "Error: " (or (aget result "error") "Elastic calculation failed")) "error")))
              (.catch (fn [error]
                        (js/console.error "Elastic calculation error:" error)
                        (show-status (str "Error: " (.-message error)) "error")))
              (.finally (fn []
                          (set! (.-disabled calculate-btn) false)
                          (set! (.-innerHTML calculate-btn) "<i class=\"fas fa-calculator\"></i> Calculate DWBA"))))

          ;; Inelastic scattering only
          :inelastic
          (-> (js/fetch (str api-base "/api/inelastic")
                        (clj->js {:method "POST"
                                  :headers {"Content-Type" "application/json"}
                                  :body (js/JSON.stringify (clj->js params))}))
              (.then (fn [r] (.json r)))
              (.then (fn [result]
                       (if (aget result "success")
                         (let [inelastic-data (js->clj (aget result "data" "inelastic") :keywordize-keys true)
                               calculation-time (- (js/Date.now) start-time)
                               current-data (:current-data @dashboard-state)
                               merged (merge (or current-data {}) {:inelastic inelastic-data})]
                           (swap! dashboard-state assoc :current-data merged)
                           (update-all-plots)
                           (show-status (str "Inelastic scattering calculated in " calculation-time "ms") "success"))
                         (show-status (str "Error: " (or (aget result "error") "Inelastic calculation failed")) "error")))
              (.catch (fn [error]
                        (js/console.error "Inelastic calculation error:" error)
                        (show-status (str "Error: " (.-message error)) "error")))
              (.finally (fn []
                          (set! (.-disabled calculate-btn) false)
                          (set! (.-innerHTML calculate-btn) "<i class=\"fas fa-calculator\"></i> Calculate DWBA"))))

          ;; Transfer reactions only
          :transfer
          (-> (js/fetch (str api-base "/api/transfer")
                        (clj->js {:method "POST"
                                  :headers {"Content-Type" "application/json"}
                                  :body (js/JSON.stringify (clj->js params))}))
              (.then (fn [r] (.json r)))
              (.then (fn [result]
                       (if (aget result "success")
                         (let [transfer-data (js->clj (aget result "data" "transfer") :keywordize-keys true)
                               calculation-time (- (js/Date.now) start-time)
                               current-data (:current-data @dashboard-state)
                               merged (merge (or current-data {}) {:transfer transfer-data})]
                           (swap! dashboard-state assoc :current-data merged)
                           (update-all-plots)
                           (show-status (str "Transfer reaction calculated in " calculation-time "ms") "success"))
                         (show-status (str "Error: " (or (aget result "error") "Transfer calculation failed")) "error")))
              (.catch (fn [error]
                        (js/console.error "Transfer calculation error:" error)
                        (show-status (str "Error: " (.-message error)) "error")))
              (.finally (fn []
                          (set! (.-disabled calculate-btn) false)
                          (set! (.-innerHTML calculate-btn) "<i class=\"fas fa-calculator\"></i> Calculate DWBA"))))

          ;; Default: fall back to core calculation
          (do
            (js/console.warn "Unknown active tab, defaulting to core calculation" (str active-tab))
            (set! (.-disabled calculate-btn) false)
            (set! (.-innerHTML calculate-btn) "<i class=\"fas fa-calculator\"></i> Calculate DWBA"))))))))

;; Normalize API data keys (backend may send phase_shifts or phase-shifts etc.)
(defn normalize-data-keys [data]
  (when data
    {:phase_shifts (or (:phase_shifts data) (:phase-shifts data))
     :r_matrices (or (:r_matrices data) (:r-matrices data))
     :potentials (:potentials data)
     :cross_sections (or (:cross_sections data) (:cross-sections data))
     :elastic (:elastic data)
     :inelastic (:inelastic data)
     :transfer (:transfer data)
     :parameters (:parameters data)}))

;; Update all plots
(defn update-all-plots []
  (let [raw (:current-data @dashboard-state)
        current-data (normalize-data-keys raw)]
    (when current-data
      (plot-phase-shifts)
      (plot-r-matrices)
      (plot-potentials)
      (plot-cross-sections)
      (when (:elastic current-data) (plot-elastic))
      (when (:inelastic current-data) (plot-inelastic))
      (when (:transfer current-data) (plot-transfer))
      (plot-dashboard))))

;; Plot phase shifts
(defn plot-phase-shifts []
  (let [data (:phase_shifts (:current-data @dashboard-state))
        traces (reduce (fn [traces point]
                         (let [L (:L point)]
                           (update traces L
                                    (fn [trace]
                                      (if trace
                                        (-> trace
                                            (update :x conj (:energy point))
                                            (update :y conj (* (:phase_shift point) (/ 180 js/Math.PI))))
                                        {:x [(:energy point)]
                                         :y [(* (:phase_shift point) (/ 180 js/Math.PI))]
                                         :name (str "L = " L)
                                         :type "scatter"
                                         :mode "lines+markers"
                                         :line {:width 3}
                                         :marker {:size 6}})))))
                       {} data)
        plot-data (clj->js (vals traces))
        layout (clj->js {:title "Nuclear Phase Shifts vs Energy"
                         :xaxis {:title "Energy (MeV)" :gridcolor "#e0e0e0"}
                         :yaxis {:title "Phase Shift (degrees)" :gridcolor "#e0e0e0"}
                         :plot_bgcolor "rgba(0,0,0,0)"
                         :paper_bgcolor "rgba(0,0,0,0)"
                         :font {:family "Arial, sans-serif"}
                         :legend {:x 0.02 :y 0.98}
                         :margin {:t 50 :b 50 :l 60 :r 30}})]
    (.newPlot plotly "phase-plot" plot-data layout (clj->js {:responsive true}))))

;; Plot R-matrices
(defn plot-r-matrices []
  (let [data (:r_matrices (:current-data @dashboard-state))
        traces (reduce (fn [traces point]
                         (let [L (:L point)]
                           (update traces L
                                    (fn [trace]
                                      (if trace
                                        (-> trace
                                            (update-in [:nuclear :x] conj (:energy point))
                                            (update-in [:nuclear :y] conj (:r_nuclear point))
                                            (update-in [:coulomb-nuclear :x] conj (:energy point))
                                            (update-in [:coulomb-nuclear :y] conj (:r_coulomb_nuclear point)))
                                        {:nuclear {:x [(:energy point)]
                                                   :y [(:r_nuclear point)]
                                                   :name (str "L = " L " (Nuclear)")
                                                   :type "scatter"
                                                   :mode "lines+markers"}
                                         :coulomb-nuclear {:x [(:energy point)]
                                                          :y [(:r_coulomb_nuclear point)]
                                                          :name (str "L = " L " (Coul+Nuc)")
                                                          :type "scatter"
                                                          :mode "lines+markers"
                                                          :line {:dash "dash"}}}))))
                       {} data)
        plot-data (clj->js (mapcat (fn [trace] [(:nuclear trace) (:coulomb-nuclear trace)]) (vals traces)))
        layout (clj->js {:title "R-Matrix Values Comparison"
                         :xaxis {:title "Energy (MeV)" :gridcolor "#e0e0e0"}
                         :yaxis {:title "R-Matrix" :gridcolor "#e0e0e0"}
                         :plot_bgcolor "rgba(0,0,0,0)"
                         :paper_bgcolor "rgba(0,0,0,0)"
                         :font {:family "Arial, sans-serif"}
                         :legend {:x 0.02 :y 0.98}
                         :margin {:t 50 :b 50 :l 60 :r 30}})]
    (.newPlot plotly "rmatrix-plot" plot-data layout (clj->js {:responsive true}))))

;; Plot potentials
(defn plot-potentials []
  (let [data (:potentials (:current-data @dashboard-state))
        woods-saxon (clj->js {:x (map :radius data)
                              :y (map :woods_saxon data)
                              :name "Woods-Saxon"
                              :type "scatter"
                              :mode "lines"
                              :line {:color "blue" :width 3}})
        coulomb (clj->js {:x (map :radius data)
                          :y (map :coulomb data)
                          :name "Coulomb"
                          :type "scatter"
                          :mode "lines"
                          :line {:color "red" :width 3}})
        combined (clj->js {:x (map :radius data)
                            :y (map :combined data)
                            :name "Combined"
                            :type "scatter"
                            :mode "lines"
                            :line {:color "green" :width 3}})
        layout (clj->js {:title "Nuclear Potentials vs Radius"
                         :xaxis {:title "Radius (fm)" :gridcolor "#e0e0e0"}
                         :yaxis {:title "Potential (MeV)" :gridcolor "#e0e0e0"}
                         :plot_bgcolor "rgba(0,0,0,0)"
                         :paper_bgcolor "rgba(0,0,0,0)"
                         :font {:family "Arial, sans-serif"}
                         :legend {:x 0.02 :y 0.98}
                         :margin {:t 50 :b 50 :l 60 :r 30}})]
    (.newPlot plotly "potential-plot" (array woods-saxon coulomb combined) layout (clj->js {:responsive true}))))

;; Plot cross sections
(defn plot-cross-sections []
  (let [data (:cross_sections (:current-data @dashboard-state))
        trace (clj->js {:x (map :energy data)
                        :y (map :total_cross_section data)
                        :name "Total Cross-Section"
                        :type "scatter"
                        :mode "lines+markers"
                        :line {:color "purple" :width 3}
                        :marker {:size 6}})
        layout (clj->js {:title "Total Cross-Sections vs Energy"
                         :xaxis {:title "Energy (MeV)" :gridcolor "#e0e0e0"}
                         :yaxis {:title "Cross-Section (arbitrary units)" :type "log" :gridcolor "#e0e0e0"}
                         :plot_bgcolor "rgba(0,0,0,0)"
                         :paper_bgcolor "rgba(0,0,0,0)"
                         :font {:family "Arial, sans-serif"}
                         :margin {:t 50 :b 50 :l 60 :r 30}})]
    (.newPlot plotly "cross-section-plot" (array trace) layout (clj->js {:responsive true}))))

;; Plot dashboard
(defn plot-dashboard []
  (let [current-data (:current-data @dashboard-state)
        phase-data (:phase_shifts current-data)
        potential-data (:potentials current-data)
        cross-section-data (:cross_sections current-data)
        phase-traces (reduce (fn [traces point]
                               (let [L (:L point)]
                                 (update traces L
                                          (fn [trace]
                                            (if trace
                                              (-> trace
                                                  (update :x conj (:energy point))
                                                  (update :y conj (* (:phase_shift point) (/ 180 js/Math.PI))))
                                              {:x [(:energy point)]
                                               :y [(* (:phase_shift point) (/ 180 js/Math.PI))]
                                               :name (str "L = " L)
                                               :type "scatter"
                                               :mode "lines+markers"
                                               :showlegend true})))))
                             {} phase-data)
        traces (clj->js (concat (vals phase-traces)
                                [(clj->js {:x (map :radius potential-data)
                                            :y (map :woods_saxon potential-data)
                                            :name "Woods-Saxon"
                                            :type "scatter"
                                            :mode "lines"
                                            :xaxis "x2"
                                            :yaxis "y2"
                                            :showlegend false})
                                 (clj->js {:x (map :radius potential-data)
                                           :y (map :coulomb potential-data)
                                           :name "Coulomb"
                                           :type "scatter"
                                           :mode "lines"
                                           :xaxis "x2"
                                           :yaxis "y2"
                                           :showlegend false})
                                 (clj->js {:x (map :energy cross-section-data)
                                           :y (map :total_cross_section cross-section-data)
                                           :name "Cross-Section"
                                           :type "scatter"
                                           :mode "lines"
                                           :xaxis "x3"
                                           :yaxis "y3"
                                           :showlegend false})]))
        layout (clj->js {:title "DWBA Comprehensive Dashboard"
                         :grid {:rows 2
                                :columns 2
                                :subplots [["xy" "x2y2"]
                                           ["xy" "x3y3"]]}
                         :xaxis {:title "Energy (MeV)" :domain [0 0.45]}
                         :yaxis {:title "Phase Shift (degrees)" :domain [0.55 1]}
                         :xaxis2 {:title "Radius (fm)" :domain [0.55 1]}
                         :yaxis2 {:title "Potential (MeV)" :domain [0.55 1]}
                         :xaxis3 {:title "Energy (MeV)" :domain [0 0.45]}
                         :yaxis3 {:title "Cross-Section" :type "log" :domain [0 0.45]}
                         :plot_bgcolor "rgba(0,0,0,0)"
                         :paper_bgcolor "rgba(0,0,0,0)"
                         :font {:family "Arial, sans-serif"}
                         :margin {:t 50 :b 30 :l 50 :r 30}})]
    (.newPlot plotly "dashboard-plot" traces layout (clj->js {:responsive true}))))

;; Plot elastic
(defn plot-elastic []
  (let [data (:elastic (:current-data @dashboard-state))]
    (when (and data (not (empty? data)))
      (let [traces (reduce (fn [traces point]
                             (let [E (:energy point)]
                               (update traces E
                                        (fn [trace]
                                          (if trace
                                            (-> trace
                                                (update :x conj (:angle point))
                                                (update :y conj (:differential-cross-section point)))
                                            {:x [(:angle point)]
                                             :y [(:differential-cross-section point)]
                                             :name (str "E = " E " MeV")
                                             :type "scatter"
                                             :mode "lines+markers"
                                             :line {:width 2}
                                             :marker {:size 4}})))))
                           {} data)
            plot-data (clj->js (vals traces))
            layout (clj->js {:title "Elastic Scattering Differential Cross-Section"
                             :xaxis {:title "Scattering Angle (degrees)" :gridcolor "#e0e0e0"}
                             :yaxis {:title "dσ/dΩ (fm²/sr)" :type "log" :gridcolor "#e0e0e0"}
                             :plot_bgcolor "rgba(0,0,0,0)"
                             :paper_bgcolor "rgba(0,0,0,0)"
                             :font {:family "Arial, sans-serif"}
                             :legend {:x 0.02 :y 0.98}
                             :margin {:t 50 :b 50 :l 60 :r 30}})]
        (.newPlot plotly "elastic-plot" plot-data layout (clj->js {:responsive true}))))))

;; Plot inelastic
(defn plot-inelastic []
  (let [data (:inelastic (:current-data @dashboard-state))]
    (when (and data (not (empty? data)))
      (let [traces (reduce (fn [traces point]
                             (let [L (:L point)]
                               (update traces L
                                        (fn [trace]
                                          (if trace
                                            (-> trace
                                                (update :x conj (:energy point))
                                                (update :y conj (:differential-cross-section point)))
                                            {:x [(:energy point)]
                                             :y [(:differential-cross-section point)]
                                             :name (str "L = " L)
                                             :type "scatter"
                                             :mode "lines+markers"
                                             :line {:width 3}
                                             :marker {:size 6}})))))
                           {} data)
            plot-data (clj->js (vals traces))
            layout (clj->js {:title "Inelastic Scattering Differential Cross-Section"
                             :xaxis {:title "Energy (MeV)" :gridcolor "#e0e0e0"}
                             :yaxis {:title "dσ/dΩ (fm²/sr)" :type "log" :gridcolor "#e0e0e0"}
                             :plot_bgcolor "rgba(0,0,0,0)"
                             :paper_bgcolor "rgba(0,0,0,0)"
                             :font {:family "Arial, sans-serif"}
                             :legend {:x 0.02 :y 0.98}
                             :margin {:t 50 :b 50 :l 60 :r 30}})]
        (.newPlot plotly "inelastic-plot" plot-data layout (clj->js {:responsive true}))))))

;; Plot transfer
(defn plot-transfer []
  (let [data (:transfer (:current-data @dashboard-state))]
    (when (and data (not (empty? data)))
      (let [traces (reduce (fn [traces point]
                             (let [L (:L point)]
                               (update traces L
                                        (fn [trace]
                                          (if trace
                                            (-> trace
                                                (update :x conj (:energy point))
                                                (update :y conj (:differential-cross-section point)))
                                            {:x [(:energy point)]
                                             :y [(:differential-cross-section point)]
                                             :name (str "L = " L)
                                             :type "scatter"
                                             :mode "lines+markers"
                                             :line {:width 3}
                                             :marker {:size 6}})))))
                           {} data)
            plot-data (clj->js (vals traces))
            layout (clj->js {:title "Transfer Reaction Differential Cross-Section"
                             :xaxis {:title "Energy (MeV)" :gridcolor "#e0e0e0"}
                             :yaxis {:title "dσ/dΩ (fm²/sr)" :type "log" :gridcolor "#e0e0e0"}
                             :plot_bgcolor "rgba(0,0,0,0)"
                             :paper_bgcolor "rgba(0,0,0,0)"
                             :font {:family "Arial, sans-serif"}
                             :legend {:x 0.02 :y 0.98}
                             :margin {:t 50 :b 50 :l 60 :r 30}})]
        (.newPlot plotly "transfer-plot" plot-data layout (clj->js {:responsive true}))))))

;; Update dashboard stats
(defn update-dashboard-stats [calculation-time]
  (let [current-data (:current-data @dashboard-state)]
    (when current-data
      (when-let [phase-data (seq (:phase_shifts current-data))]
        (let [total-points (count phase-data)
              energies (map :energy phase-data)
              l-values (distinct (map :L phase-data))]
          (set-text! "total-points" (str total-points))
          (set-text! "energy-range-display" (str (apply min energies) "-" (apply max energies)))
          (set-text! "L-count" (str (count l-values)))
          (set-text! "calculation-time" (str calculation-time "ms")))))))

;; Reset parameters
(defn reset-parameters []
  (load-default-parameters)
  (show-status "Parameters reset to defaults" "info"))

;; Global functions for HTML onclick handlers
(defn ^:export calculateDWBA []
  (calculate-dwba))

(defn ^:export resetParameters []
  (reset-parameters))

;; Initialize dashboard when page loads
(defn init []
  (when (and js/document (.-readyState js/document))
    (initialize-event-listeners)
    (load-default-parameters)))

;; Set up initialization
(if (= (.-readyState js/document) "complete")
  (init)
  (events/listenOnce js/window "load" init))

