<?php
session_start();
if (!isset($_SESSION['role']) || $_SESSION['role'] !== 'propietario') {
    header('Location: index.php?msg=' . urlencode('Acceso no autorizado. Ingresá como propietario.') . '&type=error&showLogin=1');
    exit;
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Panel Propietario</title>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
  body{font-family:Arial,Helvetica,sans-serif;margin:0;background:#f0f6f0;color:#222}
  header{background:#1565C0;color:#fff;padding:1rem}
  .wrap{max-width:900px;margin:1.2rem auto;padding:1rem;background:#fff;border-radius:8px;box-shadow:0 6px 18px rgba(0,0,0,0.06)}
  .btn-ghost{background:transparent;border:1px solid #1565C0;color:#1565C0;padding:0.5rem 0.8rem;border-radius:6px;cursor:pointer;text-decoration:none}
</style>
</head>
<body>
<header>
  <div style="max-width:900px;margin:0 auto;display:flex;justify-content:space-between;align-items:center">
    <div><strong>LaCalandria</strong> — Panel Propietario</div>
    <div>Usuario: <?php echo htmlentities($_SESSION['user']); ?></div>
  </div>
</header>

<div class="wrap">
  <h2>Bienvenido, Propietario</h2>
  <p>Aquí podrás consultar información relevante. (Implementá consultas/funcionalidad según necesidad.)</p>

  <p>
    <a href="logout.php" class="btn-ghost">Cerrar sesión</a>
  </p>
</div>
</body>
</html>
<?php
session_start();
if (!isset($_SESSION['role']) || $_SESSION['role'] !== 'inquilino') {
    header('Location: index.php?msg=' . urlencode('Acceso no autorizado. Ingresá como inquilino.') . '&type=error&showLogin=1');
    exit;
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Panel Inquilino</title>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
  body{font-family:Arial,Helvetica,sans-serif;margin:0;background:#f0f6f0;color:#222}
  header{background:#4A148C;color:#fff;padding:1rem}
  .wrap{max-width:900px;margin:1.2rem auto;padding:1rem;background:#fff;border-radius:8px;box-shadow:0 6px 18px rgba(0,0,0,0.06)}
  .btn-ghost{background:transparent;border:1px solid #4A148C;color:#4A148C;padding:0.5rem 0.8rem;border-radius:6px;cursor:pointer;text-decoration:none}
</style>
</head>
<body>
<header>
  <div style="max-width:900px;margin:0 auto;display:flex;justify-content:space-between;align-items:center">
    <div><strong>LaCalandria</strong> — Panel Inquilino</div>
    <div>Usuario: <?php echo htmlentities($_SESSION['user']); ?></div>
  </div>
</header>

<div class="wrap">
  <h2>Bienvenido, Inquilino</h2>
  <p>Página de inquilino. (Implementá funcionalidades específicas según requisitos.)</p>

  <p>
    <a href="logout.php" class="btn-ghost">Cerrar sesión</a>
  </p>
</div>
</body>
</html>

