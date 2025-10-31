from decimal import Decimal
from datetime import datetime
from pydantic import BaseModel, Field, field_validator


class CreateAccountRequest(BaseModel):
    initial_balance: Decimal = Field(..., ge=0, description="Initial account balance")

    @field_validator('initial_balance')
    @classmethod
    def validate_decimal_places(cls, v: Decimal) -> Decimal:
        if v.as_tuple().exponent < -2:
            raise ValueError("Balance cannot have more than 2 decimal places")
        return v


class UpdateBalanceRequest(BaseModel):
    balance: Decimal = Field(..., ge=0, description="New account balance")

    @field_validator('balance')
    @classmethod
    def validate_decimal_places(cls, v: Decimal) -> Decimal:
        if v.as_tuple().exponent < -2:
            raise ValueError("Balance cannot have more than 2 decimal places")
        return v


class AccountResponse(BaseModel):
    id: str
    balance: Decimal
    created_at: datetime

    class Config:
        json_encoders = {
            Decimal: str
        }
