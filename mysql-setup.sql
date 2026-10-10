-- =============================================================================
-- SCRAP CONNECTION — MYSQL COMMAND LINE SETUP SCRIPT
-- =============================================================================
-- Execute this script directly in the MySQL Command Line Client:
-- mysql -u root -p < mysql-setup.sql
-- or run the commands step-by-step in the MySQL prompt.

-- 1. Create the database
CREATE DATABASE IF NOT EXISTS scrap_connection
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Select the database
USE scrap_connection;

-- 3. (Optional) Create a dedicated MySQL application user
-- CREATE USER IF NOT EXISTS 'scrap_user'@'localhost' IDENTIFIED BY 'Scrap@123';
-- GRANT ALL PRIVILEGES ON scrap_connection.* TO 'scrap_user'@'localhost';
-- FLUSH PRIVILEGES;

-- 4. Verify Database Creation
SELECT 'Database scrap_connection successfully initialized!' AS Status;
SHOW TABLES;
