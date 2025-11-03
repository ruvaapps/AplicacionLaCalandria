<?php
// get_controls.php
// Devuelve JSON con la lista de registros de controles del cerco.
// Ahora acepta parámetro opcional GET "date" con formato yyyy-MM-dd para filtrar por día.

// Configuración CORS básica (ajustar en producción)
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Ajustá estos valores con las credenciales reales de tu base de datos
define('DB_HOST', 'localhost');
define('DB_NAME', 'admlova_bebidasonline');
define('DB_USER', 'admlova_guardiaCalandria');
define('DB_PASS', 'guardiaCalandria');

try {
    // Obtener parámetro date opcional (yyyy-MM-dd)
    $date = null;
    if (isset($_GET['date']) && is_string($_GET['date'])) {
        $candidate = trim($_GET['date']);
        // Validación simple de formato YYYY-MM-DD
        if (preg_match('/^\d{4}-\d{2}-\d{2}$/', $candidate)) {
            // Intentar normalizar y validar la fecha
            $dt = DateTime::createFromFormat('Y-m-d', $candidate);
            if ($dt && $dt->format('Y-m-d') === $candidate) {
                $date = $candidate;
            }
        }
        // si la validación falla, ignoramos el parámetro (no error) o podríamos devolver error
    }

    $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4";
    $pdo = new PDO($dsn, DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);

    if ($date !== null) {
        // Filtrar por día (date en formato yyyy-mm-dd)
        $sql = "SELECT id, tramo, roja_si, roja_no, verde_si, verde_no, `timestamp`
                FROM control_cerco
                WHERE DATE(`timestamp`) = :date
                ORDER BY `timestamp` DESC
                LIMIT 500";
        $stmt = $pdo->prepare($sql);
        $stmt->bindValue(':date', $date, PDO::PARAM_STR);
        $stmt->execute();
    } else {
        // Sin filtro por fecha: devolver todo (hasta límite)
        $sql = "SELECT id, tramo, roja_si, roja_no, verde_si, verde_no, `timestamp`
                FROM control_cerco
                ORDER BY `timestamp` DESC
                LIMIT 500";
        $stmt = $pdo->query($sql);
    }

    $rows = $stmt->fetchAll();

    // Normalizar/asegurar tipos si hace falta
    $data = [];
    foreach ($rows as $r) {
        $data[] = [
            'id' => isset($r['id']) ? (int)$r['id'] : null,
            'tramo' => isset($r['tramo']) ? $r['tramo'] : null,
            'roja_si' => isset($r['roja_si']) ? (int)$r['roja_si'] : 0,
            'roja_no' => isset($r['roja_no']) ? (int)$r['roja_no'] : 0,
            'verde_si' => isset($r['verde_si']) ? (int)$r['verde_si'] : 0,
            'verde_no' => isset($r['verde_no']) ? (int)$r['verde_no'] : 0,
            'timestamp' => isset($r['timestamp']) ? $r['timestamp'] : null,
        ];
    }

    echo json_encode([
        'success' => true,
        'count' => count($data),
        'data' => $data
    ], JSON_UNESCAPED_UNICODE);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'DB error: ' . $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
    exit;
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
    exit;
}
