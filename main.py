import uvicorn
from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from middleware import api_key_middleware
from api.routes import router
from utils import (
    account_not_found_handler,
    insufficient_funds_handler,
    invalid_amount_handler,
    validation_error_handler,
    global_exception_handler
)
from exceptions import (
    AccountNotFoundException,
    InsufficientFundsException,
    InvalidAmountException
)

app = FastAPI(
    title="Simple Payments Ledger API",
    description="In-memory transactional ledger for managing accounts and transfers",
    version="1.0.0"
)


# Register middleware
# api_key_middleware(app)  # Disabled API key authentication

# Register exception handlers
app.add_exception_handler(AccountNotFoundException, account_not_found_handler)
app.add_exception_handler(InsufficientFundsException, insufficient_funds_handler)
app.add_exception_handler(InvalidAmountException, invalid_amount_handler)
app.add_exception_handler(RequestValidationError, validation_error_handler)
app.add_exception_handler(Exception, global_exception_handler)

app.include_router(router)


@app.get("/health")
def root():
    """Health check endpoint."""
    return {"status": "ok", "service": "payments-ledger"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
