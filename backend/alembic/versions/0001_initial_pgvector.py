"""Initial EngBuddy schema with pgvector HNSW index

Revision ID: 0001_initial_pgvector
Revises: None
Create Date: 2026-11-06
"""
from __future__ import annotations

from alembic import op
import sqlalchemy as sa
from pgvector.sqlalchemy import Vector
from sqlalchemy.dialects import postgresql

revision = "0001_initial_pgvector"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")

    op.create_table(
        "users",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("email", sa.String(length=255), nullable=False, unique=True),
        sa.Column("display_name", sa.String(length=255), nullable=False),
        sa.Column("tier", sa.String(length=32), nullable=False, server_default="free"),
        sa.Column("encrypted_byo_gemini_key", sa.Text(), nullable=True),
        sa.Column("daily_ai_quota_used", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("wifi_only_sync", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )

    op.create_table(
        "subjects",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("code", sa.String(length=32), nullable=False),
        sa.Column("name", sa.String(length=255), nullable=False),
        sa.Column("target_attendance_pct", sa.Float(), nullable=False, server_default="75.0"),
        sa.Column("lab_weight_classes", sa.Integer(), nullable=False, server_default="2"),
        sa.Column("count_medical_leave_as_excused", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("rounding_mode", sa.String(length=16), nullable=False, server_default="NEAREST"),
        sa.Column("attended_classes", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("total_classes", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("medical_excused_classes", sa.Integer(), nullable=False, server_default="0"),
    )
    op.create_index("ix_subjects_user_code", "subjects", ["user_id", "code"], unique=True)

    op.create_table(
        "oauth_connections",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("provider", sa.String(length=32), nullable=False),
        sa.Column("encrypted_access_token", sa.Text(), nullable=False),
        sa.Column("encrypted_refresh_token", sa.Text(), nullable=True),
        sa.Column("scopes", sa.Text(), nullable=False),
        sa.Column("forwarding_address", sa.String(length=255), nullable=False),
        sa.Column("status", sa.String(length=32), nullable=False, server_default="active"),
        sa.Column("last_synced_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.create_index("ix_oauth_user_provider", "oauth_connections", ["user_id", "provider"], unique=True)

    op.create_table(
        "documents",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("subject_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("subjects.id", ondelete="SET NULL"), nullable=True),
        sa.Column("chapter_title", sa.String(length=255), nullable=False, server_default=""),
        sa.Column("title", sa.String(length=255), nullable=False),
        sa.Column("source_type", sa.String(length=32), nullable=False, server_default="CAMERA"),
        sa.Column("storage_key", sa.String(length=512), nullable=False),
        sa.Column("page_count", sa.Integer(), nullable=False, server_default="1"),
        sa.Column("ordering_confidence", sa.Float(), nullable=False, server_default="1.0"),
        sa.Column("status", sa.String(length=32), nullable=False, server_default="READY"),
        sa.Column("trashed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_documents_user_status", "documents", ["user_id", "status"])

    op.create_table(
        "note_chunks",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("document_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("documents.id", ondelete="CASCADE"), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("page_number", sa.Integer(), nullable=False),
        sa.Column("chunk_text", sa.Text(), nullable=False),
        sa.Column("embedding", Vector(768), nullable=False),
    )
    op.create_index("ix_note_chunks_user_doc", "note_chunks", ["user_id", "document_id"])
    op.execute(
        "CREATE INDEX IF NOT EXISTS ix_note_chunks_embedding_hnsw "
        "ON note_chunks USING hnsw (embedding vector_cosine_ops)"
    )


def downgrade() -> None:
    op.drop_table("note_chunks")
    op.drop_table("documents")
    op.drop_table("oauth_connections")
    op.drop_table("subjects")
    op.drop_table("users")
