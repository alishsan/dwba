                                        ;16Opd
                                        ;16O: R0 = 2.5 fm, V0 = 50 MeV, E = -15.6 MeV, dif = 0.6 fm, l=1
;d: R0 = 1.5 fm, V0 = 50 MeV, E = -2.2 MeV, diff = 0.6 fm, l=0 
(let [v0-i 62. R0-i 2.7 diff-i 0.6 r-max 20. h 0.01 l-i 1
      v0-f 50. R0-f 1.5 diff-f 0.6 l-f 0
;      Es-i (first (map :root (find-bound-state-energy [v0-i R0-i diff-i] l-i r-max h)))
 ;     Es-f (first (map :root (find-bound-state-energy [v0-f R0-f diff-f] l-f r-max h)))
      Es-i -15.67
      Es-f -2.214
      phi-i (solve-bound-state-numerov Es-i l-i v0-i R0-i diff-i h r-max)
      phi-f (solve-bound-state-numerov  Es-f l-f v0-f R0-f diff-f h r-max)]
 ; (mapv   #(form-factor-r % phi-f phi-i h) (range 0. 2. 0.1))
  (normalized-overlap phi-i phi-f r-max h)
 ; (overlap-integral phi-i phi-f r-max h)
  )

