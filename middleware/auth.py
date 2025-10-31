from fastapi import Request, HTTPException, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware
from config import get_settings


class APIKeyMiddleware(BaseHTTPMiddleware):
    """Middleware to validate API key for all requests except health check"""

    async def dispatch(self, request: Request, call_next):
        settings = get_settings()

        # Skip API key validation for health check endpoint
        if request.url.path in ["/", "/health"]:
            return await call_next(request)

        # Skip validation for OpenAPI docs endpoints
        if request.url.path in ["/docs", "/redoc", "/openapi.json"]:
            return await call_next(request)

        # Skip API key validation in test environment
        if settings.app_env == "test":
            return await call_next(request)

        # Get API key from header
        api_key = request.headers.get("X-API-Key")

        # Validate API key
        if not api_key:
            return JSONResponse(
                status_code=status.HTTP_401_UNAUTHORIZED,
                content={
                    "error": "Unauthorized",
                    "message": "API key is required. Please provide X-API-Key header."
                }
            )

        if api_key != settings.api_key:
            return JSONResponse(
                status_code=status.HTTP_403_FORBIDDEN,
                content={
                    "error": "Forbidden",
                    "message": "Invalid API key provided."
                }
            )

        # API key is valid, proceed with request
        response = await call_next(request)
        return response


def api_key_middleware(app):
    """Add API key middleware to FastAPI app"""
    app.add_middleware(APIKeyMiddleware)
