"""initial_setup

Revision ID: 6d84b1dfcb77
Revises: 
Create Date: 2025-10-31 15:33:42.380638

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '6d84b1dfcb77'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Create accounts table
    op.create_table(
        'accounts',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('balance', sa.Numeric(precision=20, scale=2), nullable=False),
        sa.Column('created_at', sa.DateTime(), nullable=False),
        sa.Column('updated_at', sa.DateTime(), nullable=False)
    )

    # Create transactions table
    op.create_table(
        'transactions',
        sa.Column('id', sa.String(36), primary_key=True),
        sa.Column('from_account_id', sa.String(36), nullable=False),
        sa.Column('to_account_id', sa.String(36), nullable=False),
        sa.Column('amount', sa.Numeric(precision=20, scale=2), nullable=False),
        sa.Column('status', sa.String(20), nullable=False),
        sa.Column('error_message', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False),
        sa.ForeignKeyConstraint(['from_account_id'], ['accounts.id']),
        sa.ForeignKeyConstraint(['to_account_id'], ['accounts.id'])
    )

    # Create indexes
    op.create_index('ix_accounts_created_at', 'accounts', ['created_at'])
    op.create_index('ix_transactions_from_account', 'transactions', ['from_account_id'])
    op.create_index('ix_transactions_to_account', 'transactions', ['to_account_id'])
    op.create_index('ix_transactions_created_at', 'transactions', ['created_at'])
    op.create_index('ix_transactions_status', 'transactions', ['status'])


def downgrade() -> None:
    # Drop indexes
    op.drop_index('ix_transactions_status')
    op.drop_index('ix_transactions_created_at')
    op.drop_index('ix_transactions_to_account')
    op.drop_index('ix_transactions_from_account')
    op.drop_index('ix_accounts_created_at')

    # Drop tables
    op.drop_table('transactions')
    op.drop_table('accounts')
