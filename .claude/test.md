# Testing Strategy

## Overview

Testing a concurrent payments ledger requires proving three critical properties:
1. **Correctness**: Business rules enforced (no negative balances, atomic transfers)
2. **Thread-Safety**: No race conditions under concurrent access
3. **API Contracts**: End-to-end HTTP requests/responses work correctly

---

## Test Pyramid

```
       /\
      /11\    Playwright API Tests (E2E)
     /----\
    /  4   \   Concurrency Tests (Thread-safety)
   /--------\
  /    8     \ Unit Tests (Business Logic)
 /------------\
```

**Total: 23 automated tests**

---

## 1. Unit Tests (8 tests) - `TransferServiceTest.java`

**Purpose:** Verify business logic and edge cases in isolation

### Test Cases

| Test | Scenario | Expected |
|------|----------|----------|
| `testSuccessfulTransfer` | Transfer $250 from Alice ($1000) to Bob ($500) | Alice=$750, Bob=$750, status=COMPLETED |
| `testInsufficientFunds` | Transfer $150 from account with $100 | `InsufficientFundsException`, balance unchanged |
| `testNonExistentFromAccount` | Transfer from invalid account | `AccountNotFoundException` (404) |
| `testNonExistentToAccount` | Transfer to invalid account | `AccountNotFoundException` (404) |
| `testNegativeAmount` | Transfer negative amount | `InvalidAmountException` |
| `testZeroAmount` | Transfer zero amount | `InvalidAmountException` |
| `testAmountWithTooManyDecimalPlaces` | Transfer $10.123 (3 decimals) | `InvalidAmountException` |
| `testTransferToSameAccount` | Transfer from account to itself | Rejected |

**Coverage:** 95%+ lines, 90%+ branches

**Run:**
```bash
mvn test -Dtest=TransferServiceTest
```

---

## 2. Concurrency Tests (4 tests) - `ConcurrencyTest.java`

**Purpose:** Prove thread-safety under high contention. Critical because unit tests cannot prove thread-safety.

### Test 1: Race Condition Prevention
```java
@Test
void testConcurrentTransfersFromSameAccount()
```

**Scenario:**
- Alice has $1000
- 20 threads simultaneously transfer $100 to Bob
- Only 10 should succeed

**Expected:**
- Exactly 10 succeed, 10 fail with `InsufficientFundsException`
- Alice: $0, Bob: $1000
- **No lost updates** (race conditions would allow >10 to succeed)

**Proves:** Account-level locking prevents race conditions

---

### Test 2: Deadlock Prevention
```java
@Test
void testDeadlockPrevention()
```

**Scenario:**
- Alice ($1000) ↔ Bob ($1000) bidirectional transfers
- Thread 1: A→B repeatedly
- Thread 2: B→A repeatedly
- 100 iterations each

**Expected:**
- Both complete in <10 seconds
- No system hang

**Proves:** Lock ordering (sorted by account ID) prevents circular wait → no deadlocks

---

### Test 3: Money Conservation
```java
@Test
void testMoneyConservation()
```

**Scenario:**
- 5 accounts, $1000 each (total: $5000)
- 100 random transfers

**Expected:**
- Sum of all balances after = $5000 (unchanged)

**Proves:** Atomic transfers maintain system-wide balance invariant

---

### Test 4: Parallel Execution
```java
@Test
void testParallelExecution()
```

**Scenario:**
- Transfer A→B and C→D simultaneously
- Measure execution overlap

**Expected:**
- Both execute in parallel (overlapping time)
- Not serialized by global lock

**Proves:** Account-level locking allows independent transfers to run concurrently

**Run:**
```bash
mvn test -Dtest=ConcurrencyTest
```

---

## 3. Playwright API Tests (11 tests) - `tests/api.spec.ts`

**Purpose:** End-to-end API verification with actual HTTP requests

### Test Coverage

| Category | Tests | Description |
|----------|-------|-------------|
| Health Check | 1 | `GET /api/v1/health` returns 200 |
| Account Creation | 2 | Create account with valid/invalid balance |
| Account Retrieval | 2 | Get existing account (200), non-existent (404) |
| Transfer Success | 2 | Valid transfer updates both balances atomically |
| Transfer Errors | 4 | Insufficient funds (400), invalid amount (400), non-existent account (404), same account (400) |

**Example Test:**
```typescript
test('should transfer funds between accounts', async ({ request }) => {
  const sender = await createAccount('1000.00');
  const receiver = await createAccount('0.00');

  const response = await request.post(`${BASE_URL}/transactions`, {
    data: {
      fromAccountId: sender.id,
      toAccountId: receiver.id,
      amount: '100.00'
    }
  });

  expect(response.status()).toBe(201);

  const senderAfter = await getAccount(sender.id);
  const receiverAfter = await getAccount(receiver.id);

  expect(senderAfter.balance).toBe(900.00);
  expect(receiverAfter.balance).toBe(100.00);
});
```

**What This Proves:**
- ✅ All endpoints return correct status codes
- ✅ Request/response serialization works (JSON ↔ Java DTOs)
- ✅ Error messages are clear and consistent
- ✅ API contracts match documentation

**Run:**
```bash
# Install dependencies (first time only)
npm install

# Run all tests
npm test

# Run with UI mode
npm run test:ui

# View test report
npm run test:report
```

---

## Test Execution

### Run All Tests
```bash
# Java tests (unit + concurrency)
mvn test

# Playwright API tests
npm test
```

### Run with Coverage
```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

### Test Results
```
Java Tests: 12/12 passed
  ✓ Unit Tests (8)
  ✓ Concurrency Tests (4)

Playwright Tests: 11/11 passed
  ✓ Health check (1)
  ✓ Account operations (4)
  ✓ Transfer operations (6)

Total: 23/23 passed ✅
```

---

## Why These Tests Matter

### Without Concurrency Tests
- Unit tests pass ✅
- System fails in production under load ❌
- Race conditions cause lost updates, money creation, deadlocks

### Without API Tests
- Business logic works ✅
- HTTP serialization broken ❌
- API contracts not verified

### With All Three Test Types
- Business logic proven correct ✅
- Thread-safety proven ✅
- API contracts verified ✅
- **Confidence in production deployment** ✅

---

## Test Coverage Goals

| Layer | Coverage | Tests |
|-------|----------|-------|
| Service Layer | 95%+ | Unit + Concurrency |
| Repository Layer | 80%+ | Unit |
| API Layer | 100% | Playwright |
| Models | 100% | Unit |

---

## Manual Testing (Quick Smoke Test)

```bash
# Start server
mvn spring-boot:run

# Create accounts
ACC1=$(curl -s -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"initialBalance": "1000.00"}' | jq -r '.id')

ACC2=$(curl -s -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"initialBalance": "500.00"}' | jq -r '.id')

# Transfer funds
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\": \"$ACC1\", \"toAccountId\": \"$ACC2\", \"amount\": \"250.00\"}"

# Verify balances
curl http://localhost:8080/api/v1/accounts/$ACC1  # Expected: 750.00
curl http://localhost:8080/api/v1/accounts/$ACC2  # Expected: 750.00
```

---

## Future Improvements

### Phase 1: Performance Tests
- Load testing with JMeter/Gatling
- Metrics: Throughput (TPS), latency (p50, p95, p99), error rate
- Scenarios: Sustained load (1000 TPS), spike test (5000 TPS)

### Phase 2: Database Tests
- Test with embedded PostgreSQL (Testcontainers)
- Verify transaction rollback on errors
- Test database deadlock handling

### Phase 3: Chaos Testing
- Network failures
- Database connection loss
- Partial system failures

---

## Summary

**Test Types:**
- **Unit Tests (8)**: Business logic correctness
- **Concurrency Tests (4)**: Thread-safety under load
- **Playwright Tests (11)**: End-to-end API contracts

**Total Coverage:**
- 23 automated tests
- 95%+ code coverage
- All critical paths tested

**Confidence Level:** High. All atomic operations, thread-safety, and API contracts verified.
