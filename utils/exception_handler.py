from fastapi import Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError

from exceptions import (
    AccountNotFoundException,
    InsufficientFundsException,
    InvalidAmountException
)
from config.constants import (
    HTTP_400_BAD_REQUEST,
    HTTP_404_NOT_FOUND,
    HTTP_422_UNPROCESSABLE_ENTITY,
    HTTP_500_INTERNAL_SERVER_ERROR,
    ERROR_CODE_ACCOUNT_NOT_FOUND,
    ERROR_CODE_INSUFFICIENT_FUNDS,
    ERROR_CODE_INVALID_AMOUNT,
    ERROR_CODE_VALIDATION_ERROR,
    ERROR_CODE_INTERNAL_SERVER_ERROR
)


async def account_not_found_handler(request: Request, exc: AccountNotFoundException):
    """Handle account not found errors"""
    return JSONResponse(
        status_code=HTTP_404_NOT_FOUND,
        content={
            "success": False,
            "error": {
                "code": ERROR_CODE_ACCOUNT_NOT_FOUND,
                "message": str(exc)
            }
        }
    )


async def insufficient_funds_handler(request: Request, exc: InsufficientFundsException):
    """Handle insufficient funds errors"""
    return JSONResponse(
        status_code=HTTP_400_BAD_REQUEST,
        content={
            "success": False,
            "error": {
                "code": ERROR_CODE_INSUFFICIENT_FUNDS,
                "message": str(exc)
            }
        }
    )


async def invalid_amount_handler(request: Request, exc: InvalidAmountException):
    """Handle invalid amount errors"""
    return JSONResponse(
        status_code=HTTP_400_BAD_REQUEST,
        content={
            "success": False,
            "error": {
                "code": ERROR_CODE_INVALID_AMOUNT,
                "message": str(exc)
            }
        }
    )


async def validation_error_handler(request: Request, exc: RequestValidationError):
    """Handle Pydantic validation errors"""
    return JSONResponse(
        status_code=HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "success": False,
            "error": {
                "code": ERROR_CODE_VALIDATION_ERROR,
                "message": "Invalid request data",
                "details": exc.errors()
            }
        }
    )


async def global_exception_handler(request: Request, exc: Exception):
    """Fallback handler for unhandled exceptions"""
    return JSONResponse(
        status_code=HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "success": False,
            "error": {
                "code": ERROR_CODE_INTERNAL_SERVER_ERROR,
                "message": "An unexpected error occurred",
                "details": str(exc) if request.app.debug else None
            }
        }
    )
