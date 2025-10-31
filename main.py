import subprocess
import sys
import uvicorn
from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from api.routes import router
from middleware import api_key_middleware
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


@app.on_event("startup")
async def startup_event():
    """Run database migrations on startup"""
    from config import get_settings
    settings = get_settings()

    if settings.app_env == "test":
        print("Skipping migrations in test environment...")
        return

    try:
        print("Running database migrations...")
        result = subprocess.run(
            ["alembic", "upgrade", "head"],
            check=True,
            capture_output=True,
            text=True
        )
        print(f"Migration completed successfully: {result.stdout}")
    except subprocess.CalledProcessError as e:
        print(f"Migration failed: {e.stderr}", file=sys.stderr)
        raise
    except Exception as e:
        print(f"Unexpected error during migration: {str(e)}", file=sys.stderr)
        raise


async def seed_mock_data():
    """Seed mock accounts if they don't exist"""
    import psycopg2
    from config import get_settings
    from datetime import datetime

    settings = get_settings()

    conn = psycopg2.connect(
        host=settings.db_host,
        port=settings.db_port,
        database=settings.db_name,
        user=settings.db_user,
        password=settings.db_password
    )

    try:
        cursor = conn.cursor()

        mock_accounts = [
            {"id": "test_account_1", "balance": 1000.00},
            {"id": "test_account_2", "balance": 2000.00}
        ]

        for mock_account in mock_accounts:
            cursor.execute("SELECT id FROM accounts WHERE id = %s", (mock_account["id"],))
            if cursor.fetchone():
                print(f"Account {mock_account['id']} already exists, skipping...")
            else:
                now = datetime.utcnow()
                cursor.execute(
                    "INSERT INTO accounts (id, balance, created_at, updated_at) VALUES (%s, %s, %s, %s)",
                    (mock_account["id"], mock_account["balance"], now, now)
                )
                print(f"Created mock account: {mock_account['id']} with balance {mock_account['balance']}")

        conn.commit()
    finally:
        cursor.close()
        conn.close()


# Register middleware
api_key_middleware(app)

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
