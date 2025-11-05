<?php
session_start();
// acceso restringido
if (!isset($_SESSION['role']) || $_SESSION['role'] !== 'admin') {
    // no está logueado como admin
    header('Location: index.php?msg=' . urlencode('Acceso no autorizado. Ingresá como admin.') . '&type=error&showLogin=1');
    exit;
}

// Configuración DB - reemplazar por tus credenciales reales
define('DB_HOST','localhost');
define('DB_NAME','tu_base_de_datos');
define('DB_USER','tu_usuario');
define('DB_PASS','tu_contraseña');

$fechaFiltro = isset($_GET['date']) && $_GET['date'] !== '' ? $_GET['date'] : null;
$controls = [];
$errMsg = null;

try {
    $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4";
    $pdo = new PDO($dsn, DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);

    if ($fechaFiltro) {
        // validar formato YYYY-MM-DD (simple)
        if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $fechaFiltro)) {
            throw new Exception('Formato de fecha inválido');
        }
        $stmt = $pdo->prepare("SELECT * FROM control_cerco WHERE DATE(timestamp) = :fecha ORDER BY timestamp DESC");
        $stmt->execute([':fecha' => $fechaFiltro]);
    } else {
        $stmt = $pdo->query("SELECT * FROM control_cerco ORDER BY timestamp DESC LIMIT 500");
    }

    $controls = $stmt->fetchAll();
} catch (Throwable $e) {
    // log simple
    @file_put_contents(__DIR__ . '/logs/admin_debug.log', date('Y-m-d H:i:s') . " - " . $e->getMessage() . PHP_EOL, FILE_APPEND | LOCK_EX);
    $errMsg = 'Error al consultar la base de datos. Revisá logs del servidor.';
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Panel Admin - Controles</title>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
    body{font-family:Arial,Helvetica,sans-serif;margin:0;background:#f0f6f0;color:#222}
    header{background:#2E7D32;color:#fff;padding:1rem}
    .container{max-width:1100px;margin:1.2rem auto;padding:1rem;background:#fff;border-radius:8px;box-shadow:0 6px 18px rgba(0,0,0,0.06)}
    .topbar{display:flex;align-items:center;justify-content:space-between;margin-bottom:1rem}
    .filters{display:flex;gap:0.5rem;align-items:center}
    input[type="date"]{padding:0.45rem;border:1px solid #ccc;border-radius:6px}
    button{background:#2E7D32;color:#fff;border:none;padding:0.6rem 0.9rem;border-radius:6px;cursor:pointer}
    table{width:100%;border-collapse:collapse;margin-top:1rem}
    th,td{padding:0.6rem;border:1px solid #e6e6e6;text-align:left;font-size:0.95rem}
    th{background:#f5f5f5}
    .small{font-size:0.85rem;color:#555}
    .err{background:#fdecea;color:#b00020;padding:0.6rem;border-radius:6px}
    .actions a{margin-left:0.5rem;color:#2E7D32;text-decoration:none}
</style>
</head>
<body>
<header>
    <div style="max-width:1100px;margin:0 auto;display:flex;justify-content:space-between;align-items:center">
        <div><strong>LaCalandria</strong> — Panel Administrador</div>
        <div class="small">Usuario: <?php echo htmlentities($_SESSION['user'] ?? 'admin'); ?></div>
    </div>
</header>

<div class="container">
    <div class="topbar">
        <h2>Controles registrados</h2>
        <div class="actions">
            <a href="logout.php">Cerrar sesión</a>
        </div>
    </div>

    <form method="get" action="admin.php" class="filters" aria-label="Filtros">
        <label for="date">Filtrar por fecha:</label>
        <input type="date" id="date" name="date" value="<?php echo $fechaFiltro ? htmlentities($fechaFiltro) : ''; ?>">
        <button type="submit">Consultar</button>
        <a href="admin.php" style="margin-left:0.5rem;color:#666;text-decoration:none">Ver todos</a>
    </form>

    <?php if ($errMsg): ?>
        <div class="err" role="alert"><?php echo htmlentities($errMsg); ?></div>
    <?php endif; ?>

    <div class="small" style="margin-top:0.75rem">Mostrando <?php echo count($controls); ?> registros<?php if ($fechaFiltro) echo " para " . htmlentities($fechaFiltro); ?>.</div>

    <?php if (count($controls) === 0): ?>
        <p style="margin-top:1rem">No hay controles para mostrar.</p>
    <?php else: ?>
        <table role="table" aria-label="Lista de controles">
            <thead>
                <tr>
                    <?php
                    // generar cabeceras dinámicas desde la primera fila
                    $first = $controls[0];
                    foreach (array_keys($first) as $col) {
                        echo "<th>" . htmlentities($col) . "</th>";
                    }
                    ?>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($controls as $row): ?>
                    <tr>
                        <?php foreach ($row as $val): ?>
                            <td><?php echo htmlentities((string)$val); ?></td>
                        <?php endforeach; ?>
                    </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    <?php endif; ?>
</div>
</body>
</html>
<?php
session_start();
$_SESSION = [];
if (ini_get("session.use_cookies")) {
    $params = session_get_cookie_params();
    setcookie(session_name(), '', time() - 42000,
        $params["path"], $params["domain"],
        $params["secure"], $params["httponly"]
    );
}
session_destroy();
header('Location: index.php?msg=' . urlencode('Sesión cerrada.') . '&type=success');
exit;

