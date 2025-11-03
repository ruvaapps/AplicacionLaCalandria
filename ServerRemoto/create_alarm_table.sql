CREATE TABLE IF NOT EXISTS alarm_config (
  id INT AUTO_INCREMENT PRIMARY KEY,
  alarm_hour TINYINT UNSIGNED NOT NULL DEFAULT 9,
  alarm_min  TINYINT UNSIGNED NOT NULL DEFAULT 0,
  active     TINYINT(1) NOT NULL DEFAULT 1,
  label      VARCHAR(100) DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Fila por defecto: alarma a las 09:00 activa
INSERT INTO alarm_config (alarm_hour, alarm_min, active, label)
VALUES (9, 0, 1, 'Alarma diaria control cercos');

