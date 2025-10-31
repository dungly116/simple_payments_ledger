# Testing Strategy

## Philosophy

**Tests must fail on broken code**

Verification: Write test → passes → break code → test fails → restore

## Test Pyramid

```
Integration (10%) - API endpoints
Unit (60%) - Business logic
Concurrency (30%) - Thread-safety
```

---

## Unit Tests

**Location:** `app/tests/test_transfer_service.py`

**Coverage:**
- Happy path (successful transfer)
- Insufficient funds
- Account not found
- Invalid amounts (negative, zero, >2 decimals)
- Exact balance transfer
- Transfer to self
- Boundary values (Decimal.MAX, 0.01)

**Edge Cases:**
- Transfer with same from/to account
- Concurrent read during write
- Multiple transfers draining same account
- Very large amounts (overflow check)
- Fractional cent amounts (>2 decimals)

---

## Concurrency Tests

**Location:** `app/tests/test_concurrency.py`

**Must Cover:**

### Test 1: Race Condition Prevention
- 20 threads, $1000 account, $100 each
- Use Barrier for simultaneous start
- Expected: Exactly 10 succeed, 10 fail, balance = $0
- Verification: Remove locks → balance goes negative

### Test 2: Deadlock Prevention
- Thread 1: A→B 100x
- Thread 2: B→A 100x
- Timeout: 5s
- Expected: Both complete
- Verification: Remove lock ordering → test hangs

### Test 3: Parallel Execution
- Transfer A→B and C→D simultaneously
- Measure timing overlap
- Expected: Overlap exists (proves parallelism)

### Test 4: Money Conservation
- 5 accounts, $1000 each
- 100 random transfers
- Expected: Sum = $5000

**Edge Cases:**
- 1000+ concurrent transfers
- Transfer to self under concurrency
- Rapid bidirectional transfers (A↔B spam)
- Lock timeout scenarios
- Thread pool exhaustion

---

## Integration Tests

**Location:** `app/tests/test_api.py`

**Coverage:**
- POST /accounts → 201
- GET /accounts/{id} → 200/404
- POST /transactions → 201/400/404
- Error response format
- Request validation

**Edge Cases:**
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

**Use Playwright for:**
- Full user journey
- API interaction testing
- Multi-step workflows

**Why Playwright:**
- Fast, reliable
- Parallel execution
- Auto-wait for responses
- Built-in API testing capabilities

### Testing Patterns Used

**1. Setup Pattern**
```typescript
test.beforeEach(async ({ page }) => {
  await page.goto('/');
});
```
- Runs before each test
- Ensures clean state
- Navigates to base URL

**2. API Request Testing**
```typescript
const response = await request.get('/health');
expect(response.ok()).toBeTruthy();
const data = await response.json();
```
- Uses `request` fixture for HTTP calls
- Tests API without browser overhead
- Validates response status and body

**3. Test Data Setup**
```typescript
const account = await request.post('/accounts', {
  headers: {
    'X-API-Key': API_KEY,
    'Content-Type': 'application/json',
  },
  data: { initial_balance: 1000.00 }
}).then(res => res.json());
```
- Creates test data programmatically
- Returns data for use in test
- Chains `.then()` for cleaner syntax

**4. Authentication Pattern**
```typescript
headers: {
  'X-API-Key': API_KEY,
  'Content-Type': 'application/json',
}
```
- Uses constant for API key
- Applied to all protected endpoints
- Centralized configuration

**5. Assertion Patterns**
```typescript
expect(response.status()).toBe(201);
expect(account).toHaveProperty('id');
expect(account.balance).toBe('1000.50');
expect(error.detail).toContain('insufficient');
```
- Status code validation
- Property existence checks
- Exact value matching
- Partial string matching

**6. Multi-Step Test Pattern**
```typescript
// Step 1: Create accounts
const alice = await request.post('/accounts', {...});
const bob = await request.post('/accounts', {...});

// Step 2: Perform actions
await request.post('/transactions', {...});

// Step 3: Verify state
const finalAlice = await request.get(`/accounts/${alice.id}`, {...});
expect(finalAlice.balance).toBe('700.00');
```
- Clear separation of setup, action, assertion
- Named variables for clarity (alice, bob, charlie)
- End-to-end workflow validation

**7. Money Conservation Pattern**
```typescript
const totalBalance = parseFloat(finalAlice.balance) +
                    parseFloat(finalBob.balance) +
                    parseFloat(finalCharlie.balance);
expect(totalBalance).toBe(1500.00);
```
- Verifies invariants across operations
- Aggregates data from multiple sources
- Proves system integrity

**Coverage:**

### Happy Path Tests
- Health check endpoint (tests/e2e.spec.ts:10)
- Create account with valid balance (tests/e2e.spec.ts:19)
- Get account by ID (tests/e2e.spec.ts:38)
- Successful fund transfer between accounts (tests/e2e.spec.ts:74)
- Complete user journey (tests/e2e.spec.ts:167)

### Error Handling Tests
- Get non-existent account (404) (tests/e2e.spec.ts:62)
- Transfer with insufficient funds (400) (tests/e2e.spec.ts:116)
- Transfer to non-existent account (404) (tests/e2e.spec.ts:145)

### Money Conservation Test
- Multi-step transfers across 3 accounts (tests/e2e.spec.ts:167)
- Verify total balance remains constant (tests/e2e.spec.ts:225)
- Alice ($1000) → Bob ($200) → Bob sends to Charlie ($300) → Alice sends to Charlie ($100)
- Final: Alice=$700, Bob=$400, Charlie=$400, Total=$1500

**Test Organization:**
- Grouped in `test.describe()` block
- Clear test naming convention
- Isolated test data per test
- No shared state between tests

**Test Results:**
- 24 tests across 3 browsers (Chromium, Firefox, WebKit)
- All tests passing
- Execution time: ~12s

**Test Execution:**
```bash
# Run all Playwright tests
npx playwright test

# Run with list reporter
npx playwright test --reporter=list

# Run with UI
npx playwright test --ui

# Run specific test file
npx playwright test tests/e2e.spec.ts

# Show report
npx playwright show-report
```

**Note:** Tests run in `APP_ENV=test` mode which skips database migrations and API key validation for faster execution.

---

## Test Execution

```bash
# All tests
pytest

# Specific file
pytest app/tests/test_concurrency.py

# Verbose
pytest -v

# Coverage
pytest --cov=app
```

**Expected:** 40 tests, all pass, <5s

---

## Critical Tests

**These prove thread-safety:**

1. `test_concurrent_transfers_from_same_account`
   - Proves: No race condition on balance check

2. `test_deadlock_prevention`
   - Proves: Lock ordering works

3. `test_money_conservation`
   - Proves: No money created/destroyed

**Validation method:**
- Temporarily break code
- Verify test fails
- Restore code
- Test passes

---

## Coverage Goals

- Line: >90%
- Branch: >85%
- Concurrency scenarios: 100%
- Error cases: 100%
