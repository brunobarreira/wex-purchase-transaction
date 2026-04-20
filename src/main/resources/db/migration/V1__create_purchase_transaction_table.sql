CREATE TABLE purchase_transaction
(
    id               UUID          NOT NULL,
    description      VARCHAR(50)   NOT NULL,
    transaction_date DATE          NOT NULL,
    amount_usd       DECIMAL(19, 2) NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_purchase_transaction PRIMARY KEY (id)
);
