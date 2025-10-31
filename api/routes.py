from fastapi import APIRouter, HTTPException, status
from decimal import Decimal

from models.schema import (
    CreateAccountRequest,
    UpdateBalanceRequest,
    AccountResponse,
    TransferRequest,
    TransactionResponse
)
from services import AccountService, TransferService
from repositories import InMemoryAccountRepository, InMemoryTransactionRepository
from exceptions import (
    AccountNotFoundException,
    InsufficientFundsException,
    InvalidAmountException
)

router = APIRouter()

account_repo = InMemoryAccountRepository()
transaction_repo = InMemoryTransactionRepository()

account_service = AccountService(account_repo)
transfer_service = TransferService(account_repo, transaction_repo)


@router.post("/accounts", response_model=AccountResponse, status_code=status.HTTP_201_CREATED)
def create_account(request: CreateAccountRequest):
    """
    Create a new account with initial balance.

    Args:
        request: Account creation parameters

    Returns:
        Created account details

    Raises:
        400: If initial balance is negative
    """
    try:
        account = account_service.create_account(request.initial_balance)
        return account.to_dict()
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.get("/accounts/{account_id}", response_model=AccountResponse)
def get_account(account_id: str):
    """
    Retrieve account information by ID.

    Args:
        account_id: Account identifier

    Returns:
        Account details

    Raises:
        404: If account not found
    """
    try:
        account = account_service.get_account(account_id)
        return account.to_dict()
    except AccountNotFoundException as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.put("/accounts/{account_id}/balance", response_model=AccountResponse)
def update_account_balance(account_id: str, request: UpdateBalanceRequest):
    """
    Update account balance.

    Args:
        account_id: Account identifier
        request: Balance update parameters

    Returns:
        Updated account details

    Raises:
        400: If balance is invalid
        404: If account not found
    """
    try:
        account = account_service.update_balance(account_id, request.balance)
        return account.to_dict()
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except AccountNotFoundException as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.post("/transactions", response_model=TransactionResponse, status_code=status.HTTP_201_CREATED)
def create_transaction(request: TransferRequest):
    """
    Transfer funds between two accounts.

    This operation is atomic - both debit and credit succeed or fail together.

    Args:
        request: Transfer parameters

    Returns:
        Transaction record

    Raises:
        400: If amount is invalid or insufficient funds
        404: If either account not found
    """
    try:
        transaction = transfer_service.transfer(
            request.from_account_id,
            request.to_account_id,
            request.amount
        )
        return transaction.to_dict()
    except InvalidAmountException as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except InsufficientFundsException as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except AccountNotFoundException as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))
