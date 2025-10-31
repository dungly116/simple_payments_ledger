# Coding Conventions

## Naming

**Variables/Functions:**
- snake_case: `account_balance`, `transfer_amount`, `get_account()`

**Classes:**
- PascalCase: `TransferService`, `AccountRepository`, `InsufficientFundsException`

**Constants:**
- UPPER_SNAKE_CASE: `MAX_DECIMAL_PLACES`, `DEFAULT_TIMEOUT`

**Private:**
- Prefix with `_`: `_storage`, `_execute_transfer()`

## Comments

```python
# Good - explains WHY
# Lock ordering prevents deadlock

# Bad - explains WHAT (obvious)
# This increments the counter
```

**Rules:**
- English only
- No Vietnamese
- No emojis
- Explain reasoning, not code

## Error Handling

```python
# Good
try:
    transfer(from_acc, to_acc, amount)
except InsufficientFundsException as e:
    logger.error(f"Transfer failed: {e}")
    raise
finally:
    cleanup()

# Bad - swallows exception
try:
    transfer(from_acc, to_acc, amount)
except:
    pass
```

## Imports

```python
# stdlib
from decimal import Decimal
from threading import Lock

# third-party
from fastapi import FastAPI
from pydantic import BaseModel

# local
from app.models import Account
from app.services import TransferService
```

## Type Hints

```python
def transfer(
    self,
    from_account_id: str,
    to_account_id: str,
    amount: Decimal
) -> Transaction:
    pass
```

## Docstrings

```python
def transfer(self, from_id: str, to_id: str, amount: Decimal) -> Transaction:
    """
    Transfer funds atomically between accounts.

    Args:
        from_id: Source account ID
        to_id: Destination account ID
        amount: Transfer amount (max 2 decimals)

    Returns:
        Completed transaction record

    Raises:
        InsufficientFundsException: Not enough balance
        AccountNotFoundException: Account doesn't exist
    """
```
