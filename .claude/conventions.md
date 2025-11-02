# Coding Conventions

**Purpose:** Maintain consistency across codebase for readability and maintainability

---

## Naming Conventions

### Variables & Functions
**Format:** `snake_case`

**Examples:**
```python
account_balance = Decimal("100.00")
transfer_amount = Decimal("25.50")

def get_account(account_id: str) -> Account:
    pass

def execute_transfer(from_id: str, to_id: str, amount: Decimal) -> Transaction:
    pass
```

---

### Classes
**Format:** `PascalCase`

**Examples:**
```python
class Account:
    pass

class TransferService:
    pass

class AccountRepository:
    pass

class InsufficientFundsException(Exception):
    pass

class AccountNotFoundException(Exception):
    pass
```

---

### Constants
**Format:** `UPPER_SNAKE_CASE`

**Examples:**
```python
MAX_DECIMAL_PLACES = 2
DEFAULT_TIMEOUT = 30
API_VERSION = "v1"
```

---

### Private Members
**Format:** Prefix with single underscore `_`

**Examples:**
```python
class AccountRepository:
    def __init__(self):
        self._storage = {}  # Private
        self._lock = Lock()  # Private

    def _validate_balance(self, amount: Decimal) -> None:  # Private method
        if amount < 0:
            raise ValueError("Balance cannot be negative")

    def get_account(self, account_id: str) -> Account:  # Public method
        return self._storage.get(account_id)
```

**Rule:** Use `_` for internal implementation details not meant for external use.

---

## Comments

### Good Comments (Explain WHY)

```python
# Good - explains reasoning
# Lock ordering prevents deadlock by ensuring consistent acquisition order
first = from_acc if from_acc.id < to_acc.id else to_acc

# Good - explains business rule
# Financial regulations require max 2 decimal places
if amount.as_tuple().exponent < -2:
    raise InvalidAmountException("Max 2 decimal places allowed")

# Good - explains non-obvious behavior
# Using Barrier to ensure all threads start simultaneously for race condition test
barrier = threading.Barrier(num_threads)
```

### Bad Comments (Explain WHAT - Obvious)

```python
# Bad - obvious from code
# This increments the counter
counter += 1

# Bad - repeats variable name
# The account balance
account_balance = get_balance()

# Bad - states the obvious
# Loop through accounts
for account in accounts:
    process(account)
```

---

## Comment Rules

1. **English Only**
   - No Vietnamese
   - No mixed languages
   - Professional technical English

2. **No Emojis**
   ```python
   # Bad
   # Lock ordering prevents deadlock 🔒🚫

   # Good
   # Lock ordering prevents deadlock
   ```

3. **Explain Reasoning, Not Code**
   - Focus on "why", not "what"
   - Assume reader understands Python
   - Explain domain knowledge, tradeoffs, edge cases

4. **Update Comments When Code Changes**
   - Outdated comments worse than no comments
   - Delete comments if code becomes self-explanatory

---

## Error Handling

### Good Error Handling

```python
def transfer(self, from_id: str, to_id: str, amount: Decimal) -> Transaction:
    try:
        from_account = self._account_repo.get(from_id)
        to_account = self._account_repo.get(to_id)

        if from_account is None:
            raise AccountNotFoundException(f"Account {from_id} not found")
        if to_account is None:
            raise AccountNotFoundException(f"Account {to_id} not found")

        if from_account.balance < amount:
            raise InsufficientFundsException(
                f"Account {from_id} has insufficient funds"
            )

        return self._execute_transfer(from_account, to_account, amount)

    except InsufficientFundsException as e:
        logger.error(f"Transfer failed: {e}")
        raise  # Re-raise for caller to handle

    except AccountNotFoundException as e:
        logger.error(f"Transfer failed: {e}")
        raise

    finally:
        # Cleanup (if needed)
        pass
```

**Key Principles:**
- Catch specific exceptions, not bare `except:`
- Log errors before re-raising
- Re-raise exceptions (don't swallow)
- Use `finally` for cleanup

---

### Bad Error Handling

```python
# Bad - swallows exception (no visibility)
try:
    transfer(from_acc, to_acc, amount)
except:
    pass

# Bad - bare except catches everything (including KeyboardInterrupt)
try:
    transfer(from_acc, to_acc, amount)
except:
    return None

# Bad - loses original exception context
try:
    transfer(from_acc, to_acc, amount)
except InsufficientFundsException:
    raise Exception("Transfer failed")  # Loses original error details
```

---

## Import Organization

### Standard Order

```python
# 1. Standard library imports
from decimal import Decimal
from threading import Lock
from typing import Optional, Dict
from uuid import uuid4

# 2. Third-party imports
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

# 3. Local application imports
from app.models import Account, Transaction
from app.services import TransferService
from app.repositories import AccountRepository
```

**Rules:**
1. Group imports by category (stdlib, third-party, local)
2. Blank line between groups
3. Alphabetical order within groups
4. One import per line for `from ... import` (unless related)

---

### Import Examples

```python
# Good
from typing import Optional, Dict, List

# Good
from fastapi import FastAPI
from fastapi import HTTPException

# Also acceptable
from fastapi import FastAPI, HTTPException

# Bad - mixed categories
from fastapi import FastAPI
from app.models import Account
from decimal import Decimal
```

---

## Type Hints

**Always use type hints** for function signatures.

### Function Signatures

```python
def transfer(
    self,
    from_account_id: str,
    to_account_id: str,
    amount: Decimal
) -> Transaction:
    """Transfer funds between accounts."""
    pass

def get_account(self, account_id: str) -> Optional[Account]:
    """Retrieve account by ID. Returns None if not found."""
    return self._storage.get(account_id)

def calculate_total_balance(self) -> Decimal:
    """Sum all account balances."""
    return sum(acc.balance for acc in self._storage.values())
```

---

### Variable Type Hints (When Helpful)

```python
# Good - clarifies complex type
accounts: Dict[str, Account] = {}

# Good - clarifies from context
balance: Decimal = Decimal("0.00")

# Unnecessary - obvious from assignment
name: str = "Alice"  # Overkill
count: int = 0       # Overkill
```

---

## Docstrings

### Function Docstrings

**Format:** Google style

```python
def transfer(self, from_id: str, to_id: str, amount: Decimal) -> Transaction:
    """
    Transfer funds atomically between two accounts.

    Args:
        from_id: Source account ID
        to_id: Destination account ID
        amount: Transfer amount (must be positive, max 2 decimals)

    Returns:
        Completed transaction record with status COMPLETED

    Raises:
        AccountNotFoundException: If either account doesn't exist
        InsufficientFundsException: If source account balance < amount
        InvalidAmountException: If amount <= 0 or > 2 decimal places
    """
    pass
```

---

### Class Docstrings

```python
class TransferService:
    """
    Service for managing fund transfers between accounts.

    Implements thread-safe transfers with account-level locking
    and deadlock prevention via lock ordering.

    Attributes:
        account_repo: Repository for account storage
        transaction_repo: Repository for transaction records
    """

    def __init__(
        self,
        account_repo: AccountRepository,
        transaction_repo: TransactionRepository
    ):
        self.account_repo = account_repo
        self.transaction_repo = transaction_repo
```

---

### Module Docstrings

```python
"""
Transfer service for atomic fund transfers.

This module implements thread-safe account-to-account transfers
using account-level locking with deadlock prevention.
"""

from decimal import Decimal
from app.models import Account
```

---

## Code Formatting

### Line Length
**Max:** 88 characters (Black default)

```python
# Good - under 88 chars
def transfer(from_id: str, to_id: str, amount: Decimal) -> Transaction:
    pass

# Good - break long lines
raise InsufficientFundsException(
    f"Account {from_id} has insufficient funds. "
    f"Balance: {balance}, Required: {amount}"
)
```

---

### Indentation
**Standard:** 4 spaces (not tabs)

```python
def transfer(self, from_id: str, to_id: str, amount: Decimal) -> Transaction:
    if from_id == to_id:
        raise InvalidTransferException("Cannot transfer to self")

    with self._lock:
        from_account = self._get_account(from_id)
        to_account = self._get_account(to_id)

        if from_account.balance < amount:
            raise InsufficientFundsException()

        from_account.debit(amount)
        to_account.credit(amount)

    return Transaction(...)
```

---

### Blank Lines

```python
# 2 blank lines between top-level classes/functions
class Account:
    pass


class Transaction:
    pass


def create_account():
    pass


# 1 blank line between methods
class AccountRepository:
    def get(self, account_id: str) -> Optional[Account]:
        return self._storage.get(account_id)

    def save(self, account: Account) -> None:
        self._storage[account.id] = account

    def delete(self, account_id: str) -> None:
        del self._storage[account_id]
```

---

## Specific Conventions

### Decimal Usage

```python
# Good
amount = Decimal("10.50")
balance = Decimal("0.00")

# Bad - float has precision issues
amount = 10.50
balance = 0.0
```

**Rule:** Always use `Decimal` for financial amounts.

---

### Lock Acquisition

```python
# Good - explicit acquire/release for ordering
first.get_lock().acquire()
second.get_lock().acquire()
try:
    # Critical section
finally:
    second.get_lock().release()
    first.get_lock().release()

# Good - context manager when ordering not needed
with self._lock:
    # Critical section
```

---

### Exception Naming

```python
# Good - specific exception classes
class InsufficientFundsException(Exception):
    pass

class AccountNotFoundException(Exception):
    pass

class InvalidAmountException(Exception):
    pass

# Bad - generic exceptions
raise Exception("Not enough money")
raise ValueError("Account not found")
```

**Rule:** Use domain-specific exception classes.

---

## Testing Conventions

### Test Function Naming

**Format:** `test_<what>_<when>_<expected>`

```python
def test_transfer_with_sufficient_funds_succeeds():
    pass

def test_transfer_with_insufficient_funds_raises_exception():
    pass

def test_concurrent_transfers_prevent_negative_balance():
    pass

def test_bidirectional_transfers_prevent_deadlock():
    pass
```

---

### AAA Pattern (Arrange-Act-Assert)

```python
def test_transfer_with_sufficient_funds_succeeds():
    # Arrange
    service = TransferService(account_repo, transaction_repo)
    alice = account_repo.create(Decimal("1000"))
    bob = account_repo.create(Decimal("500"))

    # Act
    transaction = service.transfer(alice.id, bob.id, Decimal("200"))

    # Assert
    assert transaction.status == TransactionStatus.COMPLETED
    assert account_repo.get(alice.id).balance == Decimal("800")
    assert account_repo.get(bob.id).balance == Decimal("700")
```

---

### Test Fixtures

```python
import pytest
from decimal import Decimal

@pytest.fixture
def account_repo():
    return InMemoryAccountRepository()

@pytest.fixture
def transaction_repo():
    return InMemoryTransactionRepository()

@pytest.fixture
def transfer_service(account_repo, transaction_repo):
    return TransferService(account_repo, transaction_repo)

def test_transfer(transfer_service, account_repo):
    alice = account_repo.create(Decimal("1000"))
    bob = account_repo.create(Decimal("500"))
    transfer_service.transfer(alice.id, bob.id, Decimal("200"))
```

---

## Tools

### Recommended

- **Linter:** `pylint` or `flake8`
- **Formatter:** `black` (auto-format)
- **Type Checker:** `mypy`
- **Import Sorter:** `isort`

### Pre-commit Hook Example

```bash
#!/bin/bash
black app/
isort app/
mypy app/
pytest
```

---

**Last Updated:** 2025-10-31
