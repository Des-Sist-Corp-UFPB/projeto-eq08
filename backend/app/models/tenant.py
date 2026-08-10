import uuid
from datetime import datetime, timezone
from sqlalchemy import String, DateTime
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base


def get_utc_now():
    return datetime.now(timezone.utc)


class Tenant(Base):
    __tablename__ = "tenants"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    slug: Mapped[str] = mapped_column(String(255), unique=True, nullable=False, index=True)
    status: Mapped[str] = mapped_column(String(50), default="active")
    # sector_type define o domínio de negócio do tenant.
    # Valores válidos: "food_service" | "retail_apparel" | "generic"
    # Validação de enum é feita via Pydantic (schema), não a nível de DDL,
    # para facilitar a adição de novos setores sem migrations de schema.
    sector_type: Mapped[str] = mapped_column(String(50), nullable=False, default="generic")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), 
        default=get_utc_now
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), 
        default=get_utc_now, 
        onupdate=get_utc_now
    )

    users = relationship("User", back_populates="tenant", cascade="all, delete-orphan")
    audit_logs = relationship("AuditLog", back_populates="tenant", cascade="all, delete-orphan")
