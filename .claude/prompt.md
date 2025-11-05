# AI Prompts and Refinement Process

This document outlines the key prompts used with Claude Code (AI assistant) and the refinements made during development.

## Initial Project Setup

### Prompt 1: Project Structure
```
Create a simple payments ledger API in Java with Spring Boot:
- POST /accounts - create account with initial balance
- GET /accounts/{id} - get account details
- POST /transactions - transfer funds between accounts
- In-memory storage using ConcurrentHashMap
- Atomic transfers with proper concurrency handling
```

**AI's Initial Output:**
- Generated basic Spring Boot project structure
- Created REST endpoints with correct paths
- Used `HashMap` instead of `ConcurrentHashMap` initially

**Refinement Required:**
Changed `HashMap` to `ConcurrentHashMap` for thread-safety in repository layer. While the locking strategy handles transfer atomicity, repository-level operations still needed thread-safe collections.

---

## Concurrency Implementation

### Prompt 2: Thread-Safe Transfers
```
Implement atomic fund transfers with:
1. Account-level locking (not global lock)
2. Deadlock prevention
3. Atomicity guarantees (debit and credit together)
4. Insufficient funds validation
```

**AI's First Draft:**
Generated transfer logic but used a **global `synchronized` block**:
```java
public synchronized Transaction transfer(...) {
    // All transfers serialized
}
```

**Problem Identified:**
This serializes ALL transfers, even independent ones (A→B and C→D would block each other).

**Refinement Applied:**
Replaced with account-level locking:
```java
Account fromAccount = accountRepository.findById(fromAccountId)
    .orElseThrow(() -> new AccountNotFoundException(fromAccountId));
Account toAccount = accountRepository.findById(toAccountId)
    .orElseThrow(() -> new AccountNotFoundException(toAccountId));

String firstId = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
String secondId = fromAccountId.equals(firstId) ? toAccountId : fromAccountId;
Account firstAccount = firstId.equals(fromAccountId) ? fromAccount : toAccount;
Account secondAccount = firstId.equals(fromAccountId) ? toAccount : fromAccount;

firstAccount.getLock().lock();
try {
    secondAccount.getLock().lock();
    try {
        // Transfer logic
    } finally {
        secondAccount.getLock().unlock();
    }
} finally {
    firstAccount.getLock().unlock();
}
```

**Why This Matters:**
- Independent transfers run in parallel
- Deadlock prevented by consistent lock ordering
- Performance scales with number of accounts

---

## Test Coverage

### Prompt 3: Concurrency Tests
```
Write comprehensive concurrency tests:
1. Race condition test - 20 threads transferring from same account
2. Deadlock prevention test - bidirectional transfers A↔B
3. Money conservation test - verify total balance unchanged
4. Parallel execution test - prove independent transfers don't block
```

**AI's First Draft:**
Generated tests but **didn't declare checked exceptions**:
```java
@Test
void testDeadlockPrevention() throws InterruptedException, ExecutionException {
    // ...
    future1.get(10, TimeUnit.SECONDS);  // TimeoutException not declared
}
```

**Compilation Error:**
```
unreported exception java.util.concurrent.TimeoutException;
must be caught or declared to be thrown
```

**Refinement Applied:**
Added `TimeoutException` to method signature:
```java
@Test
void testDeadlockPrevention() throws InterruptedException, ExecutionException, TimeoutException {
    // ...
}
```

**Lesson Learned:**
AI sometimes misses checked exception declarations. Always compile and run tests to catch these issues.

---

## Docker Deployment

### Prompt 4: Dockerfile
```
Create a multi-stage Dockerfile for the Java app:
- Use Maven to build
- Use smaller JRE image for runtime
- Expose port 8080
```

**AI's First Draft:**
```dockerfile
FROM eclipse-temurin:17-jre-alpine
```

**Build Error:**
```
no match for platform in manifest: not found
```

**Problem:**
Alpine image not available for ARM64 (Mac M1/M2).

**Refinement Applied:**
Changed to standard JRE image:
```dockerfile
FROM eclipse-temurin:17-jre
```

**Trade-off:**
- Larger image size (~200MB vs ~100MB)
- Better platform compatibility
- For production, would use platform-specific images in CI/CD

---

## API Design

### Prompt 5: Request Validation
```
Add validation for:
- Initial balance >= 0
- Transfer amount > 0 and <= 2 decimal places
- BigDecimal for money (not double)
```

**AI's Output:**
Correctly used `BigDecimal` and added validation annotations:
```java
@NotNull(message = "Initial balance is required")
@DecimalMin(value = "0.0", message = "Balance must be non-negative")
private BigDecimal initialBalance;
```

**No Refinement Needed:**
This was correct on first try. AI handled BigDecimal precision and validation properly.

---

## Key Insights

### What AI Did Well
1. **Project Structure**: Generated clean layered architecture (API → Service → Repository)
2. **Spring Boot Setup**: Correct annotations and dependency injection
3. **Error Handling**: Good use of custom exceptions and global exception handler
4. **BigDecimal Usage**: Properly handled money precision from the start

### What Required Human Refinement
1. **Concurrency Strategy**: Changed from global lock to account-level locking
2. **Deadlock Prevention**: Added lock ordering logic (AI missed this critical detail)
3. **Exception Handling**: Added missing checked exception declarations in tests
4. **Docker Platform Issues**: Fixed Alpine compatibility for ARM64

### Development Approach
1. Start with AI-generated scaffold
2. Identify performance/correctness issues
3. Refine critical sections (concurrency, validation)
4. Write tests to verify behavior
5. Iterate based on test failures

### Time Breakdown
- AI generation: ~30 minutes (endpoints, models, basic logic)
- Refinement: ~90 minutes (concurrency, tests, Docker)
- Testing & verification: ~60 minutes (manual testing, fixing issues)

**Total: ~3 hours**

---

## Prompts That Worked Best

### Specific, Constraint-Driven Prompts
✅ "Use account-level locking with deadlock prevention"
✅ "Write concurrency test with 20 threads and verify exactly 10 succeed"
✅ "Use ConcurrentHashMap for repository storage"

### Vague Prompts (Less Effective)
❌ "Make it thread-safe"
❌ "Add tests"
❌ "Handle errors"

**Lesson:** Specific requirements → better AI output. Vague prompts → more refinement needed.

---

## Final Assessment

**What AI Replaced:**
- Boilerplate code (DTOs, controllers, models)
- Basic Spring Boot configuration
- Standard CRUD operations

**What Required Engineering Judgment:**
- Choosing account-level locking over global lock
- Implementing deadlock prevention via lock ordering
- Designing comprehensive concurrency tests
- Understanding performance trade-offs

**Conclusion:**
AI accelerated development significantly but **critical design decisions still required human expertise**. The concurrency strategy, which is the most important part of this system, needed manual refinement based on systems design knowledge.
