# Design Decisions

## Decision 1: Account-Level Locks vs Global Lock

### Options

**A. Global Lock (Rejected)**
```python
self._global_lock = Lock()
with self._global_lock:
    # All transfers sequential
```
- Pro: Simple, no deadlock
- Con: Serializes everything, poor throughput

**B. Account-Level Locks (Chosen)**
```python
first.get_lock().acquire()
second.get_lock().acquire()
# Parallel transfers on different accounts
```
- Pro: Parallel execution, high throughput
- Con: Deadlock risk, more complex

### Comparison

| Aspect | Global Lock | Account-Level |
|--------|-------------|---------------|
| Simplicity | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| Throughput | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| Deadlock Risk | None | Needs prevention |
| Scalability | Poor | Good |

### Decision

**Chosen: Account-Level Locks**

**Reason:** 100K users, low contention per account → parallelism benefit > complexity cost

**When to reconsider:**
- <10 concurrent requests → use global lock
- 10M+ users, high contention → optimistic locking + DB

---

## Decision 2: Lock Ordering for Deadlock Prevention

### Problem

```
Thread 1: Lock A → Wait for B
Thread 2: Lock B → Wait for A
Result: DEADLOCK
```

### Solution

**Always acquire locks in sorted order by account ID**

```python
first = from_acc if from_acc.id < to_acc.id else to_acc
second = to_acc if from_acc.id < to_acc.id else from_acc

first.get_lock().acquire()
second.get_lock().acquire()
# Critical section
second.get_lock().release()
first.get_lock().release()
```

### Why This Works

**Without ordering:**
```
Thread 1: [A] --waits--> [B]
             ↑___________|
Thread 2: [B] --waits--> [A]

Circular wait = Deadlock
```

**With ordering:**
```
Thread 1: [A] → [B] → execute → release
Thread 2: waits for A → [A] → [B] → execute → release

Linear wait = No deadlock
```

### Alternatives Rejected

**A. Timeout + Retry**
- Doesn't prevent, only detects
- Non-deterministic

**B. Try-Lock**
- Live-lock risk
- CPU waste

**C. Lock Ordering ✅**
- Deterministic prevention
- No retry overhead

---

## Implementation

**File:** `app/services/transfer_service.py:75-105`

**Methods:**
- `_execute_transfer()` - Lock ordering
- `transfer()` - Entry point

**Test:** `app/tests/test_concurrency.py::test_deadlock_prevention`
- Thread1: A↔B 100x
- Thread2: B↔A 100x
- Timeout: 5s
- Result: Completes without hanging

**Verification:** Remove lock ordering → test hangs

---

## Tradeoffs

**Gained:**
- High concurrency throughput
- Parallel execution
- No deadlocks

**Cost:**
- Increased complexity
- Lock ordering required
- Harder debugging

**Worth it?** Yes for 100K+ users

---

## Scalability Path

**Phase 1 (100K):** Current design ✅

**Phase 2 (1M):**
- Add DB persistence
- Keep in-memory cache
- Same locking

**Phase 3 (10M+):**
- Optimistic locking
- Event sourcing
- DB sharding

**Phase 4 (Global):**
- Distributed consensus
- Multi-region
- Event streaming
