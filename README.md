# Simple Payments Ledger API

An in-memory transactional ledger API for managing accounts and fund transfers with strict atomicity guarantees. Built for ~100K users with Java 11, Spring Boot, and Maven.

---

## 1. Quick Start

### 1.1 Prerequisites
- Java 11+
- Maven 3.6+
- Docker (optional)

### 1.2 Build and Run

#### 1.2.1 Using Maven
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

#### 1.2.2 Using Docker
```bash
# Build and run with Docker Compose
docker compose up --build

# Or manually
docker build -t payments-ledger .
docker run -p 8080:8080 payments-ledger
```

### 1.3 Running Tests

#### 1.3.1 Java Tests
```bash
# All tests
mvn test

# Specific test suite
mvn test -Dtest=TransferServiceTest
mvn test -Dtest=ConcurrencyTest
```

#### 1.3.2 Playwright API Tests
```bash
# Install dependencies (first time only)
npm install

# Run all API tests
npm test

# Run with UI mode
npm run test:ui

# View test report
npm run test:report
```

---

## 2. API Documentation

### 2.1 Interactive Documentation (Swagger)
Once the server is running, visit **Swagger UI** for interactive API documentation:
```
http://localhost:8080/swagger-ui/index.html
```

Features:
- Browse all endpoints with detailed descriptions
- Test APIs directly in your browser with "Try it out"
- View request/response schemas and examples

### 2.2 Endpoints Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/health` | Health check |
| `POST` | `/api/v1/accounts` | Create account with initial balance |
| `GET` | `/api/v1/accounts/{id}` | Get account details |
| `POST` | `/api/v1/transactions` | Transfer funds between accounts |
| `GET` | `/api/v1/transactions/{id}` | Get transaction details |

---

## 3. Architecture & Design

### 3.1 Core Design Principles

#### 3.1.1 Fine-Grained Locking (Account-Level, Not Global)

**The Problem:**
A global lock would serialize all transfers, creating a throughput bottleneck for 100K users.

**The Solution:**
Each account has its own `ReentrantLock`. Transfers only block if they share an account.

**Deadlock Prevention:**
Locks are always acquired in sorted order (by account ID), preventing circular wait conditions.

```java
// Simplified example
String firstId = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
String secondId = fromAccountId.equals(firstId) ? toAccountId : fromAccountId;

firstAccount.getLock().lock();
try {
    secondAccount.getLock().lock();
    try {
        // Transfer logic here
    } finally {
        secondAccount.getLock().unlock();
    }
} finally {
    firstAccount.getLock().unlock();
}
```

**Impact:**
- Independent transfers (A→B and C→D) execute in parallel
- Transfers on same account are serialized (necessary for correctness)
- See: `TransferService.java:65-115`

#### 3.1.2 Repository Pattern (Database-Ready Architecture)

**Current State:**
In-memory storage using `ConcurrentHashMap`.

**Migration Path:**
Business logic is decoupled from storage via repository interfaces:
- `AccountRepository` interface
- `TransactionRepository` interface

**To migrate to PostgreSQL/MySQL:**
1. Create `JpaAccountRepository implements AccountRepository`
2. Wire via Spring dependency injection
3. **Zero changes to business logic or API layer**

All storage operations are isolated in repository implementations.

### 3.2 Project Structure

```
java-app/
├── src/
│   ├── main/java/com/payments/ledger/
│   │   ├── models/          # Domain entities
│   │   ├── repositories/    # Data access layer
│   │   ├── services/        # Business logic
│   │   ├── api/             # REST controllers + DTOs
│   │   └── exceptions/      # Custom exceptions
│   └── test/java/           # Unit & concurrency tests
├── tests/                   # Playwright API tests
├── .claude/
│   ├── prompt.md            # AI prompts and insights
│   ├── design.md            # Design decisions and trade-offs
│   └── test.md              # Testing strategy
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## 4. Testing & Verification

### 4.1 Test Suite Overview

**Test pyramid:**
```
       /\
      /11\    Integration tests (API contracts)
     /----\
    /  4   \   Concurrency tests (thread-safety)
   /--------\
  /    8     \ Unit tests (business logic)
 /------------\
```

### 4.2 Unit Tests (8 tests)
Tests all business rules and edge cases:
- ✅ Successful transfer updates both balances atomically
- ✅ Insufficient funds returns 400 with clear error message
- ✅ Non-existent accounts return 404
- ✅ Invalid amounts (negative, zero, >2 decimals) rejected
- ✅ Transfer to same account rejected

```bash
mvn test -Dtest=TransferServiceTest
```

### 4.3 Concurrency Tests (4 tests)
Proves thread-safety under high contention:

**Race Condition Prevention:**
- 20 threads simultaneously transfer from same account
- Result: Exactly 10 succeed, 10 fail ✅ No lost updates

**Deadlock Prevention:**
- Bidirectional transfers A↔B, 100 iterations
- Result: Completes in <10s ✅ No deadlocks

**Money Conservation:**
- 100 random transfers across 5 accounts
- Result: Sum(balances) unchanged ✅ Atomic transfers

**Parallel Execution:**
- Transfer A→B and C→D simultaneously
- Result: Overlapping execution ✅ Not serialized

```bash
mvn test -Dtest=ConcurrencyTest
```

### 4.4 API Integration Tests (11 Playwright tests)
End-to-end API verification:
- ✅ All endpoints return correct status codes
- ✅ Request/response serialization works
- ✅ Error messages are clear

```bash
npm test
```

### 4.5 Manual Testing
```bash
# Verified with cURL and Docker deployment
docker compose up --build
curl http://localhost:8080/api/v1/health
```

**Confidence Level:** All critical paths tested. Atomicity and thread-safety proven.

---

## 5. Development Guide

### 5.1 Refactoring Workflow

The codebase is structured to allow safe evolution. Follow this workflow when adding new features or refactoring:

#### Step 1: Establish Baseline
Run all tests to ensure everything passes before making changes:
```bash
mvn test && npm test
```
All tests should be green. This is your safety net.

#### Step 2: Make Changes Following Layered Architecture
```
API Layer (Controllers) → Service Layer (Business Logic) → Repository Layer (Data Access)
```

**Golden Rule:** Business logic ONLY lives in the Service layer.

**Example: Adding Transaction Fees**

1. **Update DTO** (API Layer):
```java
// CreateTransferRequest.java
public class CreateTransferRequest {
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private BigDecimal fee;  // NEW: Add optional fee field (default 0)
}
```

2. **Update Service** (Business Logic Layer):
```java
// TransferService.java
public Transaction transfer(String fromId, String toId, BigDecimal amount, BigDecimal fee) {
    // Validate fee >= 0
    BigDecimal totalDeduction = amount.add(fee);  // NEW: amount + fee

    // Check if sender has enough for amount + fee
    if (fromAccount.getBalance().compareTo(totalDeduction) < 0) {
        throw new InsufficientFundsException();
    }

    // Deduct amount + fee from sender
    fromAccount.setBalance(fromAccount.getBalance().subtract(totalDeduction));

    // Credit only amount to receiver (fee is collected)
    toAccount.setBalance(toAccount.getBalance().add(amount));
}
```

3. **Repository Layer**: No changes needed (data structure unchanged).

4. **API Controller**: No changes needed (Spring auto-maps new field).

#### Step 3: Write Tests for New Functionality

**3a. Unit Tests** (Business logic verification):
```java
// TransferServiceTest.java
@Test
void testTransferWithFee() {
    Account sender = accountRepository.create(new BigDecimal("1000.00"));
    Account receiver = accountRepository.create(new BigDecimal("0.00"));

    // Transfer 100 with 5 fee
    transferService.transfer(sender.getId(), receiver.getId(),
                            new BigDecimal("100.00"),
                            new BigDecimal("5.00"));

    // Sender should have 1000 - 100 - 5 = 895
    assertEquals(new BigDecimal("895.00"), sender.getBalance());

    // Receiver should have 100 (fee not transferred)
    assertEquals(new BigDecimal("100.00"), receiver.getBalance());
}

@Test
void testInsufficientFundsWithFee() {
    Account sender = accountRepository.create(new BigDecimal("100.00"));
    Account receiver = accountRepository.create(new BigDecimal("0.00"));

    // Try to transfer 98 + 5 fee = 103 total (should fail)
    assertThrows(InsufficientFundsException.class, () -> {
        transferService.transfer(sender.getId(), receiver.getId(),
                                new BigDecimal("98.00"),
                                new BigDecimal("5.00"));
    });
}
```

**3b. Concurrency Tests** (Thread-safety verification):
```java
// ConcurrencyTest.java
@Test
void testConcurrentTransfersWithFees() {
    Account sender = accountRepository.create(new BigDecimal("1000.00"));
    Account receiver = accountRepository.create(new BigDecimal("0.00"));

    // 10 threads try to transfer 100 + 2 fee = 102 each
    // Only 9 should succeed (9 * 102 = 918, leaving 82)

    ExecutorService executor = Executors.newFixedThreadPool(10);
    AtomicInteger successCount = new AtomicInteger(0);

    for (int i = 0; i < 10; i++) {
        executor.submit(() -> {
            try {
                transferService.transfer(sender.getId(), receiver.getId(),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("2.00"));
                successCount.incrementAndGet();
            } catch (InsufficientFundsException e) {
                // Expected when balance runs out
            }
        });
    }

    // Verify no race conditions or lost updates
    assertEquals(9, successCount.get());
}
```

**3c. Integration Tests** (API contract verification):
```typescript
// tests/api.spec.ts
test('transfer with fee deducts from sender', async ({ request }) => {
  const sender = await createAccount('1000.00');
  const receiver = await createAccount('0.00');

  const response = await request.post(`${BASE_URL}/transactions`, {
    data: {
      fromAccountId: sender.id,
      toAccountId: receiver.id,
      amount: '100.00',
      fee: '5.00'  // NEW: Optional fee field
    }
  });

  expect(response.status()).toBe(201);

  const senderAfter = await getAccount(sender.id);
  const receiverAfter = await getAccount(receiver.id);

  expect(senderAfter.balance).toBe(895.00);  // 1000 - 100 - 5
  expect(receiverAfter.balance).toBe(100.00);
});
```

#### Step 4: Run Test Suite
```bash
# Run unit + concurrency tests
mvn test

# Run integration tests
npm test
```

**Expected Result:**
- **All NEW tests pass** → New feature works correctly
- **All OLD tests pass** → Existing functionality unchanged

**If old tests fail:**
- You broke existing behavior → fix your changes
- Update test expectations ONLY if behavior change is intentional

#### Step 5: Verify Test Coverage
```bash
# Check which lines are covered by tests
mvn test jacoco:report
open target/site/jacoco/index.html
```

Aim for:
- Service layer: >90% coverage
- Critical paths (transfers, validation): 100% coverage

### 5.2 Architecture Patterns

#### 5.2.1 Dependency Injection
Services receive dependencies via constructor:
```java
public TransferService(AccountRepository accountRepo, TransactionRepository txnRepo) {
    this.accountRepository = accountRepo;
    this.transactionRepository = txnRepo;
}
```

**Why this helps:**
- Tests mock repositories → no side effects
- Easy to swap implementations (in-memory → PostgreSQL)
- No tight coupling to concrete classes

#### 5.2.2 Repository Pattern
All data access through interfaces:
```java
public interface AccountRepository {
    Optional<Account> findById(String id);
    Account create(BigDecimal initialBalance);
    void update(Account account);
}
```

**Why this helps:**
- Service layer doesn't know about storage details
- Migrating to MySQL? Just implement `JpaAccountRepository`
- **Zero service layer changes** when switching databases

### 5.3 Safety Checklist

Before committing changes:
- [ ] All existing tests pass (`mvn test && npm test`)
- [ ] New tests added for new functionality
- [ ] No business logic leaked into API or Repository layers
- [ ] Concurrency tests pass (if touching shared state)
- [ ] API contracts unchanged (or versioned if breaking)
- [ ] Code reviewed for thread-safety (locks, atomicity)

### 5.4 Common Refactoring Scenarios

#### Scenario 1: Adding a New Field
**Example:** Add `description` field to transactions
- Update `Transaction` model
- Update `CreateTransferRequest` DTO
- **No service logic changes needed** (field is just stored)
- Old tests pass (backward compatible)

#### Scenario 2: Changing Business Logic
**Example:** Prevent transfers below $10
- Add validation in `TransferService.transfer()`
- Write unit test for new validation
- **Old tests will fail if they transfer <$10** → update test data

#### Scenario 3: Migrating Storage
**Example:** Switch from in-memory to PostgreSQL
- Create `JpaAccountRepository implements AccountRepository`
- Update Spring configuration
- **Zero service/API changes**
- Run same test suite to verify correctness

---

## 6. Additional Resources

### AI Prompt Documentation
For detailed information, see `.claude/` directory:
- **prompt.md** - AI prompts used and refinement process
- **design.md** - Design decisions and trade-offs
- **test.md** - Comprehensive testing strategy
