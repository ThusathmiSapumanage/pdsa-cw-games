# Minimum Cost Assignment Game – Team Sprint Plan
**Team:** Chaniru & Rumeth  
**Project:** ALGOCORE (PDSA Coursework)

This file tracks the synchronized development progress for the Minimum Cost Assignment Game.

---

## Sprint Schedule (1 Week)

### Day 1 – Foundation & Matrix Setup
*   **Chaniru:** [x] Create backend module structure (folders, controllers, services).
*   **Chaniru:** [x] Implement Matrix Generator (N=50-100, Values=20-200).
*   **Rumeth:** [x] Create Frontend page layout (`GameLayoutShell`).
*   **Rumeth:** [x] Implement visual Matrix Grid component.

### Day 2 – Greedy Strategy & Highlighting
*   **Rumeth:** [x] Implement Greedy algorithm with row/column blocking logic.
*   **Rumeth:** [x] Store sequential assignments and total costs.
*   **Chaniru:** [x] Add visual cell highlighting for assigned tasks.
*   **Chaniru:** [x] Display dimmed overlays for blocked rows/columns.
*   **Chaniru:** [x] Implement interactive log panel to track greedy decisions.

### Day 3 – Optimal Hungarian & Metrics
*   **Chaniru:** [x] Implement Hungarian Algorithm ($O(N^3)$) with matrix reduction logic.
*   **Rumeth:** [x] Create responsive results panel for optimal totals.
*   **Rumeth:** [x] Integrate side-by-side execution timer displays.

### Day 4 – Live Integration
*   **Rumeth:** [x] Build API orchestrator (`/api/mincost/start`) returning both algorithms.
*   **Rumeth:** [x] Synchronously map execution timings to JSON response.
*   **Chaniru:** [x] Connect `page.tsx` to Spring Boot using Fetch/Axios.
*   **Chaniru:** [x] Add loading states/spinners and "Execute" trigger logic.

### Day 5 – Dynamic Step Visualization
*   **Chaniru:** [ ] Implement structural decision tracking for "Step-by-Step" mode.
*   **Chaniru:** [ ] Format backend output for smooth ingestion by frontend hooks.
*   **Rumeth:** [ ] Implement paused step-by-step UI rendering logic.
*   **Rumeth:** [ ] Build "Playback Toolbar" (Next Step, Prev, Play/Pause).
*   **Rumeth:** [ ] Present localized logs explaining *why* each choice was made.

### Day 6 – Persistence & Analytics
*   **Rumeth:** [ ] Create DB Entity/Repository for execution results.
*   **Rumeth:** [ ] Setup silent `.save()` triggers in the API.
*   **Rumeth:** [ ] Construct `GET` endpoint for historical statistics.
*   **Chaniru:** [ ] Implement responsive analytical charts (Bar/Line).
*   **Chaniru:** [ ] Graph time efficiency and cost margins from history API.

### Day 7 – Final Polish & Validation
*   **Chaniru:** [ ] Run load testing (100x100 constraints) on JVM.
*   **Chaniru:** [ ] **IMPORTANT:** Add robust exception handling and edge-case logic.
*   **Rumeth:** [ ] Apply professional UI polish (padding, transitions).
*   **Rumeth:** [ ] **IMPORTANT:** Final responsiveness and accessibility check.

---

## Project Analysis & Critical Improvements

### 1. Mandatory Input Validations
*   **Current State:** Plan assumes random generation.
*   **Required Change:** Allow users to define `N` and `Cost Range`.
*   **Implementation:** Use `AlgoInput` with real-time validation ($5 \le N \le 100$). Show "System Generated" error messages if the input is invalid.

### 2. Algorithmic Demonstration (Score Boost)
*   **Recommendation:** The Hungarian algorithm reduction steps are complex. Visualizing the **Zeros**, **Circles/Squares (Assignments)**, and **Lines (Covering)** will significantly improve the "Algorithms" section of your coursework.
*   **Action:** Add a "Reduction Phase" visualizer to the Step-by-Step mode.

### 3. Performance & Responsiveness
*   **Note:** 100x100 is 10k cells. Ensure the Matrix Grid uses `overflow: auto` and does not bloat the DOM.
*   **Accessibility:** Ensure all interactive elements have unique IDs and ARIA labels.

### 4. Professional UI Rules
*   **No Pure Black:** Use OLED black (`#050505`) as defined in the guide.
*   **No Grey Cards:** Use surface hex `#0A0A0A`.
*   **Alerts:** Use `AlgoNotify` for win/loss/error states. Never use `alert()` or `localhost` messages.

---
