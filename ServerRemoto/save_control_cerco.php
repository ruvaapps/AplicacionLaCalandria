<?php
header('Content-Type: application/json; charset=utf-8');
// Opcional: permitir llamadas desde clientes locales (ajustar en producción)
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    // preflight
    http_response_code(200);
    exit;
}

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) {
    echo json_encode(['success' => false, 'message' => 'JSON inválido o body vacío']);
    exit;
}

$tramo = isset($data['tramo']) ? trim($data['tramo']) : '';
$roja_si = !empty($data['roja_si']) ? 1 : 0;
$roja_no = !empty($data['roja_no']) ? 1 : 0;
$verde_si = !empty($data['verde_si']) ? 1 : 0;
$verde_no = !empty($data['verde_no']) ? 1 : 0;
$timestamp = isset($data['timestamp']) && !empty($data['timestamp']) ? $data['timestamp'] : date('Y-m-d H:i:s');

// Validaciones mínimas
if ($tramo === '') {
    echo json_encode(['success' => false, 'message' => 'Campo tramo requerido']);
    exit;
}

/*
 * CONFIGURA ESTAS VARIABLES CON TU SERVIDOR
 */
define('DB_HOST', 'TU_HOST');
define('DB_NAME', 'TU_DB');
define('DB_USER', 'TU_USER');
define('DB_PASS', 'TU_PASS');

$dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4";

try {
    $pdo = new PDO($dsn, DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);

    $sql = "INSERT INTO control_cerco (tramo, roja_si, roja_no, verde_si, verde_no, timestamp)
            VALUES (:tramo, :roja_si, :roja_no, :verde_si, :verde_no, :timestamp)";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([
        ':tramo' => $tramo,
        ':roja_si' => $roja_si,
        ':roja_no' => $roja_no,
        ':verde_si' => $verde_si,
        ':verde_no' => $verde_no,
        ':timestamp' => $timestamp,
    ]);

    echo json_encode(['success' => true, 'message' => 'Guardado']);
} catch (PDOException $e) {
    // No exponer mensajes sensibles en producción
    echo json_encode(['success' => false, 'message' => 'Error BD: ' . $e->getMessage()]);
}

