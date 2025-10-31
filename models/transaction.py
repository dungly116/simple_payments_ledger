from decimal import Decimal
from datetime import datetime
from enum import Enum
import uuid


class TransactionStatus(str, Enum):
    """Transaction lifecycle states."""
    PENDING = "pending"
    COMPLETED = "completed"
    FAILED = "failed"


class Transaction:
    """
    Represents a fund transfer between two accounts.

    Immutable record of a transaction attempt.
    """

    def __init__(
        self,
        from_account_id: str,
        to_account_id: str,
        amount: Decimal,
        transaction_id: str = None
    ):
        self.id = transaction_id or Transaction.generate_id()
        self.from_account_id = from_account_id
        self.to_account_id = to_account_id
        self.amount = amount
        self.status = TransactionStatus.PENDING
        self.created_at = datetime.utcnow()
        self.error_message = None

    def mark_completed(self):
        """Mark transaction as successfully completed."""
        self.status = TransactionStatus.COMPLETED

    def mark_failed(self, error_message: str):
        """Mark transaction as failed with reason."""
        self.status = TransactionStatus.FAILED
        self.error_message = error_message

    def to_dict(self) -> dict:
        """Serialize transaction to dictionary."""
        result = {
            "id": self.id,
            "from_account_id": self.from_account_id,
            "to_account_id": self.to_account_id,
            "amount": f"{self.amount:.2f}",
            "status": self.status.value,
            "created_at": self.created_at.isoformat()
        }
        if self.error_message:
            result["error_message"] = self.error_message
        return result

    @staticmethod
    def generate_id() -> str:
        """Generate unique transaction ID."""
        return f"txn_{uuid.uuid4().hex[:12]}"
