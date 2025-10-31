from abc import ABC, abstractmethod
from typing import Optional, Dict
from threading import Lock

from models import Account


class AccountRepository(ABC):
    """
    Abstract repository for account persistence.

    Defines interface that can be swapped for database implementation.
    """

    @abstractmethod
    def save(self, account: Account) -> Account:
        """Save account to storage."""
        pass

    @abstractmethod
    def find_by_id(self, account_id: str) -> Optional[Account]:
        """Retrieve account by ID."""
        pass

    @abstractmethod
    def exists(self, account_id: str) -> bool:
        """Check if account exists."""
        pass


class InMemoryAccountRepository(AccountRepository):
    """
    In-memory implementation using dictionary.

    Thread-safety: Uses a lock for dictionary mutations.
    Note: Individual account operations use account-level locks.
    """

    def __init__(self):
        self._storage: Dict[str, Account] = {}
        self._lock = Lock()

    def save(self, account: Account) -> Account:
        """
        Store account in memory.

        Thread-safe: Locks during dictionary write.
        """
        with self._lock:
            self._storage[account.id] = account
        return account

    def find_by_id(self, account_id: str) -> Optional[Account]:
        """
        Retrieve account by ID.

        Thread-safe: Dictionary reads are atomic in CPython.
        """
        return self._storage.get(account_id)

    def exists(self, account_id: str) -> bool:
        """Check if account exists."""
        return account_id in self._storage
