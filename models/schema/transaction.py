from decimal import Decimal
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field, field_validator


class TransferRequest(BaseModel):
    from_account_id: str = Field(..., description="Source account ID")
    to_account_id: str = Field(..., description="Destination account ID")
    amount: Decimal = Field(..., gt=0, description="Transfer amount")

    @field_validator('amount')
    @classmethod
    def validate_decimal_places(cls, v: Decimal) -> Decimal:
        if v.as_tuple().exponent < -2:
            raise ValueError("Amount cannot have more than 2 decimal places")
        return v


class TransactionResponse(BaseModel):
    id: str
    from_account_id: str
    to_account_id: str
    amount: Decimal
    status: str
    created_at: datetime
    error_message: Optional[str] = None

    class Config:
        json_encoders = {
            Decimal: str
        }
