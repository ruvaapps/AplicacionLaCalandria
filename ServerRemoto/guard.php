<?php
session_start();
// acceso solo guardia
if (!isset($_SESSION['role']) || $_SESSION['role'] !== 'guard') {
    header('Location: index.php?msg=' . urlencode('Acceso no autorizado. Ingresá como guardia.') . '&type=error&showLogin=1');
    exit;
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Registro de Control - Guardia</title>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
  body{font-family:Arial,Helvetica,sans-serif;margin:0;background:#f0f6f0;color:#222}
  header{background:#2E7D32;color:#fff;padding:1rem}
  .wrap{max-width:720px;margin:1.2rem auto;padding:1rem;background:#fff;border-radius:8px;box-shadow:0 6px 18px rgba(0,0,0,0.06)}
  label{display:block;margin-top:0.75rem;font-weight:600}
  select,input[type="radio"]{margin-top:0.4rem}
  .row{display:flex;gap:1rem;align-items:center;margin-top:0.5rem}
  .btn{background:#2E7D32;color:#fff;border:none;padding:0.6rem 0.9rem;border-radius:6px;cursor:pointer}
  .btn-ghost{background:transparent;border:1px solid #2E7D32;color:#2E7D32;padding:0.5rem 0.8rem;border-radius:6px}
  .msg{margin-top:0.8rem;padding:0.6rem;border-radius:6px}
  .msg.ok{background:#e7f8ee;color:#0b6623}
  .msg.err{background:#fff1f1;color:#b00020}
</style>
</head>
<body>
<header>
  <div style="max-width:720px;margin:0 auto;display:flex;justify-content:space-between;align-items:center">
    <div><strong>LaCalandria</strong> — Registro Guardia</div>
    <div>Usuario: <?php echo htmlentities($_SESSION['user']); ?></div>
  </div>
</header>

<div class="wrap">
  <h2>Registrar Control</h2>

  <form id="frmControl">
    <label for="tramo">Tramo</label>
    <select id="tramo" name="tramo" required>
      <option value="">-- Seleccionar Tramo --</option>
      <?php for($i=1;$i<=7;$i++): ?>
        <option value="<?php echo $i; ?>">Tramo <?php echo $i; ?></option>
      <?php endfor; ?>
    </select>

    <label style="margin-top:0.8rem;">Luz Electricidad Roja</label>
    <div class="row">
      <label><input type="radio" name="roja" value="1" required> Si</label>
      <label><input type="radio" name="roja" value="0"> No</label>
    </div>

    <label style="margin-top:0.8rem;">Luz Verde Titila</label>
    <div class="row">
      <label><input type="radio" name="verde" value="1" required> Si</label>
      <label><input type="radio" name="verde" value="0"> No</label>
    </div>

    <div style="margin-top:1rem;display:flex;gap:0.5rem">
      <button type="submit" class="btn">Guardar</button>
      <a href="logout.php" class="btn-ghost" style="align-self:center;">Cerrar sesión</a>
    </div>
  </form>

  <div id="result" aria-live="polite"></div>
</div>

<script>
(function(){
  const form = document.getElementById('frmControl');
  const result = document.getElementById('result');

  form.addEventListener('submit', function(e){
    e.preventDefault();
    result.innerHTML = '';
    const data = {
      tramo: form.tramo.value,
      roja_si: form.roja.value === '1' ? 1 : 0,
      verde_si: form.verde.value === '1' ? 1 : 0,
      // timestamp lo puede poner el servidor; enviamos el cliente por si hace falta
      timestamp: new Date().toISOString().slice(0,19).replace('T',' ')
    };
    if (!data.tramo) {
      result.innerHTML = '<div class="msg err">Seleccioná un tramo.</div>';
      return;
    }

    // Ajustá la URL si tu endpoint está en otra ruta
    fetch('save_control_cerco.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
      credentials: 'same-origin'
    })
    .then(resp => resp.json().catch(()=>({success:false,message:'Respuesta inválida'})))
    .then(json => {
      if (json && json.success) {
        result.innerHTML = '<div class="msg ok">Registro guardado correctamente.</div>';
        form.reset();
      } else {
        const msg = (json && json.message) ? json.message : 'Error al guardar.';
        result.innerHTML = '<div class="msg err">' + msg + '</div>';
      }
    })
    .catch(err => {
      result.innerHTML = '<div class="msg err">Error de red: ' + (err.message || '') + '</div>';
      console.error(err);
    });
  });
})();
</script>
</body>
</html>

