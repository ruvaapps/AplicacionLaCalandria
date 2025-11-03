<?php
header('Content-Type: application/json; charset=utf-8');
// Permitir CORS para pruebas (ajustar en producción)
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

/*
 * CONFIGURA ESTAS VARIABLES CON TU SERVIDOR
 */
define('DB_HOST', 'localhost');
define('DB_NAME', 'admlova_bebidasonline');
define('DB_USER', 'admlova_guardiaCalandria');
define('DB_PASS', 'guardiaCalandria');

$dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4";

try {
    $pdo = new PDO($dsn, DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);

    // Obtener todas las alarmas activas (puedes quitar WHERE active = 1 si quieres todas)
    $sql = "SELECT id, alarm_hour, alarm_min, active, label FROM alarm_config WHERE active = 1 ORDER BY id ASC";
    $stmt = $pdo->query($sql);
    $rows = $stmt->fetchAll();

    $alarms = [];
    if ($rows) {
        foreach ($rows as $row) {
            $hour = isset($row['alarm_hour']) ? (int)$row['alarm_hour'] : 9;
            $min  = isset($row['alarm_min'])  ? (int)$row['alarm_min']  : 0;
            if ($hour < 0 || $hour > 23) $hour = 9;
            if ($min < 0 || $min > 59) $min = 0;

            $alarms[] = [
                'id' => isset($row['id']) ? (int)$row['id'] : null,
                'alarm_hour' => $hour,
                'alarm_min' => $min,
                'active' => isset($row['active']) ? (int)$row['active'] : 0,
                'label' => isset($row['label']) ? $row['label'] : null
            ];
        }
    }

    echo json_encode([
        'success' => true,
        'message' => 'Alarms fetched',
        'count' => count($alarms),
        'alarms' => $alarms
    ]);
} catch (PDOException $e) {
    // No exponer errores sensibles en producción
    echo json_encode([
        'success' => false,
        'message' => 'DB error: ' . $e->getMessage()
    ]);
}

