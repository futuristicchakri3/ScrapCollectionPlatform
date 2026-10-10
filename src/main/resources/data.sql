-- =============================================================================
-- SCRAP CONNECTION — MySQL INITIAL SEED DATA
-- =============================================================================

USE scrap_connection;

-- Standard Material Categories
INSERT IGNORE INTO material_categories (id, code, display_name, description, icon_name, active) VALUES
(1, 'PAPER', 'Paper & Books', 'Newspapers, Books, Magazines, Office Paper', 'newspaper', TRUE),
(2, 'CARDBOARD', 'Cardboard & Cartons', 'Corrugated Cardboard, Packaging Boxes', 'box', TRUE),
(3, 'PLASTIC', 'Plastic Materials', 'PET Bottles, Hard Plastic, HDPE Containers', 'bottle', TRUE),
(4, 'IRON', 'Iron & Steel Scrap', 'Iron Scrap, Steel Utensils, Rebar, Metal Rods', 'hammer', TRUE),
(5, 'ALUMINIUM', 'Aluminium Scrap', 'Aluminium Cans, Utensils, Frames, Foil', 'industry', TRUE),
(6, 'COPPER', 'Copper Wires & Pipes', 'Copper Wires, Pipes, Motors, Transformers', 'zap', TRUE),
(7, 'GLASS', 'Glass Bottles', 'Glass Bottles, Broken Glass, Jars', 'wine', TRUE),
(8, 'E_WASTE', 'Electronic Waste (E-Waste)', 'Electronic Scrap, PCBs, Old Phones, CPU Parts', 'cpu', TRUE);

-- Standard Rates per Kilogram
INSERT IGNORE INTO material_prices (id, category_id, price_per_kg, effective_from, active) VALUES
(1, 1, 14.00, NOW(), TRUE),
(2, 2, 10.00, NOW(), TRUE),
(3, 3, 16.00, NOW(), TRUE),
(4, 4, 28.00, NOW(), TRUE),
(5, 5, 110.00, NOW(), TRUE),
(6, 6, 450.00, NOW(), TRUE),
(7, 7, 2.50, NOW(), TRUE),
(8, 8, 35.00, NOW(), TRUE);

-- Initial Administrator Account (Password: Admin@1234)
INSERT IGNORE INTO users (id, mobile_number, password, role, status, created_at, updated_at) VALUES
(1, '9999999999', '$2a$10$Y5n2lE0u15R7gI6w9o8W0OMm8P1C4Z1P7N7x0a6K5c0p9Q5e3s4G2', 'ROLE_ADMIN', 'ACTIVE', NOW(), NOW());
