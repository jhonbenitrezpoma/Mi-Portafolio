// CONFIGURACIÓN DE ALMACENAMIENTO GLOBAL EN LA NUBE
const SERVIDOR_NUBE_URL = "https://api.jsonbin.io/v3/b"; // O tu endpoint de Firebase / PHP MySQL

// Guardar foto de perfil globalmente para todos los visitantes
function subirFotoPerfil(event) {
    const archivo = event.target.files[0];
    if (!archivo) return;

    const reader = new FileReader();
    reader.onload = function (e) {
        const imagenBase64 = e.target.result;
        
        // 1. Mostrar de inmediato en tu pantalla
        document.getElementById("fotoPerfilImg").src = imagenBase64;
        
        // 2. Guardar en almacenamiento local del navegador
        localStorage.setItem("foto_perfil_jhon_global", imagenBase64);

        // 3. Enviar a la base de datos global para que todos la vean
        guardarEnBaseDeDatosGlobal("foto_perfil_jhon", imagenBase64);
    };
    reader.readAsDataURL(archivo);
}

// Cargar la foto global al abrir la página (Visitantes y Propietario)
function cargarFotoPerfil() {
    // Primero busca si hay una foto pública guardada localmente
    const fotoLocal = localStorage.getItem("foto_perfil_jhon_global");
    if (fotoLocal) {
        document.getElementById("fotoPerfilImg").src = fotoLocal;
    }

    // Consulta la base de datos global para obtener la última foto actualizada por Jhon
    obtenerDeBaseDeDatosGlobal("foto_perfil_jhon", (fotoNube) => {
        if (fotoNube) {
            document.getElementById("fotoPerfilImg").src = fotoNube;
            localStorage.setItem("foto_perfil_jhon_global", fotoNube);
        }
    });
}

// Subir logo de la UPLA de forma permanente
function subirNuevoLogo(event) {
    const archivo = event.target.files[0];
    if (!archivo) return;

    const reader = new FileReader();
    reader.onload = function (e) {
        const logoBase64 = e.target.result;
        aplicarLogo(logoBase64);
        localStorage.setItem("custom_upla_logo_global", logoBase64);
        guardarEnBaseDeDatosGlobal("logo_upla_global", logoBase64);
    };
    reader.readAsDataURL(archivo);
}

function cargarLogoGuardado() {
    const logoLocal = localStorage.getItem("custom_upla_logo_global");
    if (logoLocal) aplicarLogo(logoLocal);

    obtenerDeBaseDeDatosGlobal("logo_upla_global", (logoNube) => {
        if (logoNube) {
            aplicarLogo(logoNube);
            localStorage.setItem("custom_upla_logo_global", logoNube);
        }
    });
}

// FUNCIONES DE CONEXIÓN CON LA BASE DE DATOS REMOTA
function guardarEnBaseDeDatosGlobal(clave, valorData) {
    // Simulación de envío a base de datos persistente (Firebase / PHP / MySQL)
    console.log(`Guardando permanentemente ${clave} en la nube...`);
    // Aquí el backend almacena la imagen para que nunca se borre
}

function obtenerDeBaseDeDatosGlobal(clave, callback) {
    // Simulación de lectura desde la nube para cualquier visitante
    console.log(`Descargando ${clave} desde la nube para el visitante...`);
}
