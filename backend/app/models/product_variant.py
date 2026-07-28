import uuid
from datetime import datetime
from sqlalchemy import String, DateTime, ForeignKey, Numeric, Boolean
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base
from app.models.tenant import get_utc_now


class ProductVariant(Base):
    """
    Representa uma variação específica de um produto base (ex: Camiseta M, Azul).
    Exclusivo para o setor retail_apparel, controla estoque e SKU independente do produto.
    """
    __tablename__ = "product_variants"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    product_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("products.id", ondelete="CASCADE"), 
        nullable=False
    )
    sku: Mapped[str] = mapped_column(String(100), unique=True, index=True, nullable=False)
    color: Mapped[str | None] = mapped_column(String(50), nullable=True)
    size: Mapped[str | None] = mapped_column(String(50), nullable=True)
    
    # Se None, herda o preço do produto base
    price_override: Mapped[float | None] = mapped_column(
        Numeric(10, 2, asdecimal=False), 
        nullable=True
    )
    
    # Estoque é gerenciado na variante para o retail_apparel
    current_stock: Mapped[float] = mapped_column(
        Numeric(12, 4, asdecimal=False), 
        default=0.0
    )
    minimum_stock: Mapped[float] = mapped_column(
        Numeric(12, 4, asdecimal=False), 
        default=0.0
    )
    
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), 
        default=get_utc_now
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), 
        default=get_utc_now, 
        onupdate=get_utc_now
    )

    product = relationship("Product", back_populates="variants")
