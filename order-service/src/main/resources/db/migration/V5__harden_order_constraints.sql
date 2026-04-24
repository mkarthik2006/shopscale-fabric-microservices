DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_orders_total_amount_positive'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT chk_orders_total_amount_positive
            CHECK (total_amount > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_orders_currency_iso'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT chk_orders_currency_iso
            CHECK (currency ~ '^[A-Z]{3}$');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_orders_user_email_format'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT chk_orders_user_email_format
            CHECK (user_email ~* '^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_order_items_sku_not_blank'
    ) THEN
        ALTER TABLE order_items
            ADD CONSTRAINT chk_order_items_sku_not_blank
            CHECK (length(btrim(sku)) > 0);
    END IF;
END $$;
