;; Test tricomi function in both namespaces
(require '[fastmath.special :as spec])
(require '[fastmath.special.hypergeometric :as hg])
(require '[fastmath.complex :as cplx])

(println "=== Testing Tricomi Function in Both Namespaces ===")

;; Test in fastmath.special
(println "Testing in fastmath.special:")
(try
  (let [z (cplx/complex 2 -1)
        a 4.0
        b cplx/I
        result (spec/tricomi-U-complex z a b)]
    (println "  spec/tricomi-U-complex(2-i, 4.0, cplx/I) = " result))
  (catch Exception e
    (println "  Error in spec: " (.getMessage e))))

;; Test in fastmath.special.hypergeometric
(println "\nTesting in fastmath.special.hypergeometric:")
(try
  (let [z (cplx/complex 2 -1)
        a 4.0
        b cplx/I
        result (hg/tricomi-U-complex z a b)]
    (println "  hg/tricomi-U-complex(2-i, 4.0, cplx/I) = " result))
  (catch Exception e
    (println "  Error in hg: " (.getMessage e))))

;; Test with different arguments
(println "\nTesting with different arguments:")
(try
  (let [z (cplx/complex 1 0)  ; 1 + 0i
        a 1.0
        b (cplx/complex 0 1)  ; 0 + i
        result (hg/tricomi-U-complex z a b)]
    (println "  hg/tricomi-U-complex(1, 1.0, i) = " result))
  (catch Exception e
    (println "  Error: " (.getMessage e))))

;; List available functions
(println "\nAvailable functions in spec:")
(println "  Functions containing 'tricomi': " 
         (filter #(re-find #"tricomi" (str %)) 
                 (keys (ns-publics 'fastmath.special))))

(println "\nAvailable functions in hg:")
(println "  Functions containing 'tricomi': " 
         (filter #(re-find #"tricomi" (str %)) 
                 (keys (ns-publics 'fastmath.special.hypergeometric))))

(println "\n✅ Test completed!")
