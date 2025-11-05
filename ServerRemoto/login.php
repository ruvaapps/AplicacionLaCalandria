<?php
// Handler de login con logging para depuración (no imprime HTML)

// Colocar buffering al inicio para evitar "headers already sent"
ob_start();
session_start(); // <--- iniciar sesión para persistir rol

ini_set('display_errors', '0');
ini_set('log_errors', '1');

$logDir = __DIR__ . '/logs';
$logFile = $logDir . '/login_debug.log';

// Asegurar existencia de carpeta logs
if (!is_dir($logDir)) {
    @mkdir($logDir, 0755, true);
}

function dbg($msg) {
    global $logFile;
    $ip = $_SERVER['REMOTE_ADDR'] ?? 'cli';
    $ua = $_SERVER['HTTP_USER_AGENT'] ?? '';
    $req = ($_SERVER['REQUEST_METHOD'] ?? '') . ' ' . ($_SERVER['REQUEST_URI'] ?? '');
    $line = sprintf("[%s] %s %s - %s - %s%s", date('Y-m-d H:i:s'), $ip, $req, $ua, $msg, PHP_EOL);
    // log al error_log del servidor y también al archivo local
    error_log($line);
    @file_put_contents($logFile, $line, FILE_APPEND | LOCK_EX);
}

try {
    dbg("Acceso a login.php");

    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        dbg("Acceso no POST, redirigiendo a index.php");
        header('Location: index.php');
        exit;
    }

    // Leer POST de forma segura
    $user = isset($_POST['user']) ? trim((string)$_POST['user']) : '';
    $pass = isset($_POST['pass']) ? trim((string)$_POST['pass']) : '';
    $role = isset($_POST['role']) ? trim((string)$_POST['role']) : '';

    // Normalizar a minúsculas para comparación case-insensitive
    $user_l = mb_strtolower($user, 'UTF-8');
    $role_l = mb_strtolower($role, 'UTF-8');

    // Registrar intento (no guardar la contraseña plana; sólo longitud)
    dbg("Intento de login - raw user=\"{$user}\" role=\"{$role}\" pass_len=" . strlen($pass) . " | normalized user_l=\"{$user_l}\" role_l=\"{$role_l}\"");

    // Validaciones mínimas
    if ($user === '' || $pass === '' || $role === '') {
        dbg("Error: campos vacíos o perfil no seleccionado");
        $msg = urlencode('Usuario, contraseña o perfil inválidos.');
        header("Location: index.php?msg={$msg}&type=error&showLogin=1");
        exit;
    }

    // Credenciales demo por perfil (comparación case-insensitive)
    $ok = false;

    if ($role_l === 'admin' && $user_l === 'admin' && $pass === '1234') {
        $ok = true;
        $_SESSION['user'] = 'admin';
        $_SESSION['role'] = 'admin';
        dbg("Login correcto: admin");
        header("Location: ./admin.php");
        ob_end_flush();
        exit;
    }

    if ($role_l === 'propietario' && $user_l === 'propietario' && $pass === '1234') {
        $ok = true;
        $_SESSION['user'] = $user;
        $_SESSION['role'] = 'propietario';
        dbg("Login correcto: propietario");
        header("Location: ./propietario.php");
        ob_end_flush();
        exit;
    }

    if ($role_l === 'inquilino' && $user_l === 'inquilino' && $pass === '1234') {
        $ok = true;
        $_SESSION['user'] = $user;
        $_SESSION['role'] = 'inquilino';
        dbg("Login correcto: inquilino");
        header("Location: ./inquilino.php");
        ob_end_flush();
        exit;
    }

    if ($role_l === 'guard' && $user_l === 'guard' && $pass === '1234') {
        $ok = true;
        $_SESSION['user'] = $user;
        $_SESSION['role'] = 'guard';
        dbg("Login correcto: guard");
        header("Location: ./guard.php");
        ob_end_flush();
        exit;
    }

    // Alternativa útil para demo: permitir que cualquier username pueda acceder si selecciona el rol correcto
    // y usa la contraseña demo '1234' (descomentar si querés esta flexibilidad):
    /*
    if ($pass === '1234' && in_array($role_l, ['propietario','inquilino','guard'])) {
        $ok = true;
        $_SESSION['user'] = $user;
        $_SESSION['role'] = $role_l;
        dbg("Login demo flexible aceptado para role={$role_l} user={$user}");
        // redirigir según role
        if ($role_l === 'propietario') header("Location: ./propietario.php");
        elseif ($role_l === 'inquilino') header("Location: ./inquilino.php");
        else header("Location: ./guard.php");
        ob_end_flush();
        exit;
    }
    */

    if (! $ok) {
        dbg("Login fallido para user=\"" . $user . "\" role=\"" . $role . "\" (normalizados: {$user_l}/{$role_l})");
        $msg = urlencode('Usuario, contraseña o perfil incorrectos.');
        header("Location: index.php?msg={$msg}&type=error&showLogin=1");
        ob_end_flush();
        exit;
    }
} catch (Throwable $e) {
    // Capturar cualquier excepción/error fatal y loguear
    $err = "Exception: " . $e->getMessage() . " in " . $e->getFile() . ":" . $e->getLine();
    dbg($err);
    // Responder con mensaje genérico al usuario y reabrir modal de login
    $msg = urlencode('Error interno. Revisá los logs en el servidor.');
    header("Location: index.php?msg={$msg}&type=error&showLogin=1");
    exit;
}
