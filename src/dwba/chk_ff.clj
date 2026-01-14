(let [v0 90. R0 2. diff 0.6 r-max 20. h 0.01 l 0
      Es (map :root (find-bound-state-energy [v0 R0 diff] l r-max h))
phi-i (solve-bound-state-numerov (first Es) l v0 R0 diff h r-max)
phi-f (solve-bound-state-numerov (second Es) l v0 R0 diff h r-max)
]
  (mapv   #(form-factor-r % phi-f phi-i h) (range 0. 2. 0.1))

)
