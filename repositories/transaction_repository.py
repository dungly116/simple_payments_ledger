from abc import ABC, abstractmethod
from typing import Optional, Dict
from threading import Lock

from models import Transaction


class TransactionRepository(ABC):
    """
    Abstract repository for transaction persistence.

    Allows easy migration to database-backed storage.
    """

    @abstractmethod
    def save(self, transaction: Transaction) -> Transaction:
        """Save transaction to storage."""
        pass

    @abstractmethod
    def find_by_id(self, transaction_id: str) -> Optional[Transaction]:
        """Retrieve transaction by ID."""
        pass


class InMemoryTransactionRepository(TransactionRepository):
    """
    In-memory implementation using dictionary.

    Thread-safety: Uses lock for write operations.
    """

    def __init__(self):
        self._storage: Dict[str, Transaction] = {}
        self._lock = Lock()

    def save(self, transaction: Transaction) -> Transaction:
        """
        Store transaction in memory.

        Thread-safe: Locks during dictionary write.
        """
        with self._lock:
            self._storage[transaction.id] = transaction
        return transaction

    def find_by_id(self, transaction_id: str) -> Optional[Transaction]:
        """
        Retrieve transaction by ID.

        Thread-safe: Dictionary reads are atomic in CPython.
        """
        return self._storage.get(transaction_id)
