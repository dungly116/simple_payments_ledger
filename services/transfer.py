from decimal import Decimal
from threading import Lock

from models import Transaction
from repositories import AccountRepository, TransactionRepository
from exceptions import (
    AccountNotFoundException,
    InsufficientFundsException,
    InvalidAmountException
)


class TransferService:
    """
    Service for atomic fund transfers between accounts.

    Key Design Decision: Account-Level Locking
    - Each account has its own lock
    - Transfers acquire locks on both accounts in sorted order (by account ID)
    - This prevents deadlocks while allowing parallel transfers on different account pairs

    Alternative Considered: Global lock for all transfers
    - Pro: Simpler, no deadlock risk
    - Con: Serializes ALL transactions (poor throughput for 100K users)
    """

    def __init__(
        self,
        account_repository: AccountRepository,
        transaction_repository: TransactionRepository
    ):
        self.account_repository = account_repository
        self.transaction_repository = transaction_repository

    def transfer(
        self,
        from_account_id: str,
        to_account_id: str,
        amount: Decimal
    ) -> Transaction:
        """
        Transfer funds atomically between two accounts.

        Args:
            from_account_id: Source account ID
            to_account_id: Destination account ID
            amount: Amount to transfer

        Returns:
            Completed transaction record

        Raises:
            InvalidAmountException: If amount <= 0 or transferring to same account
            AccountNotFoundException: If either account doesn't exist
            InsufficientFundsException: If source account has insufficient balance
        """
        if amount <= 0:
            raise InvalidAmountException("Amount must be positive")

        if from_account_id == to_account_id:
            raise InvalidAmountException("Cannot transfer to same account")

        from_account = self.account_repository.find_by_id(from_account_id)
        if from_account is None:
            raise AccountNotFoundException(f"Account {from_account_id} not found")

        to_account = self.account_repository.find_by_id(to_account_id)
        if to_account is None:
            raise AccountNotFoundException(f"Account {to_account_id} not found")

        transaction = Transaction(from_account_id, to_account_id, amount)

        self._execute_transfer(from_account, to_account, amount, transaction)

        return self.transaction_repository.save(transaction)

    def _execute_transfer(self, from_account, to_account, amount: Decimal, transaction: Transaction):
        """
        Execute transfer with proper locking to ensure atomicity.

        CRITICAL: Locks must be acquired in consistent order to prevent deadlock.

        Deadlock scenario if not careful:
            Thread 1: Lock A -> Lock B
            Thread 2: Lock B -> Lock A
            Both threads wait forever

        Solution: Always lock in ascending ID order
        """
        first_account = from_account if from_account.id < to_account.id else to_account
        second_account = to_account if from_account.id < to_account.id else from_account

        first_lock = first_account.get_lock()
        second_lock = second_account.get_lock()

        first_lock.acquire()
        try:
            second_lock.acquire()
            try:
                if from_account.balance < amount:
                    transaction.mark_failed("Insufficient funds")
                    raise InsufficientFundsException(
                        f"Account {from_account.id} has insufficient balance"
                    )

                from_account.balance -= amount
                to_account.balance += amount

                transaction.mark_completed()

            finally:
                second_lock.release()
        finally:
            first_lock.release()
