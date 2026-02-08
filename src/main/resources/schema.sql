CREATE TABLE IF NOT EXISTS trainer_workload (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    is_active BOOLEAN,
    training_year INT NOT NULL,
    training_month INT NOT NULL,
    duration_min INT DEFAULT 0,
    CONSTRAINT unique_trainer_month UNIQUE (username, training_year, training_month),
    CONSTRAINT positive_duration CHECK(duration_min >= 0)
);

CREATE INDEX idx_trainer_lookup ON trainer_workload (username, training_year, training_month);