"""Add verified merchant logo and domain metadata columns.

Revision ID: d4e5f6a7b8c9
Revises: c3d4e5f6a7b8
Create Date: 2026-11-16 21:58:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

revision: str = 'd4e5f6a7b8c9'
down_revision: Union[str, Sequence[str], None] = 'c3d4e5f6a7b8'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Use inspect/conditional check or try/except to be idempotent across environments
    conn = op.get_bind()
    inspector = sa.inspect(conn)
    existing_columns = [col['name'] for col in inspector.get_columns('merchants')]

    if 'domain' not in existing_columns:
        op.add_column('merchants', sa.Column('domain', sa.String(length=255), nullable=True))
        op.create_index(op.f('ix_merchants_domain'), 'merchants', ['domain'], unique=False)
    if 'logo_url' not in existing_columns:
        op.add_column('merchants', sa.Column('logo_url', sa.String(length=500), nullable=True))
    if 'logo_source' not in existing_columns:
        op.add_column('merchants', sa.Column('logo_source', sa.String(length=100), nullable=True))
    if 'verification_status' not in existing_columns:
        op.add_column('merchants', sa.Column('verification_status', sa.String(length=50), server_default='unverified', nullable=False))


def downgrade() -> None:
    conn = op.get_bind()
    inspector = sa.inspect(conn)
    existing_columns = [col['name'] for col in inspector.get_columns('merchants')]

    if 'verification_status' in existing_columns:
        op.drop_column('merchants', 'verification_status')
    if 'logo_source' in existing_columns:
        op.drop_column('merchants', 'logo_source')
    if 'logo_url' in existing_columns:
        op.drop_column('merchants', 'logo_url')
    if 'domain' in existing_columns:
        op.drop_index(op.f('ix_merchants_domain'), table_name='merchants')
        op.drop_column('merchants', 'domain')
