# Branching Agent Architecture

## Mission

Enable the agentic prompter to explore multiple solution paths in parallel, learning from each exploration to make better decisions. When the agent recognizes multiple viable approaches, it can branch its exploration, evaluate outcomes, and synthesize the best solution.

## Core Concepts

### The Branching Agent

The agent gains the ability to:
- Recognize when multiple approaches are viable
- Spawn parallel explorations of different strategies
- Evaluate outcomes based on confidence and success metrics
- Synthesize learnings from all branches into optimal solutions
- Learn from branch outcomes to improve future decisions

### Ex Architecture Integration

Branching leverages the Ex pattern's strengths:
- **Pure Decision Making**: All branching logic is data transformation
- **Controlled Effects**: REPL evaluations and AI calls are managed effects
- **Observable State**: Complete branch tree visible in state atom
- **Promise Coordination**: Promesa handles parallel execution elegantly

## Key Design Decisions

### 1. Namespace Isolation

Each branch executes in its own dynamically created namespace to prevent interference:
- Parent namespace symbols are copied to child
- Branch-specific bindings are isolated
- Results are captured without side effects
- Cleanup happens automatically on branch completion

### 2. Resource Management

Prevent exponential explosion through configurable limits:
- Maximum branch depth (default: 3)
- Maximum concurrent executions (default: 5)
- Timeout per branch (default: 30 seconds)
- Confidence threshold for branching (default: 0.6)

### 3. Evaluation Strategies

The agent can evaluate branches using different strategies:
- **Best**: Select highest confidence result
- **Consensus**: Find common patterns across branches
- **Synthesis**: Combine insights from multiple branches

## Architecture Components

### State Shape

Using flat structure with synthetic namespacing for clarity:

```clojure
{;; Branch registry
 :branch/id->branch {branch-id {:branch/id branch-id
                                :branch/parent parent-id
                                :branch/status :pending|:executing|:completed|:failed
                                :branch/strategy "functional approach"
                                :branch/confidence 0.85
                                :branch/result any?}}

 ;; Execution tracking
 :branch/active-ids #{branch-id ...}
 :branch/parent->children {parent-id [child-id ...]}

 ;; Agent integration
 :agent/current-exploration branch-id
 :agent/branch-history [{:branch/id id :branch/decision "..."}]

 ;; Configuration
 :branch/max-depth 3
 :branch/max-concurrent 5
 :branch/timeout-ms 30000}
```

### Action Catalog

#### Agent Recognition Actions

```clojure
[:agent/ax.analyze-for-branches {:response "I see three approaches: functional, imperative, and hybrid..."}]
;; Detects branching opportunity and extracts strategies

[:agent/ax.initiate-exploration {:strategies [{:approach "functional" :description "..."}
                                             {:approach "imperative" :description "..."}]}]
;; Creates branch structure for exploration
```

#### Branch Lifecycle Actions

```clojure
[:branch/ax.create {:parent-id id :strategies [...]}]
;; Creates new branches with unique IDs

[:branch/ax.execute {:branch/id id}]
;; Triggers branch execution via effects

[:branch/ax.complete {:branch/id id :result {...} :confidence 0.85}]
;; Records branch outcome

[:branch/ax.evaluate {:branch/ids [...] :strategy :synthesis}]
;; Compares and synthesizes branch results
```

#### Learning Actions

```clojure
[:agent/ax.record-branch-outcome {:branch/id id :success? true :insights [...]}]
;; Updates agent's knowledge base

[:agent/ax.apply-learnings {:context "similar problem detected"}]
;; Uses past branch outcomes to inform decisions
```

### Effect Handlers

#### Isolated Execution

```clojure
[:branch/fx.eval-isolated {:branch/id id
                          :code "(solve-problem input)"
                          :namespace 'branch.exploration-1
                          :bindings {'input data}
                          :ex/then [[:branch/ax.complete {:branch/id id ...}]]
                          :ex/catch [[:branch/ax.handle-error {:branch/id id ...}]]}]
```

#### Parallel Coordination

```clojure
[:branch/fx.explore-parallel {:branch/ids [id1 id2 id3]
                             :timeout-ms 30000
                             :ex/then [[:agent/ax.synthesize-results {:branch/ids ids}]]}]
```

### Execution Flow

1. **Recognition Phase**
   - Agent analyzes response for multiple viable approaches
   - Confidence scoring determines if branching is worthwhile
   - Strategies are extracted with clear differentiation

2. **Exploration Phase**
   - Branches created with isolated contexts
   - Parallel execution respecting resource limits
   - Progress monitoring with timeout protection

3. **Evaluation Phase**
   - Results collected from completed branches
   - Synthesis strategy applied (best/consensus/merge)
   - Failed branches analyzed for learnings

4. **Integration Phase**
   - Best solution integrated into presentation flow
   - Learnings recorded for future use
   - Metrics updated for performance tracking

## Implementation Strategy

### Phase 1: Core Branching Infrastructure
- Branch state management actions
- Namespace isolation utilities
- Basic execution effects with Promesa
- Resource limit enforcement

### Phase 2: Agent Intelligence
- Branch opportunity recognition
- Strategy extraction from AI responses
- Confidence scoring algorithms
- Result synthesis logic

### Phase 3: Learning System
- Branch outcome tracking
- Pattern recognition across branches
- Decision improvement over time
- Metrics collection and analysis

### Phase 4: Advanced Features
- Multi-level branching for complex problems
- Dynamic strategy selection
- Cross-branch communication protocols
- Real-time visualization support

## Promise Patterns

Key Promesa patterns for branch coordination:

```clojure
;; Controlled parallel execution
(defn execute-branches-with-limit [branch-ids limit timeout-ms]
  (p/let [chunks (partition-all limit branch-ids)
          results (p/loop [chunks chunks
                          all-results []]
                    (if-let [chunk (first chunks)]
                      (p/let [chunk-results (p/race [(p/all (map execute-branch chunk))
                                                     (p/delay timeout-ms :timeout)])]
                        (p/recur (rest chunks)
                                (into all-results chunk-results)))
                      all-results))]
    results))

;; Branch with error boundary
(defn safe-branch-execution [branch-id]
  (-> (execute-branch branch-id)
      (p/catch (fn [error]
                {:branch/id branch-id
                 :branch/status :failed
                 :branch/error error}))
      (p/then (fn [result]
                [[:branch/ax.complete {:branch/id branch-id
                                      :result result}]]))))
```

## Configuration

```clojure
{;; Core limits
 :branch/enabled true
 :branch/max-depth 3
 :branch/max-concurrent 5
 :branch/timeout-ms 30000

 ;; Intelligence thresholds
 :branch/min-confidence-to-branch 0.6
 :branch/prune-threshold 0.3
 :branch/synthesis-threshold 0.7

 ;; Strategy preferences
 :branch/prefer-functional true
 :branch/allow-recursive-branching true
 :branch/learn-from-failures true}
```

## Benefits

1. **Smarter Solutions**: Agent explores multiple approaches before committing
2. **Learning System**: Each branch contributes to agent knowledge
3. **Controlled Exploration**: Resource limits prevent runaway complexity
4. **Observable Process**: Complete visibility into decision making
5. **Resilient**: Failures isolated to branches, main flow protected

## Integration Points

- **Presentation Flow**: Branches can explore different explanation styles
- **Audio Generation**: Announce significant branching decisions
- **Slide Navigation**: Branch on different presentation paths
- **Error Recovery**: Use branches for fallback strategies

## Next Steps

1. Implement core branch lifecycle actions
2. Create namespace isolation utilities
3. Build promise-based execution effects
4. Integrate recognition logic into agent
5. Design synthesis strategies
6. Add learning system foundation