class AccountNotFoundException(Exception):
    """Raised when account ID does not exist."""
    pass


class InsufficientFundsException(Exception):
    """Raised when account balance is insufficient for transfer."""
    pass


class InvalidAmountException(Exception):
    """Raised when transaction amount is invalid (negative or zero)."""
    pass
