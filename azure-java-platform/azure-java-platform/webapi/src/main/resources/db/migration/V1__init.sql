CREATE TABLE products (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    sku         NVARCHAR(64)  NOT NULL UNIQUE,
    name        NVARCHAR(200) NOT NULL,
    price_cents INT           NOT NULL,
    created_at  DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE jobs (
    id             UNIQUEIDENTIFIER PRIMARY KEY,
    status         NVARCHAR(16) NOT NULL,
    result         NVARCHAR(4000) NULL,
    error_message  NVARCHAR(4000) NULL,
    created_at     DATETIME2 NOT NULL,
    completed_at   DATETIME2 NULL
);
