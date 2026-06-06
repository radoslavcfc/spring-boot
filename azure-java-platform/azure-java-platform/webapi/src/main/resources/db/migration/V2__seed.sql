-- One-time seed. Flyway records this in flyway_schema_history; it will never run twice.
INSERT INTO products (sku, name, price_cents) VALUES
 ('SKU-001', 'Sample Widget',     1999),
 ('SKU-002', 'Sample Gadget',     2999),
 ('SKU-003', 'Sample Thingamajig', 4999);
