from decimal import Decimal
from datetime import datetime
from threading import Lock
import uuid


class Account:
    """
    Represents a user account with balance.

    Thread-safety: Each account has its own lock for atomic operations.
    """

    def __init__(self, account_id: str, initial_balance: Decimal):
        if initial_balance < 0:
            raise ValueError("Initial balance cannot be negative")

        self.id = account_id
        self.balance = initial_balance
        self.created_at = datetime.utcnow()
        self._lock = Lock()

    def get_lock(self) -> Lock:
        """Returns the lock associated with this account for external synchronization."""
        return self._lock

    def to_dict(self) -> dict:
        """Serialize account to dictionary."""
        return {
            "id": self.id,
            "balance": f"{self.balance:.2f}",
            "created_at": self.created_at.isoformat()
        }

    @staticmethod
    def generate_id() -> str:
        """Generate unique account ID."""
        return f"acc_{uuid.uuid4().hex[:12]}"
