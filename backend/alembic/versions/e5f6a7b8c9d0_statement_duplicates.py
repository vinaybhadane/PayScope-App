"""Keep statement membership when overlapping payments are deduplicated."""
from alembic import op
import sqlalchemy as sa

revision = "e5f6a7b8c9d0"
down_revision = "d4e5f6a7b8c9"
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        "statement_duplicates",
        sa.Column("import_id", sa.Uuid(), sa.ForeignKey("imports.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("transaction_id", sa.Uuid(), sa.ForeignKey("transactions.id", ondelete="CASCADE"), primary_key=True),
    )
    op.create_index("ix_statement_duplicates_transaction_id", "statement_duplicates", ["transaction_id"])


def downgrade():
    op.drop_table("statement_duplicates")
