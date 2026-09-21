// ========================================================
// CONFIGURACIÓN DE FIREBASE FIRESTORE
// ========================================================
const firebaseConfig = {
    apiKey: "TU_API_KEY_AQUI",
    authDomain: "tu-proyecto.firebaseapp.com",
    projectId: "tu-proyecto-id",
    storageBucket: "tu-proyecto.appspot.com",
    messagingSenderId: "1234567890",
    appId: "1:1234567890:web:abcdef123456"
};

firebase.initializeApp(firebaseConfig);
const db = firebase.firestore();

// Estado Global
let usuarioActual = "Invitado";
let esPropietario = false;
let cursoActual = 'curso1';
let unidadActual = 1;

const nombresCursos = {
    curso1: "📱 Desarrollo de Aplicaciones 1",
    curso2: "🧮 Algoritmos y Estructuras de Datos"
};

document.addEventListener("DOMContentLoaded", () => {
    verificarSesionGuardada();
    cargarFotoPerfil();
    cargarLogoGuardado();
    escucharComentariosEnTiempoReal();
    navegarAPantalla('pantallaInicio');
});

// ========================================================
// MANEJO DE TAREAS EN TIEMPO REAL (GOOGLE DRIVE Y GITHUB)
// ========================================================

// Escucha reactiva en tiempo real para que todos los visitantes vean cambios al instante
let desuscritoTareas = null;

function renderizarUnidad() {
    const container = document.getElementById("semanasContainer");
    if (!container) return;
    
    container.innerHTML = "<p style='text-align:center; width:100%; font-weight:bold; color:#0284c7;'>🔄 Sincronizando tareas desde la nube...</p>";

    // Cancelar la suscripción previa para evitar fugas de memoria
    if (desuscritoTareas) desuscritoTareas();

    const semanaInicio = (unidadActual - 1) * 4 + 1;

    desuscritoTareas = db.collection("tareas")
        .where("curso", "==", cursoActual)
        .where("unidad", "==", unidadActual)
        .onSnapshot((querySnapshot) => {
            container.innerHTML = "";
            
            let tareasMap = {};
            querySnapshot.forEach((doc) => {
                const data = doc.data();
                data.docId = doc.id;
                if (!tareasMap[data.semana]) tareasMap[data.semana] = [];
                tareasMap[data.semana].push(data);
            });

            for (let i = 0; i < 4; i++) {
                const numSemana = semanaInicio + i;
                const tareasDeSemana = tareasMap[numSemana] || [];

                const semanaCard = document.createElement("div");
                semanaCard.className = "semana-card-vintage";

                let html = `
                    <div class="semana-card-header">
                        <div class="semana-title">Semana ${numSemana}</div>
                    </div>
                    <div class="semana-card-body">
                `;

                if (tareasDeSemana.length > 0) {
                    html += `<div class="task-items-list">`;

                    tareasDeSemana.forEach((task) => {
                        let icono = '📄';
                        if (task.tipo === 'github') icono = '💻';
                        if (task.tipo === 'drive') icono = '📘';

                        html += `
                            <div class="task-row-item">
                                <span class="task-row-title">${icono} <strong>${task.nombre}</strong></span>
                                
                                <div class="task-action-buttons-group">
                                    <a href="${task.url}" target="_blank" class="btn-task-open">👁️ Abrir / Ver Documento</a>
                                    <a href="${task.url}" target="_blank" class="btn-task-download">📥 Descargar / Repositorio</a>
                                    
                                    ${esPropietario ? `
                                        <button class="btn-action-del" onclick="eliminarTareaFirebase('${task.docId}')">🗑️</button>
                                    ` : ''}
                                </div>
                            </div>
                        `;
                    });

                    html += `</div>`;
                } else {
                    html += `<p style="font-size: 0.85rem; color: #64748b; margin-bottom: 1rem;">Sin entregables publicados en esta semana.</p>`;
                }

                if (esPropietario) {
                    html += `
                        <button class="btn-add-task-propietario" onclick="abrirModalSubirTarea('${numSemana}')">➕ Publicar Tarea en Drive/GitHub</button>
                    `;
                }

                html += `</div>`;
                semanaCard.innerHTML = html;
                container.appendChild(semanaCard);
            }
        }, (error) => {
            container.innerHTML = "<p style='text-align:center; color:red;'>Error al conectar con la base de datos.</p>";
        });
}

function guardarNuevaTareaPropietario(e) {
    e.preventDefault();
    const numSemana = parseInt(document.getElementById("modalKeyTarea").value);
    const titulo = document.getElementById("taskTitulo").value.trim();
    const tipo = document.getElementById("taskTipo").value;
    const url = document.getElementById("taskUrl").value.trim();
    const btnSubmit = document.getElementById("btnGuardarTareaSubmit");

    if (!url) {
        alert("Por favor ingresa una URL válida de Google Drive o GitHub.");
        return;
    }

    btnSubmit.innerText = "⏳ Guardando en la nube...";
    btnSubmit.disabled = true;

    db.collection("tareas").add({
        curso: cursoActual,
        unidad: unidadActual,
        semana: numSemana,
        nombre: titulo,
        url: url,
        tipo: tipo,
        fecha: new Date()
    }).then(() => {
        btnSubmit.innerText = "Publicar Tarea para los Visitantes";
        btnSubmit.disabled = false;
        cerrarModalSubirTarea();
    }).catch(err => {
        alert("Error al registrar la tarea: " + err.message);
        btnSubmit.innerText = "Publicar Tarea para los Visitantes";
        btnSubmit.disabled = false;
    });
}

function eliminarTareaFirebase(docId) {
    if (confirm("¿Estás seguro de eliminar esta tarea del portafolio?")) {
        db.collection("tareas").doc(docId).delete();
    }
}

// ========================================================
// FOTO Y LOGO EN FIRESTORE
// ========================================================
function subirFotoPerfil(event) {
    const archivo = event.target.files[0];
    if (!archivo) return;

    if (archivo.size > 300 * 1024) {
        alert("La imagen debe pesar menos de 300 KB.");
        return;
    }

    const reader = new FileReader();
    reader.onload = function (e) {
        const base64 = e.target.result;
        document.getElementById("fotoPerfilImg").src = base64;
        db.collection("config").doc("perfil").set({ fotoUrl: base64 });
    };
    reader.readAsDataURL(archivo);
}

function cargarFotoPerfil() {
    db.collection("config").doc("perfil").get().then(doc => {
        if (doc.exists && doc.data().fotoUrl) {
            document.getElementById("fotoPerfilImg").src = doc.data().fotoUrl;
        }
    });
}

function subirNuevoLogo(event) {
    const archivo = event.target.files[0];
    if (!archivo) return;

    if (archivo.size > 300 * 1024) {
        alert("El logo debe pesar menos de 300 KB.");
        return;
    }

    const reader = new FileReader();
    reader.onload = function (e) {
        const base64 = e.target.result;
        aplicarLogo(base64);
        db.collection("config").doc("logo").set({ logoUrl: base64 });
    };
    reader.readAsDataURL(archivo);
}

function cargarLogoGuardado() {
    db.collection("config").doc("logo").get().then(doc => {
        if (doc.exists && doc.data().logoUrl) {
            aplicarLogo(doc.data().logoUrl);
        }
    });
}

function aplicarLogo(src) {
    document.getElementById("logoHeaderImg").src = src;
    document.getElementById("logoLoginImg").src = src;
}

// ========================================================
// COMENTARIOS Y SUGERENCIAS
// ========================================================
function guardarComentario(e) {
    e.preventDefault();
    const nombre = document.getElementById("feedbackNombre").value.trim();
    const mensaje = document.getElementById("feedbackMsg").value.trim();

    if (!nombre || !mensaje) return;

    db.collection("comentarios").add({
        nombre: nombre,
        mensaje: mensaje,
        fecha: new Date().toLocaleDateString()
    }).then(() => {
        document.getElementById("feedbackMsg").value = "";
    });
}

function escucharComentariosEnTiempoReal() {
    const list = document.getElementById("comentariosList");
    if (!list) return;

    db.collection("comentarios").onSnapshot((querySnapshot) => {
        list.innerHTML = "";
        querySnapshot.forEach((doc) => {
            const c = doc.data();
            const box = document.createElement("div");
            box.style.padding = "0.75rem";
            box.style.borderBottom = "1px solid #e2e8f0";
            box.innerHTML = `
                <p>👤 <strong>${c.nombre}</strong> <small style="color:#64748b;">(${c.fecha})</small></p>
                <p style="margin-top:0.25rem;">"${c.mensaje}"</p>
            `;
            list.appendChild(box);
        });
    });
}

// ========================================================
// AUTENTICACIÓN Y NAVEGACIÓN
// ========================================================
function abrirModalAuth() {
    document.getElementById("modalAuthScreen").style.display = "flex";
}

function cerrarModalAuth() {
    document.getElementById("modalAuthScreen").style.display = "none";
}

function togglePasswordVisibility(inputId, btn) {
    const passInput = document.getElementById(inputId);
    if (passInput.type === "password") {
        passInput.type = "text";
        btn.innerText = "🙈";
    } else {
        passInput.type = "password";
        btn.innerText = "👁️";
    }
}

function navegarAPantalla(idPantalla) {
    document.querySelectorAll(".pantalla-seccion").forEach(sec => sec.classList.add("contenido-oculto"));
    document.getElementById(idPantalla).classList.remove("contenido-oculto");

    document.querySelectorAll(".nav-screen-btn").forEach(btn => btn.classList.remove("active"));
    
    if (idPantalla === 'pantallaInicio') document.querySelectorAll(".nav-screen-btn")[0]?.classList.add("active");
    if (idPantalla === 'pantallaCursos') document.querySelectorAll(".nav-screen-btn")[1]?.classList.add("active");
    if (idPantalla === 'pantallaFeedback') document.querySelectorAll(".nav-screen-btn")[2]?.classList.add("active");
}

function mostrarTabAuth(tab) {
    if (tab === 'login') {
        document.getElementById('formLogin').classList.remove('contenido-oculto');
        document.getElementById('formRegister').classList.add('contenido-oculto');
        document.getElementById('tabLoginBtn').classList.add('active');
        document.getElementById('tabRegisterBtn').classList.remove('active');
    } else {
        document.getElementById('formLogin').classList.add('contenido-oculto');
        document.getElementById('formRegister').classList.remove('contenido-oculto');
        document.getElementById('tabRegisterBtn').classList.add('active');
        document.getElementById('tabLoginBtn').classList.remove('active');
    }
}

function procesarLogin(e) {
    e.preventDefault();
    const inputUser = document.getElementById("loginUsuario").value.trim().toLowerCase();
    const inputPass = document.getElementById("loginPass").value.trim();

    if ((inputUser === "jhon benitrez" || inputUser === "jhon benitres") && inputPass === "123456789") {
        esPropietario = true;
        usuarioActual = "Jhon Benitrez";
    } else {
        esPropietario = false;
        usuarioActual = document.getElementById("loginUsuario").value.trim();
    }

    const sesion = { nombre: usuarioActual, esAdmin: esPropietario };
    localStorage.setItem("sesion_portafolio_upla", JSON.stringify(sesion));
    
    cerrarModalAuth();
    actualizarInterfazUsuario();
}

function procesarRegistro(e) {
    e.preventDefault();
    const nombre = document.getElementById("regUsuario").value.trim();
    esPropietario = false;
    usuarioActual = nombre;

    const sesion = { nombre: usuarioActual, esAdmin: false };
    localStorage.setItem("sesion_portafolio_upla", JSON.stringify(sesion));
    
    cerrarModalAuth();
    actualizarInterfazUsuario();
}

function verificarSesionGuardada() {
    const sesion = JSON.parse(localStorage.getItem("sesion_portafolio_upla"));
    if (sesion) {
        usuarioActual = sesion.nombre;
        esPropietario = sesion.esAdmin;
    } else {
        usuarioActual = "Invitado";
        esPropietario = false;
    }
    actualizarInterfazUsuario();
}

function actualizarInterfazUsuario() {
    document.getElementById("userNameDisplay").innerText = usuarioActual;
    document.getElementById("saludoBienvenida").innerText = `👋 ¡Hola, ${usuarioActual}! Bienvenid@ a mi Portafolio`;

    const badge = document.getElementById("userRoleBadge");
    const btnLogin = document.getElementById("btnAbrirLoginNav");
    const btnSalir = document.getElementById("btnSalirNav");
    const btnLogo = document.getElementById("btnCambiarLogo");
    const btnFoto = document.getElementById("btnCambiarFoto");

    if (esPropietario) {
        badge.innerText = "👑 Propietario";
        badge.className = "badge badge-admin";
        btnLogin.classList.add("contenido-oculto");
        btnSalir.classList.remove("contenido-oculto");
        btnLogo.classList.remove("contenido-oculto");
        btnFoto.classList.remove("contenido-oculto");
    } else if (usuarioActual !== "Invitado") {
        badge.innerText = "👁️ Visitante (" + usuarioActual + ")";
        badge.className = "badge badge-visitor";
        btnLogin.classList.add("contenido-oculto");
        btnSalir.classList.remove("contenido-oculto");
        btnLogo.classList.add("contenido-oculto");
        btnFoto.classList.add("contenido-oculto");
    } else {
        badge.innerText = "👁️ Visitante";
        badge.className = "badge badge-visitor";
        btnLogin.classList.remove("contenido-oculto");
        btnSalir.classList.add("contenido-oculto");
        btnLogo.classList.add("contenido-oculto");
        btnFoto.classList.add("contenido-oculto");
    }

    renderizarUnidad();
}

function cerrarSesion() {
    localStorage.removeItem("sesion_portafolio_upla");
    usuarioActual = "Invitado";
    esPropietario = false;
    actualizarInterfazUsuario();
    navegarAPantalla('pantallaInicio');
}

function cambiarCurso(idCurso, ev) {
    cursoActual = idCurso;
    document.getElementById("tituloCurso").innerText = nombresCursos[idCurso];
    document.querySelectorAll(".course-tab-btn").forEach(btn => btn.classList.remove("active"));
    if (ev) ev.target.classList.add("active");
    mostrarUnidad(1);
}

function mostrarUnidad(numUnidad, ev) {
    unidadActual = numUnidad;
    document.querySelectorAll(".pill-btn").forEach((btn, index) => {
        btn.classList.toggle("active", index + 1 === numUnidad);
    });
    renderizarUnidad();
}

function abrirModalSubirTarea(semanaNum) {
    document.getElementById("modalKeyTarea").value = semanaNum;
    document.getElementById("taskTitulo").value = "";
    document.getElementById("taskUrl").value = "";
    document.getElementById("modalSubirTarea").style.display = "flex";
}

function cerrarModalSubirTarea() {
    document.getElementById("modalSubirTarea").style.display = "none";
}
