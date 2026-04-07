-- Create the database if it doesn't exist
-- Note: Each user should run this locally
-- CREATE DATABASE IF NOT EXISTS algocore_db;
-- USE algocore_db;

-- Table for Game 1 (Minimum Cost Problem) Results
CREATE TABLE IF NOT EXISTS min_cost_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(255) NOT NULL,
    matrix_size INT NOT NULL,
    total_cost DOUBLE NOT NULL,
    algorithm_name VARCHAR(50) NOT NULL,
    execution_time_ms BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Future tables for other games can be added here
-- Table for Snake and Ladder
-- CREATE TABLE IF NOT EXISTS snake_ladder_results (...);
