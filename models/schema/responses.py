from typing import Optional, Any, Generic, TypeVar
from pydantic import BaseModel

T = TypeVar('T')


class BaseResponse(BaseModel, Generic[T]):
    """Common response wrapper for all API responses"""
    success: bool
    message: Optional[str] = None
    data: Optional[T] = None
    error: Optional[str] = None


class ErrorDetail(BaseModel):
    """Detailed error information"""
    code: str
    message: str
    details: Optional[dict] = None


class ErrorResponse(BaseModel):
    """Standard error response"""
    success: bool = False
    error: ErrorDetail
