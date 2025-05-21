<div class="slide content-heavy">

# BONUS POINTS: USE INTERACTIVE DEBUGGING TECHNIQUES

<div class="content-area">

- A.k.a. "inline def debugging" (the power move, you love it)
- Instrument functions with inline defs to capture bindings with their existing names:
   ```clojure
   (defn process-data [items]
      (def items items) ; Capture input with its existing name
      (let [result (->> items
                        (filter :active)
                        (map :value))]
      (def result result) ; Capture output with its existing name
      result))
   ```
- Prefer capturing values over printing them when possible
- **Important**: Only do this in the code you send to the repl. Leave the code in the file as it is and only instrument the code you evaluate.
- After the function has been run, you (including the user) can evaluate expresions using the inline defined values to understand problems
- Use targeted inline defs with conditionals to capture specific data of interest:
   ```clojure
   (defn process-transactions [txns]
     (map (fn [txn]
            ;; Capture only the transaction with a specific ID
            ;; This works around the last-iteration problem
            (when (= "TX-123456" (:id txn))
              (def txn txn)) ;; captures the `txn` of interest
            (process-txn txn)))
         txns))
   ```

</div>
</div>
