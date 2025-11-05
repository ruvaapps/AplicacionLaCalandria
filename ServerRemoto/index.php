<?php
// index.php
// leer mensajes desde query params
$msg = isset($_GET['msg']) ? urldecode($_GET['msg']) : '';
$type = isset($_GET['type']) ? $_GET['type'] : ''; // success|error
$showLogin = isset($_GET['showLogin']) && $_GET['showLogin'] == '1';
$role = isset($_GET['role']) ? $_GET['role'] : '';
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>LaCalandria - Inicio</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body { font-family: Arial, sans-serif; background: #f8f8f8; margin: 0; }
        header { background: #4CAF50; color: white; padding: 1em 0; text-align: center; }
        nav { background: #333; }
        nav ul { list-style: none; margin: 0; padding: 0; display: flex; justify-content: center; flex-wrap:wrap; }
        nav li { margin: 0 0.5em; }
        nav a { color: white; text-decoration: none; padding: 0.5em 1em; display: block; }
        nav a:hover { background: #4CAF50; }
        main { padding: 2em; text-align: center; }
        footer { background: #333; color: white; text-align: center; padding: 1em 0; position: fixed; width: 100%; bottom: 0; }
        @media (max-width:600px){ nav ul{flex-direction:column;align-items:center;} nav li{margin:0.25em 0;} main{padding:1em;} }

        /* estilos para modal de login integrado */
        .login-modal { display:none; position:fixed; inset:0; background:rgba(0,0,0,0.45); align-items:center; justify-content:center; z-index:1000; }
        .login-card { background:#fff; padding:1.5rem; border-radius:10px; width:320px; box-shadow:0 6px 18px rgba(0,0,0,0.15); }
        .login-close { position:absolute; top:12px; right:16px; font-size:18px; color:#666; cursor:pointer; }
        .msg { margin-bottom:0.5rem; padding:0.6rem; border-radius:4px; text-align:center; }
        .msg.success { background:#e6f4ea; color:#1b5e20; }
        .msg.error { background:#fdecea; color:#b00020; }
    </style>
</head>
<body>
    <header>
        <h1>Bienvenidos a LaCalandria</h1>
        <p>Tu Barrio</p>
    </header>
    <nav>
        <ul>
            <li><a href="index.php">Inicio</a></li>
            <li><a href="#">Sobre Nosotros</a></li>
            <li><a href="#">Servicios</a></li>
            <li><a href="#">Contacto</a></li>
            <!-- cambiado: abrir sección de login en la misma página -->
            <li><a href="#loginSection" id="navLogin">Ingresar</a></li>
        </ul>
    </nav>

    <main>
        <h2>¡Descubre LaCalandria!</h2>
        <p>Un lugar único para disfrutar, relajarte y conectar con la naturaleza. Mudate a La Calandria y vive una experiencia inolvidable.</p>
        <img src="https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=800&q=80" alt="Naturaleza" style="max-width:100%;height:auto;border-radius:8px;">

        <!-- sección de login integrada (oculta por defecto) -->
        <div id="loginModal" class="login-modal" role="dialog" aria-hidden="true">
            <div class="login-card" role="document">
                <div class="login-close" id="loginClose" title="Cerrar">&times;</div>
                <h2 style="margin-top:0;color:#2E7D32;text-align:center;">Ingresar</h2>
                <?php if ($msg): ?>
                    <div class="msg <?php echo ($type==='success') ? 'success' : 'error'; ?>">
                        <?php echo htmlentities($msg); ?>
                    </div>
                <?php endif; ?>
                <form method="post" action="login.php" autocomplete="off">
                    <label for="user">Usuario</label>
                    <input id="user" name="user" type="text" required autofocus style="width:100%;padding:0.5rem;margin-top:0.25rem;border:1px solid #ccc;border-radius:4px;">
                    <label for="pass" style="display:block;margin-top:0.75rem;">Contraseña</label>
                    <input id="pass" name="pass" type="password" required style="width:100%;padding:0.5rem;margin-top:0.25rem;border:1px solid #ccc;border-radius:4px;">
                    <!-- agregado: selector de perfil -->
                    <label for="role" style="display:block;margin-top:0.75rem;">Perfil</label>
                    <select id="role" name="role" required style="width:100%;padding:0.45rem;margin-top:0.25rem;border:1px solid #ccc;border-radius:4px;">
                        <option value="">-- Seleccionar perfil --</option>
                        <option value="admin">Admin</option>
                        <option value="propietario">Propietario</option>
                        <option value="inquilino">Inquilino</option>
                        <option value="guard">Guardia</option>
                    </select>
                    <button type="submit" style="margin-top:1rem;width:100%;padding:0.7rem;background:#2E7D32;color:#fff;border:none;border-radius:6px;cursor:pointer;font-weight:600;">Ingresar</button>
                </form>
                <div style="margin-top:0.75rem;text-align:center;font-size:0.9rem;">
                    <p>Usuarios de prueba: <strong>admin/1234</strong>, <strong>guard/1234</strong></p>
                    <p><a href="index.php" style="color:#2E7D32;text-decoration:none;">Volver al inicio</a></p>
                </div>
            </div>
        </div>
    </main>

    <footer>
        &copy; 2024 LaCalandria. Todos los derechos reservados.
    </footer>

    <script>
        // mostrar/ocultar modal
        (function(){
            const navLogin = document.getElementById('navLogin');
            const loginModal = document.getElementById('loginModal');
            const loginClose = document.getElementById('loginClose');

            function openLogin(){ loginModal.style.display = 'flex'; loginModal.setAttribute('aria-hidden','false'); }
            function closeLogin(){ loginModal.style.display = 'none'; loginModal.setAttribute('aria-hidden','true'); }

            if(navLogin) navLogin.addEventListener('click', function(e){ e.preventDefault(); openLogin(); });
            if(loginClose) loginClose.addEventListener('click', function(){ closeLogin(); });
            // cerrar al hacer click fuera del card
            loginModal.addEventListener('click', function(e){ if(e.target === loginModal) closeLogin(); });

            // si la query indica abrir el login, hacerlo
            <?php if ($showLogin || $msg): ?>
                openLogin();
            <?php endif; ?>

            // si role indica éxito y se quiere redirigir, se podría manejar aquí
            <?php if ($role === 'admin'): ?>
                // ejemplo: después de login podrías redirigir a panel admin
                // window.location.href = 'admin_dashboard.php'; // activar si tenés endpoint web
            <?php elseif ($role === 'guard'): ?>
                // window.location.href = 'home.php';
            <?php endif; ?>
        })();
    </script>
</body>
</html>