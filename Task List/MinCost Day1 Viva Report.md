# MinCost Day 1 Viva Report

Team: Chaniru + Rumeth  
Module: PDSA CW  
Focus: Day 1 code understanding (full study notes)

---

## 1) Day 1 Scope and Completion

From `Task List/Team MinCost.md`, Day 1 had 4 tasks:

1. Chaniru - Create backend module structure  
2. Chaniru - Implement matrix generator  
3. Rumeth - Create frontend page layout  
4. Rumeth - Implement visual matrix grid

All 4 are marked done in the task file.

---

## 2) Files Covered in This Report

### Chaniru (Backend + Integrated page)
- `games-backend/src/main/java/com/pdsa/games/mincost/MinCostController.java`
- `games-backend/src/main/java/com/pdsa/games/mincost/MinCostService.java`
- `frontend/app/games/minimum-cost/page.tsx`

### Rumeth (Day 1 UI components)
- `frontend/src/games/MinCost/GameLayoutShell.jsx`
- `frontend/src/games/MinCost/MatrixGrid.jsx`

---

## 3) High-Level Flow (Easy Story for Viva)

1. User enters `N`, min cost, max cost in UI.
2. Frontend sends POST request to backend endpoint: `/api/mincost/start`.
3. Backend validates values.
4. Backend generates random `N x N` matrix.
5. Backend runs:
   - Greedy assignment
   - Hungarian assignment
6. Backend measures execution times.
7. Backend sends matrix + totals + assignments + logs back to frontend.
8. Frontend shows both matrix results side by side.
9. Frontend highlights selected cells and shows totals/time.

---

## 4) Backend Detailed Breakdown (Chaniru)

## 4.1 `MinCostController.java`

### Annotations
- `@RestController`: class handles HTTP API requests.
- `@RequestMapping("/api/mincost")`: base path for this game.
- `@CrossOrigin(origins = "*")`: allows frontend to call backend from another origin/port.

### Dependency
- `private final MinCostService minCostService;`
  - Controller keeps API logic thin.
  - Core business logic is delegated to service.

### Endpoint
- `@PostMapping("/start")`
  - Final full route: `/api/mincost/start`.
  - Accepts optional request body:
    - `n`
    - `minCost`
    - `maxCost`

### Method Logic (`start(...)`)
1. If body is null, create default request object.
2. Call `minCostService.startGame(payload)`.
3. Return 200 OK with data when success.
4. Catch `IllegalArgumentException` -> return 400 with error message.
5. Catch any other exception -> return 500 with generic message.

This is clean MVC style:
- Controller = HTTP input/output
- Service = algorithm + processing

---

## 4.2 `MinCostService.java`

This file has the full game engine.

### Fields
- `Random random`
  - Used for random matrix values and random `N`.
- `JdbcTemplate jdbcTemplate`
  - Used for direct SQL calls (simple DB writes/reads).

### Constructor
- Injects `JdbcTemplate` from Spring.

### Main Method: `startGame(MinCostStartRequest request)`
This is the central method.

Step-by-step:
1. Pick `n` using `pickN(request.n())`.
2. Pick cost limits (defaults `20..200` if not sent).
3. Validate using `validateInputs(...)`.
4. Build random matrix using `buildRandomMatrix(...)`.
5. Run greedy and time it with `System.nanoTime()`.
6. Run Hungarian and time it with `System.nanoTime()`.
7. Convert time to ms with `nanosToMs(...)`.
8. Try to save run in DB via `saveRunSafely(...)`.
9. Return `MinCostStartResponse` with all outputs.

### Helper: `pickN(Integer requestedN)`
- If user sends `N`, use it.
- Else generate random `50..100` (CW asked random 50 to 100 each round).

### Helper: `validateInputs(int n, int minCost, int maxCost)`
Checks:
- `N` between `5` and `100`
- costs must be positive
- min cost cannot be greater than max cost

If invalid -> throws `IllegalArgumentException`.

### Helper: `buildRandomMatrix(int n, int minCost, int maxCost)`
- Creates `int[n][n]`.
- Fills each cell with:
  - `minCost + random.nextInt(maxCost - minCost + 1)`
- So values are within inclusive range.

### Algorithm 1: `runGreedy(int[][] matrix)`
Logic:
1. Maintain arrays:
   - `usedRows[]`
   - `usedCols[]`
2. For each step:
   - scan all still-free cells
   - choose minimum value
3. Mark that row and column as used.
4. Add assignment and update total.
5. Add user-friendly step log like:
   - `Step 1: picked Tx -> Ey (cost z)`

Important viva point:
- Greedy is fast and simple.
- But not always globally optimal.

### Algorithm 2: `runHungarian(int[][] costs)`
Logic used:
- O(N^3) Hungarian matching with potentials (`u`, `v`) and path arrays (`p`, `way`).

Core phases:
1. Initialize helper arrays.
2. Build matching row by row.
3. Compute reduced costs and update minimum values.
4. Update potentials with `delta`.
5. Rebuild augmenting path.
6. Convert result to row -> column assignments.
7. Calculate final total cost.

Important viva point:
- This gives optimal minimum assignment.
- More complex than greedy, but better final answer.

### Helper: `nanosToMs(long nanos)`
- Converts nanoseconds to milliseconds.
- Rounds to 3 decimals.

### Persistence: `saveRunSafely(...)`
Goal:
- Save one round in DB, but never break gameplay if DB has issue.

Flow:
1. Find game id for `"Minimum Cost"`.
2. Insert game row if missing.
3. Insert `"System"` player.
4. Insert game session.
5. Insert mincost output row.
6. Ensure algorithm rows exist:
   - `"Minimum Cost - Greedy"`
   - `"Minimum Cost - Hungarian"`
7. Insert times into `Algorithm_Execute`.
8. Catch any exception silently.

Viva note:
- This is practical for dev stage: app still works even when local DB not ready.

### DTO/Record Types
- `MinCostStartRequest` -> request input from frontend.
- `AssignmentPair` -> one picked assignment with cost.
- `MinCostStartResponse` -> full output to frontend.
- `AssignmentResult` -> internal helper return object.

---

## 5) Frontend Detailed Breakdown (Chaniru Integrated Page)

File: `frontend/app/games/minimum-cost/page.tsx`

### Imports and Types
- Uses shared layout + input + notify components.
- Defines:
  - `Assignment`
  - `ApiResponse`
- These keep TypeScript strict and avoid wrong property usage.

### `MatrixTable` Component
Purpose:
- Show a matrix in table format.
- Highlight selected assignment cells.

How:
1. Build `assignmentSet` with keys like `"row-col"`.
2. Loop matrix rows/cols.
3. If current key exists in set -> apply highlight style.

### State Variables
- Inputs:
  - `n`, `minCost`, `maxCost`
- UI:
  - `isLoading`
  - `result`
  - `notify` popup object

### Main Action: `runGame()`
Step-by-step:
1. Convert input strings to numbers.
2. Frontend validation:
   - `N` integer in `5..100`
   - cost range valid and positive
3. Set loading true.
4. Send `fetch` POST to backend.
5. Parse JSON response.
6. If backend error -> throw and show notify.
7. If success:
   - save result in state
   - show success notify
8. Finally set loading false.

### UI Rendering
- Uses `GameLayoutShell` (consistent design with project style).
- `controls` area:
  - 3 input fields + execute button
- `visualization` area:
  - before run: message
  - after run:
    - Greedy matrix
    - Hungarian matrix
    - step logs
- `results` panel:
  - status
  - totals
  - timings
- `AlgoNotify`:
  - displays success/error feedback

---

## 6) Rumeth Day 1 UI Files Breakdown

## 6.1 `frontend/src/games/MinCost/GameLayoutShell.jsx`

What it does:
- Builds a simple day-1 layout:
  - header/title
  - Start Game button
  - Next Step button
  - 2 matrix sections
  - algorithm log panel

Props:
- `matrix`, `logs`, `onStartGame`, `onNextStep`

How render works:
1. Show top controls and title.
2. Render two `MatrixGrid` components.
3. Render `AlgorithmLog`.

Why important for Day 1:
- This is base UI structure for mincost view.

## 6.2 `frontend/src/games/MinCost/MatrixGrid.jsx`

What it does:
- Renders matrix in table format.

Key logic:
1. Check if matrix has rows:
   - `hasRows`
2. Compute column count from first row.
3. Table header:
   - employee labels `E1, E2, ...`
4. Body:
   - row labels `T1, T2, ...`
   - each cell value
5. If matrix empty:
   - show `"No matrix data yet"`

Why important for Day 1:
- This is the visual grid component task.

---

## 7) Data and Validation Summary

### Input constraints currently enforced
- `N`: 5 to 100
- min/max cost: positive and `min <= max`

### CW relation
- CW says N random 50..100 each round.
- Current code supports both:
  - default random mode
  - custom input mode (for testing/demo and validation)

### Timing capture
- Both algorithms timed with `System.nanoTime()`.
- Returned to UI in milliseconds.
- Also attempted DB save.

---

## 8) Complexity Notes (Viva Ready)

- Matrix generation:
  - Time: O(N^2)
  - Space: O(N^2)

- Greedy assignment:
  - Current scan-every-step approach approx O(N^3)
  - Space: O(N) + result list
  - Not guaranteed optimal

- Hungarian assignment:
  - Time: O(N^3)
  - Space: O(N^2) (including matrix + helper structures)
  - Guarantees optimal minimum assignment

---

## 9) What to Say in Viva (Short Answers)

### Q: Why two algorithms?
A: CW asks two approaches. We used Greedy and Hungarian to compare simple vs optimal.

### Q: Why Greedy can fail?
A: It picks local minimum each step. Local best choice can block better global combination.

### Q: Why Hungarian is better?
A: It solves assignment globally and guarantees minimum total cost.

### Q: How did you validate input?
A: Frontend and backend both check ranges and bad values. Backend is final safety.

### Q: Where are execution times measured?
A: In backend service using `System.nanoTime()` before and after each algorithm.

### Q: What happens if DB is down?
A: Save logic is wrapped safely, so gameplay continues and API still returns results.

---

## 10) Current Day 1 Status

Day 1 goals are achieved:
- Backend structure: done
- Matrix generation: done
- Frontend layout: done
- Matrix grid component: done

Also now there is extra progress beyond Day 1 (algorithm run + integrated page) which helps for next sprint days.

---

## 11) Quick Revision Plan (Before Viva)

1. Read `startGame(...)` flow once end-to-end.
2. Practice explaining Greedy with one 3x3 sample matrix.
3. Practice one line explanation of Hungarian:
   - "finds global optimal assignment in O(N^3)".
4. Run UI once and explain each panel on screen.
5. Be ready to explain why both frontend and backend validations are used.

---

Prepared for: Chaniru + Rumeth  
Purpose: Viva-friendly understanding of Day 1 MinCost implementation

