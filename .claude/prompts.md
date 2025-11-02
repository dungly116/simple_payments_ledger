# AI Collaboration & Prompting Guide

**Project:** Simple Payments Ledger API
**Time Investment:** ~3 hours | **AI Contribution:** 58% | **Human Contribution:** 42%

---

## Key Prompts Used

### 1. Initial Scaffolding
```
Build a FastAPI payment ledger with:
- POST /accounts (create with initial balance)
- GET /accounts/{id} (retrieve account)
- POST /transactions (transfer funds)
- Use in-memory storage (dict)
- Use Decimal for money (not float)
- Repository pattern for easy DB migration
```

**AI Output:** Boilerplate code, basic structure
**Human Refinement:** None needed - AI excels at this

---

### 2. Thread Safety (Iteration 1 - Failed)
```
Make transfers thread-safe
```

**AI Output:**
```python
# Race condition - NOT ATOMIC
if balance < amount:  # Check
    raise Exception()
balance -= amount     # Update - Race window here
```

**Problem:** Check-then-act pattern, not atomic
**Why AI Failed:** Doesn't inherently understand thread interleaving

---

### 3. Thread Safety (Iteration 2 - Improved)
```
Balance check and update must be atomic.
Use locks to prevent concurrent modifications.
Acquire locks on both accounts before transfer.
```

**AI Output:** Added locks, but locked in transfer direction (A→B, then B→A)
**Problem:** Deadlock risk when Thread1: A→B, Thread2: B→A

---

### 4. Thread Safety (Iteration 3 - Correct)
```
Acquire locks on both accounts in sorted order by account ID.
Explain why lock ordering prevents deadlocks.
```

**AI Output:** ✅ Correct implementation
**Human Addition:** Added comments explaining deadlock prevention mechanism

---

### 5. Financial Precision
```
Use Decimal for financial calculations.
Validate amounts have max 2 decimal places (financial precision).
Reject amounts like 10.123 with 400 Bad Request.
```

**Why Needed:** AI used `float` initially (0.1 + 0.2 != 0.3 issue)
**Lesson:** Domain constraints must be explicit

---

### 6. Test Generation
```
Write tests for:
- Happy path (successful transfer)
- Insufficient funds
- Account not found
- Concurrent transfers from same account (race condition test)
- Deadlock prevention (bidirectional transfers)
- Money conservation across 100 random transfers
Use pytest, AAA pattern, and Barrier for concurrent tests
```

**AI Output:** ✅ Comprehensive test suite
**Human Validation:** Broke code to verify tests actually catch bugs

---

## Effective Prompting Patterns

### ❌ Vague Prompts
```
Make it thread-safe
Add proper validation
Fix the bug
```

**Result:** Incomplete or incorrect solutions

---

### ✅ Specific Prompts
```
Balance check and update must be atomic.
Acquire locks on both accounts in sorted order.
Explain why ordering prevents deadlocks.
```

**Pattern:**
- State exact requirement
- Specify mechanism if known
- Ask for explanation (validates AI understanding)

---

### ❌ Assume Domain Knowledge
```
Add financial validation
```

**Result:** AI doesn't know finance needs Decimal, 2 decimal places, etc.

---

### ✅ Explicit Domain Constraints
```
Use Decimal (not float) for exact financial precision.
Validate amounts have max 2 decimal places.
Example: 10.50 valid, 10.123 invalid.
```

**Pattern:** State domain rules + examples

---

## AI Strengths

### 1. Boilerplate Generation (⭐⭐⭐⭐⭐)
- FastAPI routes, Pydantic models
- Repository pattern implementation
- Test fixtures, setup/teardown
- **Time Saved:** ~2 hours

### 2. Standard Patterns (⭐⭐⭐⭐⭐)
- Dependency injection
- Service layer separation
- **When told which pattern:** Implementation is correct

### 3. Test Generation (⭐⭐⭐⭐⭐)
- Happy path + edge cases
- AAA pattern (Arrange-Act-Assert)
- Comprehensive coverage

---

## Human Critical Contributions

### 1. Concurrency Correctness
**AI Limitation:** Doesn't inherently understand race conditions, deadlocks
**Human Role:**
- Identified check-then-act race condition
- Specified lock ordering for deadlock prevention
- Validated atomicity guarantees

**Impact:** Prevents negative balances, system hangs

---

### 2. Domain Knowledge Injection
**AI Limitation:** No inherent finance domain knowledge
**Human Role:**
- Specified Decimal instead of float
- Enforced 2 decimal place precision
- Defined business rules (non-negative balance, atomicity)

**Impact:** Financial accuracy, regulatory compliance

---

### 3. Test Validation
**AI:** Writes tests that pass
**Human:** Validates tests actually catch bugs

**Methodology:**
```
Write test → passes → break code → test fails ✅ → restore
```

**Without this:** No proof tests detect real bugs

---

### 4. Tradeoff Analysis
**AI:** Lists options (global lock vs account-level locks)
**Human:** Contextualizes for scale, team size, timeline

**Decision:** Account-level locks (100K users → parallelism > simplicity)

---

## Iteration Workflow

```
Prompt → AI Draft → Identify Issues → Refined Prompt → Better Output
                           ↓
                    Human Enhancement
```

**Example: Transfer Implementation**
1. "Implement transfer" → Race condition
2. "Fix race with locks" → Deadlock risk
3. "Add lock ordering" → Correct ✅
4. Human adds explanatory comments

**Expectation:** 2-3 iterations for complex concurrency logic

---

## Time Breakdown

| Phase | Time | AI% | Human% | Notes |
|-------|------|-----|--------|-------|
| Setup & Boilerplate | 30m | 70% | 30% | FastAPI, models, repos |
| Concurrency Logic | 75m | 40% | 60% | Critical thinking needed |
| Testing | 45m | 60% | 40% | AI writes, human validates |
| API Layer | 30m | 80% | 20% | Routes, error handling |
| Documentation | 40m | 50% | 50% | AI drafts, human refines |

**Key Insight:** AI for speed, human for correctness

---

## What AI Is / Isn't

**AI IS:**
- ✅ Code generation accelerator
- ✅ Pattern implementation tool
- ✅ Boilerplate eliminator
- ✅ First draft generator

**AI ISN'T:**
- ❌ Concurrency correctness expert
- ❌ Domain knowledge source
- ❌ Architecture decision-maker
- ❌ Output validator (human must verify)

---

## Recommendations for Future Projects

**Do:**
1. Use AI for scaffolding and boilerplate
2. Review critically for domain-specific issues
3. Validate with tests (break code to prove tests work)
4. Iterate with specific, refined prompts
5. Inject domain knowledge explicitly

**Don't:**
1. Blindly trust AI output (especially concurrency)
2. Skip test validation step
3. Let AI make architecture decisions without context
4. Assume AI knows your domain

---

## Code Review Quality Signals

**Strong Submission:**
- ✅ Identified and fixed AI's race conditions
- ✅ Tests fail when code is intentionally broken
- ✅ Explains why design decisions matter for scale
- ✅ Documents tradeoffs with context

**Weak Submission:**
- ❌ "AI wrote it, I ran it, it worked"
- ❌ Tests don't catch bugs when code breaks
- ❌ No tradeoff analysis or design rationale
- ❌ No evidence of critical evaluation

---

## Key Learnings

1. **Domain expertise must be injected**
   - AI doesn't know finance requires Decimal
   - AI doesn't inherently understand race conditions
   - Human provides context AI can't infer

2. **Verification is essential**
   - AI claims "thread-safe" → must be proven
   - Break-it-to-prove-it validates tests

3. **Tradeoffs need context**
   - AI lists options equally
   - Human decides based on 100K vs 10M scale

4. **Iteration is expected**
   - Complex logic takes 2-3 rounds
   - Not a failure - normal collaboration process

---
