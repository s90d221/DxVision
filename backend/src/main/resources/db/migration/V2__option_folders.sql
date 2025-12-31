-- Documentation-only migration for option folder grouping
CREATE TABLE IF NOT EXISTS option_folders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    name VARCHAR(200) NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE findings ADD COLUMN IF NOT EXISTS folder_id BIGINT NULL;
ALTER TABLE findings ADD COLUMN IF NOT EXISTS order_index INT NOT NULL DEFAULT 0;
ALTER TABLE findings ADD CONSTRAINT fk_findings_folder FOREIGN KEY (folder_id) REFERENCES option_folders(id);

ALTER TABLE diagnoses ADD COLUMN IF NOT EXISTS folder_id BIGINT NULL;
ALTER TABLE diagnoses ADD COLUMN IF NOT EXISTS order_index INT NOT NULL DEFAULT 0;
ALTER TABLE diagnoses ADD CONSTRAINT fk_diagnoses_folder FOREIGN KEY (folder_id) REFERENCES option_folders(id);
