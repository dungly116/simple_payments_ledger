import pytest
from decimal import Decimal
from services import AccountService, TransferService
from repositories import InMemoryAccountRepository, InMemoryTransactionRepository
from exceptions import (
    AccountNotFoundException,
    InsufficientFundsException,
    InvalidAmountException
)
from models import TransactionStatus


@pytest.fixture
def account_repo():
    return InMemoryAccountRepository()


@pytest.fixture
def transaction_repo():
    return InMemoryTransactionRepository()


@pytest.fixture
def account_service(account_repo):
    return AccountService(account_repo)


@pytest.fixture
def transfer_service(account_repo, transaction_repo):
    return TransferService(account_repo, transaction_repo)


class TestTransferService:
    """Unit tests for transfer business logic."""

    def test_successful_transfer(self, account_service, transfer_service):
        """Test basic successful transfer."""
        acc_a = account_service.create_account(Decimal("1000"))
        acc_b = account_service.create_account(Decimal("500"))

        transaction = transfer_service.transfer(acc_a.id, acc_b.id, Decimal("300"))

        assert transaction.status == TransactionStatus.COMPLETED
        assert acc_a.balance == Decimal("700")
        assert acc_b.balance == Decimal("800")

    def test_insufficient_funds(self, account_service, transfer_service):
        """Test transfer with insufficient balance."""
        acc_a = account_service.create_account(Decimal("100"))
        acc_b = account_service.create_account(Decimal("0"))

        with pytest.raises(InsufficientFundsException):
            transfer_service.transfer(acc_a.id, acc_b.id, Decimal("200"))

        assert acc_a.balance == Decimal("100")
        assert acc_b.balance == Decimal("0")

    def test_negative_amount(self, account_service, transfer_service):
        """Test transfer with negative amount."""
        acc_a = account_service.create_account(Decimal("1000"))
        acc_b = account_service.create_account(Decimal("0"))

        with pytest.raises(InvalidAmountException):
            transfer_service.transfer(acc_a.id, acc_b.id, Decimal("-100"))

    def test_zero_amount(self, account_service, transfer_service):
        """Test transfer with zero amount."""
        acc_a = account_service.create_account(Decimal("1000"))
        acc_b = account_service.create_account(Decimal("0"))

        with pytest.raises(InvalidAmountException):
            transfer_service.transfer(acc_a.id, acc_b.id, Decimal("0"))

    def test_account_not_found_source(self, account_service, transfer_service):
        """Test transfer with non-existent source account."""
        acc_b = account_service.create_account(Decimal("0"))

        with pytest.raises(AccountNotFoundException):
            transfer_service.transfer("nonexistent", acc_b.id, Decimal("100"))

    def test_account_not_found_destination(self, account_service, transfer_service):
        """Test transfer with non-existent destination account."""
        acc_a = account_service.create_account(Decimal("1000"))

        with pytest.raises(AccountNotFoundException):
            transfer_service.transfer(acc_a.id, "nonexistent", Decimal("100"))

    def test_transfer_to_self(self, account_service, transfer_service):
        """Test transfer to same account (valid operation)."""
        acc_a = account_service.create_account(Decimal("1000"))

        transaction = transfer_service.transfer(acc_a.id, acc_a.id, Decimal("100"))

        assert transaction.status == TransactionStatus.COMPLETED
        assert acc_a.balance == Decimal("1000")

    def test_transfer_exact_balance(self, account_service, transfer_service):
        """Test transfer of exact account balance."""
        acc_a = account_service.create_account(Decimal("500"))
        acc_b = account_service.create_account(Decimal("0"))

        transaction = transfer_service.transfer(acc_a.id, acc_b.id, Decimal("500"))

        assert transaction.status == TransactionStatus.COMPLETED
        assert acc_a.balance == Decimal("0")
        assert acc_b.balance == Decimal("500")

    def test_multiple_sequential_transfers(self, account_service, transfer_service):
        """Test multiple transfers in sequence."""
        acc_a = account_service.create_account(Decimal("1000"))
        acc_b = account_service.create_account(Decimal("500"))
        acc_c = account_service.create_account(Decimal("0"))

        transfer_service.transfer(acc_a.id, acc_b.id, Decimal("200"))
        transfer_service.transfer(acc_b.id, acc_c.id, Decimal("300"))
        transfer_service.transfer(acc_a.id, acc_c.id, Decimal("100"))

        assert acc_a.balance == Decimal("700")
        assert acc_b.balance == Decimal("400")
        assert acc_c.balance == Decimal("400")
