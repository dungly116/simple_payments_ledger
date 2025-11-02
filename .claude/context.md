## Project Scope

**Target Scale:** Grab's early days (~100K users)

**Current Implementation (Phase 1):**
- ✅ **In-memory storage** using thread-safe dictionaries
- ✅ **Repository pattern** for storage abstraction
- ✅ Account-level locking for high concurrency
- ✅ Atomic transfers with deadlock prevention

**Design Philosophy:**
> "Do not use an external database but you may write the project so that is easy to move to using DB as solution."

This implementation uses in-memory storage for simplicity and performance at 100K user scale, while maintaining a repository pattern that allows seamless migration to persistent storage (PostgreSQL, MySQL, etc.) in future phases without touching business logic.

**Note:** Database references (PostgreSQL, MySQL) appear only in future scalability discussions. Current implementation is 
**pure in-memory** as per requirements.

**Constraints:**
- No external database (in-memory only)
- Must be thread-safe with high concurrency
- Account balances never negative
- Transfers must be atomic (all-or-nothing)
---

## Table of Contents
1. [Requirements](#requirements)
2. [Design Decisions](#design-decisions)
3. [Testing Strategy](#testing-strategy)

---

# Requirements

## Domain Models

### Account
- **ID:** string (unique, auto-generated)
- **Balance:** Decimal (non-negative)
- **Lock:** threading.Lock (one per account)

### Transaction
- **ID:** string (unique, auto-generated)
- **From Account ID:** string
- **To Account ID:** string
- **Amount:** Decimal (max 2 decimal places)
- **Status:** pending / completed / failed
- **Timestamp:** datetime
- **Immutability:** Cannot be modified after creation

---

## Business Rules

### Account Creation
- Initial balance must be >= 0
- Unique ID auto-generated
- Each account gets its own lock

### Fund Transfers
- Both accounts must exist (404 if not)
- Sender balance >= amount (400 if insufficient)
- Amount > 0 and max 2 decimal places
- Operation is atomic (all-or-nothing)
- Transaction record always created

### Balance Integrity
- Never negative (enforced by atomicity)
- Money conservation (total sum constant across all accounts)
- No race conditions (prevented by locking)

---

## API Endpoints

**Interactive Docs:**
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

### POST /accounts
**Request:**
```json
{
  "initial_balance": "100.00"
}
```

**Response (201):**
```json
{
  "id": "acc_1a2b3c",
  "balance": "100.00"
}
```

---

### GET /accounts/{id}
**Response (200):**
```json
{
  "id": "acc_1a2b3c",
  "balance": "50.00"
}
```

**Response (404):**
```json
{
  "detail": "Account not found"
}
```

---

### POST /transactions
**Request:**
```json
{
  "from_account_id": "acc_1a2b3c",
  "to_account_id": "acc_4d5e6f",
  "amount": "25.50"
}
```

**Response (201):**
```json
{
  "id": "txn_7g8h9i",
  "from_account_id": "acc_1a2b3c",
  "to_account_id": "acc_4d5e6f",
  "amount": "25.50",
  "status": "COMPLETED",
  "timestamp": "2025-10-31T12:00:00Z"
}
```

**Error Codes:**
- `AccountNotFoundException` → 404
- `InsufficientFundsException` → 400
- `InvalidAmountException` → 400

---

## Concurrency Requirements

### Thread-Safety Guarantees
- **Account-level locking** (not global lock)
- **Lock ordering** by account ID (deadlock prevention)
- **No race conditions** on balance checks
- **No deadlocks** under any concurrent access pattern

### Atomic Transfer Critical Section
```python
1. Acquire locks on both accounts (in sorted order)
2. Check sender balance >= amount
3. Debit sender
4. Credit receiver
5. Release locks (reverse order)
```

**All 5 steps must complete or none do (atomicity).**

---

## Data Types

### Money = Decimal
```python
from decimal import Decimal

# CORRECT
amount = Decimal("10.50")

# WRONG - floating point precision errors
amount = 10.50  # 0.1 + 0.2 != 0.3
```

### Validation
- Max 2 decimal places (e.g., 10.50 ✅, 10.123 ❌)
- Positive for transfers (amount > 0)
- Non-negative for balances (balance >= 0)

---

## Validation Checklist

- [ ] Thread-safe operations
- [ ] No race conditions
- [ ] No deadlocks (lock ordering implemented)
- [ ] Money conservation across all transfers
- [ ] Tests fail when code is intentionally broken
- [ ] Decimal precision (max 2 decimals)
- [ ] No negative balances possible
- [ ] Design decisions documented

---

# Design Decisions

## Decision 1: Account-Level Locks vs Global Lock

### Options Considered

#### Option A: Global Lock (Rejected)
```python
self._global_lock = Lock()

def transfer(...):
    with self._global_lock:
        # All transfers sequential
```

**Pros:**
- Simple implementation
- No deadlock risk
- Easy to reason about

**Cons:**
- Serializes all transfers (kills throughput)
- Concurrent transfers on different accounts still blocked
- Poor scalability

---

#### Option B: Account-Level Locks (Chosen ✅)
```python
# Each account has its own lock
first_account.get_lock().acquire()
second_account.get_lock().acquire()
# Only conflicting transfers wait
```

**Pros:**
- Parallel execution on different accounts
- High throughput (scales with account count)
- Realistic for 100K users

**Cons:**
- Deadlock risk (requires lock ordering)
- More complex implementation
- Harder debugging

---

### Comparison Table

| Aspect | Global Lock | Account-Level Locks |
|--------|-------------|---------------------|
| **Simplicity** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Throughput** | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Deadlock Risk** | None | Requires prevention |
| **Scalability** | Poor | Good |
| **100K Users** | Bottleneck | ✅ Suitable |

---

### Final Decision

**Chosen: Account-Level Locks**

**Reasoning:**
- 100K users with low contention per account
- Parallelism benefit > implementation complexity
- Scales better for future growth

**When to Reconsider:**
- **<10 concurrent requests:** Global lock simpler, negligible perf difference
- **10M+ users, high contention:** Move to optimistic locking + DB transactions

**Implementation:** `app/services/transfer.py:67-90`

---

## Decision 2: Lock Ordering for Deadlock Prevention

### The Deadlock Problem

**Scenario:**
```
Thread 1: Lock Account A → Wait for Account B
Thread 2: Lock Account B → Wait for Account A
Result: DEADLOCK (circular wait)
```

**Visual:**
```
Thread 1: [A] --waits--> [B]
           ↑___________|
Thread 2: [B] --waits--> [A]
```

---

### Solution: Lock Ordering

**Always acquire locks in sorted order by account ID:**

```python
# Determine lock order
first = from_acc if from_acc.id < to_acc.id else to_acc
second = to_acc if from_acc.id < to_acc.id else from_acc

# Acquire in sorted order
first.get_lock().acquire()
second.get_lock().acquire()

# Critical section (balance check, transfer)

# Release in reverse order
second.get_lock().release()
first.get_lock().release()
```

---

### Why This Works

**Without ordering:**
```
Transfer A→B: Lock A → Lock B
Transfer B→A: Lock B → Lock A
Result: Deadlock (circular dependency)
```

**With ordering:**
```
Transfer A→B: Lock A → Lock B
Transfer B→A: Lock A (waits) → Lock B → execute
Result: Linear wait chain, no cycle
```

**Key Insight:** Consistent global ordering eliminates circular waits.

---

### Alternatives Considered (Rejected)

#### A. Timeout + Retry
```python
if not lock.acquire(timeout=1):
    retry()
```
- **Pro:** Detects deadlocks
- **Con:** Doesn't prevent, non-deterministic, CPU waste

#### B. Try-Lock
```python
if lock.acquire(blocking=False):
    proceed()
else:
    retry()
```
- **Pro:** Non-blocking
- **Con:** Live-lock risk (both keep retrying), CPU waste

#### C. Lock Ordering ✅
- **Pro:** Deterministic prevention, no retry overhead
- **Con:** Must maintain ordering discipline

**Decision:** Lock ordering is most robust.

---

### Implementation Details

**File:** `app/services/transfer.py:75-105`

**Methods:**
- `_execute_transfer()` - Implements lock ordering
- `transfer()` - Public entry point

**Test:** `app/tests/test_concurrency.py::test_deadlock_prevention`
- Thread 1: Transfer A↔B 100 times
- Thread 2: Transfer B↔A 100 times
- Timeout: 5 seconds
- Result: Completes without hanging

**Validation:** Removed lock ordering → test hangs → proves necessity

---

## Tradeoffs Summary

### What We Gained
- High concurrency throughput
- Parallel execution on different accounts
- Deterministic deadlock prevention
- Scalability to 100K+ users

### What It Cost
- Increased code complexity
- Lock ordering discipline required
- Harder debugging (concurrent bugs)
- More testing needed

### Worth It?
**Yes for 100K users.**
- Parallelism gains outweigh complexity.
- Essential for production workloads.

**No for <1K users.**
- Global lock simpler, performance difference negligible.

---

## Scalability Path

### Phase 1: 100K Users (Current ✅)
- In-memory storage
- Account-level locks
- Single instance

### Phase 2: 1M Users
- Add PostgreSQL for persistence
- Keep in-memory cache
- Same locking strategy
- Read replicas for GET requests

### Phase 3: 10M+ Users
- Optimistic locking with DB transactions
- Event sourcing for audit trail
- Database sharding by account ID
- Redis for distributed caching

### Phase 4: Global Scale
- Distributed consensus (Raft/Paxos)
- Multi-region deployment
- Event streaming (Kafka)
- CQRS pattern

---

# Testing Strategy

## Philosophy

**Tests must fail on broken code.**

**Verification Process:**
```
1. Write test
2. Run test → passes ✅
3. Break code intentionally
4. Run test → fails ✅ (proves test catches bugs)
5. Restore code
6. Run test → passes ✅
```

**Without this validation:** No proof tests detect real bugs.

---

## Test Pyramid

```
    /\
   /  \  Integration Tests (10%) - API endpoints
  /____\
 /      \ Unit Tests (60%) - Business logic
/________\
Concurrency Tests (30%) - Thread-safety
```

**Rationale:**
- Unit tests fast, easy to debug
- Concurrency tests prove atomicity
- Integration tests validate API contracts

---

## Unit Tests

**Location:** `app/tests/test_transfer_service.py`

### Coverage

**Happy Path:**
- Successful transfer
- Account creation with valid balance
- Account retrieval

**Error Cases:**
- Insufficient funds (balance < amount)
- Account not found (invalid ID)
- Invalid amounts (negative, zero, >2 decimals)
- Transfer to self (same from/to)

**Boundary Values:**
- Exact balance transfer (balance = amount)
- Minimum amount (0.01)
- Very large amounts (overflow check)
- Fractional cents (10.123 rejected)

### Edge Cases

- Transfer with identical from/to account
- Concurrent read during write
- Multiple transfers draining same account
- Decimal precision edge cases

---

## Concurrency Tests

**Location:** `app/tests/test_concurrency.py`

These tests **prove thread-safety**. Without them, no guarantee system works under load.

### Test 1: Race Condition Prevention

**Setup:**
- Account with $1000
- 20 threads, each tries to transfer $100
- All start simultaneously (using Barrier)

**Expected:**
- Exactly 10 succeed (balance becomes $0)
- Exactly 10 fail (InsufficientFundsException)
- Final balance = $0

**Validation:**
- Remove locks → balance goes negative ❌
- Restore locks → test passes ✅

**Code:** `app/tests/test_concurrency.py::test_concurrent_transfers_from_same_account`

---

### Test 2: Deadlock Prevention

**Setup:**
- Accounts A and B
- Thread 1: Transfer A→B 100 times
- Thread 2: Transfer B→A 100 times
- Timeout: 5 seconds

**Expected:**
- Both threads complete successfully
- No hanging

**Validation:**
- Remove lock ordering → test hangs ❌
- Restore lock ordering → test passes ✅

**Code:** `app/tests/test_concurrency.py::test_deadlock_prevention`

---

### Test 3: Parallel Execution

**Setup:**
- Transfer A→B and C→D simultaneously
- Measure timing overlap

**Expected:**
- Execution times overlap (proves parallelism)
- Not sequential

**Code:** `app/tests/test_concurrency.py::test_parallel_execution`

---

### Test 4: Money Conservation

**Setup:**
- 5 accounts, $1000 each (total $5000)
- 100 random transfers between accounts
- Concurrent execution

**Expected:**
- Total balance = $5000 (unchanged)
- No money created or destroyed

**Code:** `app/tests/test_concurrency.py::test_money_conservation`

---

### Concurrency Edge Cases

- 1000+ concurrent transfers (stress test)
- Transfer to self under concurrency
- Rapid bidirectional transfers (A↔B spam)
- Thread pool exhaustion scenarios

---

## Integration Tests

**Location:** `app/tests/test_api.py`

### API Contract Tests

**Endpoints:**
- POST /accounts → 201
- GET /accounts/{id} → 200 / 404
- POST /transactions → 201 / 400 / 404

**Validation:**
- HTTP status codes
- Response body schema
- Error message format

### Edge Cases

- Malformed JSON
- Missing required fields
- Invalid Decimal format ("abc", "10.123")
- Negative account IDs
- Non-existent routes (404)
- Large payload sizes
- Concurrent API requests

---

## E2E Testing with Playwright

**Location:** `app/tests/e2e.spec.ts`

**Why Playwright:**
- Fast, reliable
- Parallel execution across browsers
- Built-in API testing (no browser overhead)
- Auto-wait for responses

### Test Coverage

**24 tests across 3 browsers** (Chromium, Firefox, WebKit)

#### Happy Path Tests
- Health check endpoint
- Create account with valid balance
- Get account by ID
- Successful fund transfer
- Complete user journey (multi-step)

#### Error Handling Tests
- Get non-existent account (404)
- Transfer with insufficient funds (400)
- Transfer to non-existent account (404)

#### Money Conservation Test
**Scenario:**
- Alice starts with $1000
- Alice → Bob ($200)
- Bob → Charlie ($300)
- Alice → Charlie ($100)

**Expected:**
- Alice = $700
- Bob = $400
- Charlie = $400
- **Total = $1500** (unchanged)

**Code:** `app/tests/e2e.spec.ts:167-225`

### Testing Patterns Used

**1. Setup Pattern**
```typescript
test.beforeEach(async ({ page }) => {
  await page.goto('/');
});
```

**2. API Request Pattern**
```typescript
const response = await request.post('/accounts', {
  data: { initial_balance: 1000.00 }
});
expect(response.status()).toBe(201);
```

**3. Multi-Step Workflow**
```typescript
// Step 1: Create accounts
const alice = await createAccount(1000);
const bob = await createAccount(500);

// Step 2: Perform transfer
await transfer(alice.id, bob.id, 200);

// Step 3: Verify state
const finalAlice = await getAccount(alice.id);
expect(finalAlice.balance).toBe('800.00');
```

**Test Execution:**
```bash
npx playwright test               # All tests
npx playwright test --reporter=list
npx playwright test --ui          # Interactive mode
npx playwright show-report        # View results
```

**Execution Time:** ~12 seconds for 24 tests across 3 browsers

---

## Critical Tests (Must Pass)

These tests **prove core guarantees:**

### 1. `test_concurrent_transfers_from_same_account`
**Proves:** No race condition on balance check

### 2. `test_deadlock_prevention`
**Proves:** Lock ordering prevents deadlocks

### 3. `test_money_conservation`
**Proves:** No money created/destroyed under concurrency

**Validation Method:**
1. Temporarily break implementation (e.g., remove locks)
2. Verify test fails ✅
3. Restore implementation
4. Verify test passes ✅

---

## Test Execution

```bash
# All tests
pytest

# Specific file
pytest app/tests/test_concurrency.py

# Verbose output
pytest -v

# Coverage report
pytest --cov=app

# E2E tests
npx playwright test
```

**Expected:** 40+ tests, all pass, <5 seconds (excluding E2E)

---

## Coverage Goals

- **Line Coverage:** >90%
- **Branch Coverage:** >85%
- **Concurrency Scenarios:** 100% (all race conditions tested)
- **Error Cases:** 100% (all exception paths tested)

---


