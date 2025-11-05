<?php
session_start();

// acceso restringido
if (!isset($_SESSION['role']) || $_SESSION['role'] !== 'admin') {
    header('Location: index.php?msg=' . urlencode('Acceso no autorizado. Ingresá como admin.') . '&type=error&showLogin=1');
    exit;
}

// Configuración DB - reemplazar por tus credenciales reales
define('DB_HOST','localhost');
define('DB_NAME','admlova_bebidasonline');
define('DB_USER','admlova_guardiaCalandria');
define('DB_PASS','guardiaCalandria');

$fechaFiltro = isset($_GET['date']) && $_GET['date'] !== '' ? $_GET['date'] : null;
$action = isset($_GET['action']) ? $_GET['action'] : null; // nuevo: action=alarms
$controls = [];
$alarms = []; // nuevo: resultado de alarmas
$errMsg = null;
$alarmsErr = null;

$logDir = __DIR__ . '/logs';
$logFile = $logDir . '/admin_debug.log';
if (!is_dir($logDir)) {
    @mkdir($logDir, 0755, true);
}

function admin_log($msg) {
    global $logFile;
    $line = sprintf("[%s] %s%s", date('Y-m-d H:i:s'), $msg, PHP_EOL);
    @file_put_contents($logFile, $line, FILE_APPEND | LOCK_EX);
}

// helper para formatear fecha a dd/MM/yyyy HH:mm
function format_ts($ts) {
    if (!$ts) return '';
    $t = strtotime($ts);
    if ($t === false) return htmlentities($ts);
    return date('d/m/Y H:i', $t);
}

try {
    $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4";
    $pdo = new PDO($dsn, DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);

    // Si se solicitó action=alarms, obtener alarmas configuradas y NO cargar controles
    if ($action === 'alarms') {
        try {
            // Ajustá el nombre de la tabla si difiere (alarm_config, alarms, scheduled_alarms, etc.)
            $stmt2 = $pdo->query("SELECT id, alarm_hour, alarm_min, label, active FROM alarm_config ORDER BY id ASC");
            $alarms = $stmt2->fetchAll();
        } catch (Throwable $e) {
            admin_log("Alarm query error: " . $e->getMessage());
            $alarmsErr = 'Error al consultar las alarmas configuradas.';
        }
    } else {
        // consultar solo las columnas requeridas: tramo, roja_si, verde_si, timestamp (solo cuando no pedimos alarmas)
        if ($fechaFiltro) {
            // validar formato YYYY-MM-DD
            if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $fechaFiltro)) {
                throw new Exception('Formato de fecha inválido');
            }
            $stmt = $pdo->prepare("SELECT tramo, roja_si, verde_si, `timestamp` FROM control_cerco WHERE DATE(`timestamp`) = :fecha ORDER BY `timestamp` DESC");
            $stmt->execute([':fecha' => $fechaFiltro]);
        } else {
            $stmt = $pdo->query("SELECT tramo, roja_si, verde_si, `timestamp` FROM control_cerco ORDER BY `timestamp` DESC LIMIT 500");
        }
        $controls = $stmt->fetchAll();
    }

} catch (Throwable $e) {
    admin_log("DB error: " . $e->getMessage());
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
    .filters{display:flex;gap:0.5rem;align-items:center;flex-wrap:wrap}
    input[type="date"]{padding:0.45rem;border:1px solid #ccc;border-radius:6px}
    button{background:#2E7D32;color:#fff;border:none;padding:0.6rem 0.9rem;border-radius:6px;cursor:pointer}
    .btn-ghost{background:transparent;color:#2E7D32;border:1px solid #2E7D32;padding:0.5rem 0.8rem;border-radius:6px;cursor:pointer}
    table{width:100%;border-collapse:collapse;margin-top:1rem}
    th,td{padding:0.6rem;border:1px solid #e6e6e6;text-align:left;font-size:0.95rem}
    th{background:#f5f5f5}
    .small{font-size:0.85rem;color:#555}
    .err{background:#fdecea;color:#b00020;padding:0.6rem;border-radius:6px}
    .actions a{margin-left:0.5rem;color:#2E7D32;text-decoration:none}
    @media (max-width:700px){ .filters{flex-direction:column;align-items:flex-start} table{font-size:0.85rem} }
    .cell-ok { background: #e7f8ee; } /* verde claro */
    .cell-bad { background: #fff1f1; } /* rojo claro */
    .alarms-box { margin-top:1.2rem; padding:0.8rem; border-radius:8px; background:#fafafa; border:1px solid #e9e9e9; }
    /* nuevos estilos para columna Activa */
    .alarm-active-yes { background: #e7f8ee; color: #0b6623; font-weight:600; text-align:center; }
    .alarm-active-no  { background: #f2f2f2; color: #666666; text-align:center; }
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
        <h2>
            <?php
            // Título dinámico según la vista
            if ($action === 'alarms') {
                echo "Alarmas configuradas";
            } else {
                echo "Controles registrados";
            }
            ?>
        </h2>
        <div class="actions">
            <a href="logout.php">Cerrar sesión</a>
        </div>
    </div>

    <form method="get" action="admin.php" class="filters" aria-label="Filtros">
        <label for="date">Filtrar por fecha:</label>
        <input type="date" id="date" name="date" value="<?php echo $fechaFiltro ? htmlentities($fechaFiltro) : ''; ?>">
        <button type="submit">Consultar</button>
        <a href="admin.php" style="margin-left:0.5rem;color:#666;text-decoration:none">Ver todos</a>

        <!-- botón para consultar alarmas configuradas (no mezcla controles) -->
        <a href="admin.php?action=alarms" class="btn-ghost" style="margin-left:1rem;">Consultar Alarmas</a>
    </form>

    <?php if ($errMsg): ?>
        <div class="err" role="alert"><?php echo htmlentities($errMsg); ?></div>
    <?php endif; ?>

    <?php if ($action === 'alarms'): ?>
        <!-- Mostrar solo la sección de alarmas -->
        <div class="small" style="margin-top:0.75rem">Mostrando <?php echo count($alarms); ?> alarmas configuradas.</div>

        <div class="alarms-box" aria-live="polite">
            <?php if ($alarmsErr): ?>
                <div class="err"><?php echo htmlentities($alarmsErr); ?></div>
            <?php elseif (count($alarms) === 0): ?>
                <p>No hay alarmas configuradas.</p>
            <?php else: ?>
                <table role="table" aria-label="Lista de alarmas">
                    <thead>
                        <tr>
                            <th>Hora</th>
                            <th>Minuto</th>
                            <th>Etiqueta</th>
                            <th>Activa</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php foreach ($alarms as $a):
                            $hour = isset($a['alarm_hour']) ? $a['alarm_hour'] : (isset($a['hour']) ? $a['hour'] : '');
                            $min  = isset($a['alarm_min'])  ? $a['alarm_min']  : (isset($a['min'])  ? $a['min']  : '');
                            $label= isset($a['label'])      ? $a['label']      : '';
                            $active = ((int)($a['active'] ?? 0)) ? true : false;
                            $activeLabel = $active ? 'Si' : 'No';
                            $activeClass = $active ? 'alarm-active-yes' : 'alarm-active-no';
                        ?>
                            <tr>
                                <td><?php echo htmlentities((string)$hour); ?></td>
                                <td><?php echo htmlentities((string)$min); ?></td>
                                <td><?php echo htmlentities((string)$label); ?></td>
                                <td class="<?php echo $activeClass; ?>"><?php echo $activeLabel; ?></td>
                            </tr>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            <?php endif; ?>
        </div>

    <?php else: ?>
        <!-- Mostrar solo la sección de controles -->
        <div class="small" style="margin-top:0.75rem">Mostrando <?php echo count($controls); ?> registros<?php if ($fechaFiltro) echo " para " . htmlentities($fechaFiltro); ?>.</div>

        <?php if (count($controls) === 0): ?>
            <p style="margin-top:1rem">No hay controles para mostrar.</p>
        <?php else: ?>
            <table role="table" aria-label="Lista de controles">
                <thead>
                    <tr>
                        <th>Tramo</th>
                        <th>Luz Roja</th>
                        <th>Luz Verde</th>
                        <th>Fecha</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach ($controls as $row):
                        // normalizar nombres posibles (según tu tabla)
                        $tramo = isset($row['tramo']) ? $row['tramo'] : (isset($row['Tramo']) ? $row['Tramo'] : '');
                        $roja = isset($row['roja_si']) ? $row['roja_si'] : (isset($row['roja']) ? $row['roja'] : (isset($row['roja_no']) ? !$row['roja_no'] : null));
                        $verde = isset($row['verde_si']) ? $row['verde_si'] : (isset($row['verde']) ? $row['verde'] : (isset($row['verde_no']) ? !$row['verde_no'] : null));
                        $ts = isset($row['timestamp']) ? $row['timestamp'] : (isset($row['fecha']) ? $row['fecha'] : (isset($row['time']) ? $row['time'] : ''));
                        // convertir a Si/No
                        $roja_label = ($roja === null) ? '' : ((int)$roja ? 'Si' : 'No');
                        $verde_label = ($verde === null) ? '' : ((int)$verde ? 'Si' : 'No');
                        $roja_class = ((int)$roja) ? 'cell-ok' : 'cell-bad';
                        $verde_class = ((int)$verde) ? 'cell-ok' : 'cell-bad';
                    ?>
                        <tr>
                            <td><?php echo htmlentities((string)$tramo); ?></td>
                            <td class="<?php echo $roja_class; ?>"><?php echo htmlentities($roja_label); ?></td>
                            <td class="<?php echo $verde_class; ?>"><?php echo htmlentities($verde_label); ?></td>
                            <td><?php echo htmlentities(format_ts($ts)); ?></td>
                        </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        <?php endif; ?>
    <?php endif; ?>

</div>
</body>
</html>
