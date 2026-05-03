CREATE TABLE orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    total_price NUMERIC(10, 2) NOT NULL,

    city TEXT NOT NULL,
    street TEXT NOT NULL,
    house INTEGER NOT NULL,
    apartment INTEGER NOT NULL,

    items JSONB NOT NULL,

    status TEXT NOT NULL,
    created_by TEXT NOT NULL,

    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_created_by ON orders (created_by);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);
