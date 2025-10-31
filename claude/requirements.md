# Requirements

## Domain Models

### Account
- ID: string (unique)
- Balance: Decimal (non-negative)
- Lock: threading.Lock (per-account)

### Transaction
- ID: string (unique)
- From/To account IDs
- Amount: Decimal (max 2 decimals)
- Status: pending/completed/failed
- Timestamp
- Immutable after creation

## Business Rules

**Account Creation:**
- Initial balance >= 0
- Unique ID generated

**Transfers:**
- Both accounts must exist
- Sender balance >= amount
- Amount > 0 and max 2 decimals
- Atomic (all-or-nothing)
- Transaction record always created

**Balance Integrity:**
- Never negative
- Money conservation (sum constant)
- No race conditions

## API Endpoints

**Interactive Docs:**
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

### POST /accounts
- Request: `{"initial_balance": "100.00"}`
- Response: `{"id": "...", "balance": "100.00"}`
- Status: 201

### GET /accounts/{id}
- Response: `{"id": "...", "balance": "50.00"}`
- Status: 200 / 404

### POST /transactions
- Request: `{"from_account_id": "...", "to_account_id": "...", "amount": "25.50"}`
- Response: `{"id": "...", "status": "COMPLETED", ...}`
- Status: 201 / 400 / 404

**Error Codes:**
- `AccountNotFoundException` → 404
- `InsufficientFundsException` → 400
- `InvalidAmountException` → 400

## Concurrency Requirements

**Thread-Safety:**
- Account-level locking (not global)
- Lock ordering by account ID
- No deadlocks, no race conditions

**Critical Section:**
```python
# Must be atomic:
1. Lock both accounts (sorted order)
2. Check sender balance
3. Debit sender
4. Credit receiver
5. Release locks
```

## Testing Requirements

**Unit Tests:**
- Happy path
- Insufficient funds
- Account not found
- Invalid amounts (negative, >2 decimals)
- Exact balance transfer
- Transfer to self

**Concurrency Tests:**

1. Race condition: 20 threads, $1000 account, $100 each
   - Expected: 10 succeed, 10 fail, balance = $0

2. Deadlock: Thread1 A→B, Thread2 B→A (100x each)
   - Expected: Both complete <5s

3. Parallelism: A→B + C→D concurrent
   - Expected: Timing overlap

4. Money conservation: 5 accounts, 100 random transfers
   - Expected: Sum unchanged

## Data Types

**Money = Decimal**
```python
from decimal import Decimal
amount = Decimal("10.50")  # CORRECT
amount = 10.50            # WRONG (float)
```

**Validation:**
- Max 2 decimal places
- Positive for transfers
- Non-negative for balances

## Validation Checklist

- [ ] Thread-safe operations
- [ ] No race conditions
- [ ] No deadlocks (lock ordering)
- [ ] Money conservation
- [ ] Tests fail on broken code
- [ ] Decimal precision (max 2)
- [ ] No negative balances
- [ ] Design documented
