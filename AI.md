# Project Context: Structure-Based Genetic Programming

This project involves Structure-Based Genetic Programming (SBGP). The following is a precise summary derived from the course slides and the paper:

> Scheepers, D. & Pillay, N. (2026). *A structure-based genetic programming generation constructive hyper-heuristic with transfer learning for combinatorial optimisation.* Genetic Programming and Evolvable Machines, 27:2.

---

## 1. What is SBGP?

Standard (canonical) genetic programming uses **fitness alone** to direct the search — this is based purely on *behaviour* (how well a program performs), not on the *structure* of the programs.

**Structure-Based Genetic Programming** directs the search using **both behaviour (fitness) and structure**. It is motivated by the way GP converges: populations tend to cluster around structurally similar regions, which can cause premature convergence to local optima.

Structure can be taken into consideration:
- As part of the fitness function
- As part of the selection method
- When detecting local optima (e.g. the ISBA algorithm)
- To control exploration and exploitation throughout a GP run

---

## 2. The SBGP Algorithm (from Scheepers & Pillay 2026)

The algorithm alternates between two phases after initialising a population:

### Global Level Search (Exploration)
- Runs canonical GP for `Sg` generations.
- Extracts a **global area** from the best individual found: the first `Dg` levels of the tree (top portion of the syntax tree).
- On the **second and subsequent** global searches, evolution is applied to the first `Dg` levels but offspring are **discarded** if their top `Dg` levels are too similar to any previously exploited global area. This is enforced using the similarity index (see Section 3).

### Local Level Search (Exploitation)
- Runs for `Wg` generations.
- The first `n` levels of **every individual in the population** are fixed to match the identified global area.
- Genetic operators (crossover, mutation) are applied only from level `n+1` downward.
- Terminates early if fitness has not improved for the last `Wg` generations (convergence within the area).

### Loop
This global → local cycle repeats until a maximum number of generations is reached, or the overall algorithm converges (standard deviation of fitness over the last 30 generations is zero).

### Key Parameters (tuned values from the paper)
| Parameter | Symbol | Value | Meaning |
|---|---|---|---|
| Global search generations | `Sg` | 10 | Generations to find a global area |
| Global area depth | `Dg` | 4 | Levels fixed as the global area |
| Global similarity threshold | `Tg` | 6 | Max node matches allowed before offspring is discarded |
| Local window tolerance | `Wg` | 10 | Generations of no improvement before stopping local search |
| Initial transferred tree depth | `DIg` | 6 | Depth of trees seeded from transferred global areas |

---

## 3. Detecting Similarity

Similarity indexes are used to measure structural resemblance between program trees. The index used depends on how structure is being applied.

**The similarity index used in SBGP** is calculated by counting the number of nodes that occupy the **same position** in both trees, comparing from the root downward for the first `Dg` levels.

- A threshold `Tg` is set to define how much similarity is "too much."
- During the second+ global search, if an offspring's top `Dg` levels produce a similarity index >= `Tg` compared to any previously exploited global area, the offspring is **discarded** — this prevents the search from revisiting the same structural region.

---

## 4. Program Representation

Each individual in the population is a **syntax tree**. Trees are composed of:
- **Function set** (internal nodes): `+`, `-`, `x`, `/` (protected), `min`, `<0` (if-less-than-zero)
- **Terminal set** (leaves): problem-specific attributes (e.g. number of exams, bin capacity, distance to depot)

Trees are initialised using the **grow method**. Duplicate trees are not permitted in the initial population. Maximum tree depth is enforced on offspring.

**Selection**: Tournament selection (size 4). If two individuals tie on fitness, the one with fewer nodes wins.

---

## 5. Why Structure Helps: Escape from Local Optima

SBGP was originally introduced to escape local optima that canonical GP cannot escape. When the local search converges within a global area, the global search moves the population to a **structurally different region** of the search space, preventing revisiting of known local optima.

- When SBGP outperforms canonical GP: the global search successfully redirects to a better region after a local optimum is hit.
- When SBGP underperforms canonical GP: the global search moves to a region that does not yield a better optimum. This is a known limitation — identifying better global area selection mechanisms is future work.

---

## 6. Application Context: Generation Constructive Hyper-Heuristics

In the Scheepers & Pillay paper, SBGP is applied to **generation constructive hyper-heuristics** — GP systems that automatically *create* construction heuristics for combinatorial optimisation problems:

- **Examination timetabling problem (ETP)** — ITC 2007 benchmark, 12 instances
- **One-dimensional bin packing problem (ODBPP)** — Scholl and Falkenauer datasets
- **Capacitated vehicle routing problem (CVRP)** — Christofides dataset, 14 instances

Fitness is determined by using the program tree as a construction heuristic to build a solution, and measuring solution quality.

**Key result**: SBGP-HH outperformed canonical GP-HH on the majority of problem instances across all three domains.

---

## 7. Transfer Learning in SBGP (SBGP-HH-TL)

An extension combining SBGP with transfer learning:
- A source SBGP run is performed on simpler problem instances.
- **Global areas** (top `Dg` levels of the best trees) are extracted from source runs.
- These global areas seed the initial population of the target SBGP run — trees are created with a transferred global area fixed at the top, and remaining levels grown randomly.
- SBGP-HH-TL outperformed both SBGP-HH and CGP-HH on the majority of instances.

---

## 8. Terminology Glossary

| Term | Definition |
|---|---|
| Canonical GP (CGP) | Standard GP using fitness only to direct search |
| SBGP | GP using both fitness and structure to direct search |
| Global area | The top `Dg` levels of a program tree, used as a structural anchor |
| Global level search | Exploration phase — finds a new global area |
| Local level search | Exploitation phase — evolves within a fixed global area |
| Similarity index | Count of nodes in the same position in two trees (within `Dg` levels) |
| Threshold `Tg` | Maximum similarity index allowed before an offspring is discarded |
| Generation constructive hyper-heuristic | A system that automatically *creates* construction heuristics using GP |
| Terminal set | Leaf-level problem attributes available to the GP |
| Function set | Arithmetic/logical operators available as internal nodes |
| Transfer learning | Transferring global areas from a source GP run to initialise a target GP run |
| ISBA | Iterative Structure-Based Algorithm — uses structure to detect local optima |