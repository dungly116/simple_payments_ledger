from decimal import Decimal
from typing import Optional

from models import Account
from repositories import AccountRepository
from exceptions import AccountNotFoundException


class AccountService:
    """
    Service layer for account operations.

    Handles business logic for account creation and retrieval.
    """

    def __init__(self, account_repository: AccountRepository):
        self.account_repository = account_repository

    def create_account(self, initial_balance: Decimal) -> Account:
        """
        Create a new account with initial balance.

        Args:
            initial_balance: Starting balance (must be >= 0)

        Returns:
            Created account

        Raises:
            ValueError: If initial balance is negative
        """
        if initial_balance < 0:
            raise ValueError("Initial balance cannot be negative")

        account_id = Account.generate_id()
        account = Account(account_id, initial_balance)
        return self.account_repository.save(account)

    def get_account(self, account_id: str) -> Account:
        """
        Retrieve account by ID.

        Args:
            account_id: Account identifier

        Returns:
            Account object

        Raises:
            AccountNotFoundException: If account does not exist
        """
        account = self.account_repository.find_by_id(account_id)
        if account is None:
            raise AccountNotFoundException(f"Account {account_id} not found")
        return account

    def account_exists(self, account_id: str) -> bool:
        """Check if account exists."""
        return self.account_repository.exists(account_id)

    def update_balance(self, account_id: str, new_balance: Decimal) -> Account:
        """
        Update account balance.

        Args:
            account_id: Account identifier
            new_balance: New balance (must be >= 0)

        Returns:
            Updated account

        Raises:
            AccountNotFoundException: If account does not exist
            ValueError: If new balance is negative
        """
        if new_balance < 0:
            raise ValueError("Balance cannot be negative")

        account = self.account_repository.find_by_id(account_id)
        if account is None:
            raise AccountNotFoundException(f"Account {account_id} not found")

        account.balance = new_balance
        return self.account_repository.save(account)
