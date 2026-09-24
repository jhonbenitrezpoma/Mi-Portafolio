```javascript
/* ============================================================
   CONFIGURACIÓN SUPABASE
   ============================================================ */

const SUPABASE_URL =
    "https://pxjqwdmccnfxfpuccqgn.supabase.co";

const SUPABASE_ANON_KEY =
    "sb_publishable_6rodUeO0xzlqrwXTw1i0cQ_S7BOiRq4";

const OWNER_EMAIL =
    "tuhermana1591q@gmail.com";


const NOMBRES_PROPIETARIO = [
    "jhon benitrez",
    "jhon benitres",
    "jhon benitrez poma"
];

const BUCKET = "portafolio";

const SESSION_KEY =
    "sesion_portafolio_upla_v2";

const LOGO_POR_DEFECTO =
    "https://upla.edu.pe/wp-content/uploads/2021/07/LOGO-UPLA.png";

const AVATAR_POR_DEFECTO =
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=400&auto=format&fit=crop";


const configurado =
    !SUPABASE_URL.includes("PEGA_AQUI") &&
    !SUPABASE_ANON_KEY.includes("PEGA_AQUI") &&
    !OWNER_EMAIL.includes("PEGA_AQUI");


let db = null;

let usuarioActual = "Invitado";

let esPropietario = false;

let cursoActual = "curso1";

let unidadActual = 1;

let tareasPorClave = {};

let temasPorClave = {};

let comentarios = [];

let ajustes = {};


const nombresCursos = {

    curso1:
        "📱 Desarrollo de Aplicaciones 1",

    curso2:
        "🧮 Algoritmos y Estructura de Datos"

};


/* ============================================================
   FUNCIONES GENERALES
   ============================================================ */

function esc(s) {

    return String(s ?? "").replace(
        /[&<>"']/g,

        c => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        }[c])
    );

}


let toastTimer;


function toast(msg) {

    const t =
        document.getElementById("toast");

    t.textContent = msg;

    t.classList.add("show");

    clearTimeout(toastTimer);

    toastTimer = setTimeout(
        () => t.classList.remove("show"),
        3200
    );

}


function requiereConfig() {

    if (!configurado) {

        alert(
            "Primero conecta la base de datos Supabase completando las credenciales en el código."
        );

        return false;
    }

    return true;
}


/* ============================================================
   STORAGE
   ============================================================ */

function rutaDesdeUrl(url) {

    const marca =
        `/${BUCKET}/`;

    const i =
        (url || "").indexOf(marca);

    if (i === -1)
        return null;

    return decodeURIComponent(
        url
            .substring(i + marca.length)
            .split("?")[0]
    );

}


async function borrarArchivoStorage(url) {

    const ruta =
        rutaDesdeUrl(url);

    if (!ruta)
        return;

    const { error } =
        await db.storage
            .from(BUCKET)
            .remove([ruta]);

    if (error) {

        console.warn(
            "No se pudo borrar archivo anterior:",
            error.message
        );

    }

}


async function subirArchivoStorage(file, carpeta) {

    const ext =
        (
            file.name
                .split(".")
                .pop() ||
            "bin"
        )
        .toLowerCase()
        .replace(
            /[^a-z0-9]/g,
            ""
        ) || "bin";


    const ruta =
        `${carpeta}/${Date.now()}-${Math.random()
            .toString(36)
            .slice(2, 8)}.${ext}`;


    const { error } =
        await db.storage
            .from(BUCKET)
            .upload(
                ruta,
                file,
                {
                    cacheControl: "3600",
                    upsert: false,
                    contentType:
                        file.type || undefined
                }
            );


    if (error)
        throw error;


    const { data } =
        db.storage
            .from(BUCKET)
            .getPublicUrl(ruta);


    return data.publicUrl;
}


function nombreDescarga(task) {

    const m =
        (task.url || "")
            .split("?")[0]
            .match(/\.([a-z0-9]+)$/i);


    const base =
        String(task.nombre)
            .replace(
                /[\\/:*?"<>|]/g,
                ""
            )
            .trim() ||
        "archivo";


    return base +
        (m ? "." + m[1] : "");

}


/* ============================================================
   INICIO
   ============================================================ */

document.addEventListener(
    "DOMContentLoaded",
    async () => {

        limpiarFormulariosAuth();

        navegarAPantalla(
            "pantallaInicio"
        );

        aplicarAjustes();


        if (!configurado) {

            document
                .getElementById("configBanner")
                .classList
                .remove("contenido-oculto");


            actualizarInterfazUsuario();

            renderizarComentarios();

            return;
        }


        db =
            window.supabase
                .createClient(
                    SUPABASE_URL,
                    SUPABASE_ANON_KEY
                );


        await restaurarSesion();

        await cargarTodo();

        suscribirseTiempoReal();


        setInterval(
            cargarTodo,
            45000
        );


        document.addEventListener(
            "visibilitychange",
            () => {

                if (!document.hidden)
                    cargarTodo();

            }
        );

    }
);


/* ============================================================
   SESIÓN
   ============================================================ */

async function restaurarSesion() {

    const { data } =
        await db.auth.getSession();

    const sesion =
        data &&
        data.session;


    if (
        sesion &&
        (
            sesion.user.email ||
            ""
        ).toLowerCase()
        ===
        OWNER_EMAIL.toLowerCase()
    ) {

        esPropietario = true;

        usuarioActual =
            "Jhon Benitrez";

    } else {

        const guardada =
            JSON.parse(
                localStorage.getItem(
                    SESSION_KEY
                ) || "null"
            );


        if (
            guardada &&
            !guardada.esAdmin
        ) {

            usuarioActual =
                guardada.nombre;

        }

    }


    actualizarInterfazUsuario();

}


/* ============================================================
   CARGAR INFORMACIÓN
   ============================================================ */

async function cargarAjustes() {

    const { data, error } =
        await db
            .from("ajustes")
            .select("clave, valor");


    if (error)
        return console.error(error);


    ajustes = {};


    data.forEach(
        r => ajustes[r.clave] = r.valor
    );


    aplicarAjustes();

}


async function cargarTareas() {

    const { data, error } =
        await db
            .from("tareas")
            .select("*")
            .order(
                "creado_en",
                {
                    ascending: true
                }
            );


    if (error)
        return console.error(error);


    tareasPorClave = {};


    data.forEach(t => {

        if (
            !tareasPorClave[t.clave]
        ) {

            tareasPorClave[t.clave] =
                [];

        }


        tareasPorClave[t.clave]
            .push(t);

    });


    renderizarUnidad();

}


async function cargarTemas() {

    const { data, error } =
        await db
            .from("temas")
            .select(
                "clave, titulo"
            );


    if (error)
        return console.error(error);


    temasPorClave = {};


    data.forEach(
        t =>
            temasPorClave[t.clave] =
                t.titulo
    );


    renderizarUnidad();

}


async function cargarComentarios() {

    const { data, error } =
        await db
            .from("comentarios")
            .select("*")
            .order(
                "creado_en",
                {
                    ascending: false
                }
            );


    if (error)
        return console.error(error);


    comentarios = data;

    renderizarComentarios();

}


async function cargarTodo() {

    await Promise.all([

        cargarAjustes(),

        cargarTareas(),

        cargarTemas(),

        cargarComentarios()

    ]);

}


/* ============================================================
   TIEMPO REAL
   ============================================================ */

function suscribirseTiempoReal() {

    if (
        window.self !== window.top &&
        (
            location.origin.includes(
                "webcontainer"
            ) ||
            location.origin.includes(
                "sandbox"
            ) ||
            location.origin.includes(
                "preview"
            )
        )
    ) {

        console.warn(
            "WebSockets restricted in preview environment. Polling active."
        );

        return;
    }


    try {

        const avisar = () => {

            if (!esPropietario) {

                toast(
                    "🔄 Contenido actualizado en tiempo real"
                );

            }

        };


        db.channel(
            "portafolio-cambios-v2"
        )

        .on(
            "postgres_changes",
            {
                event: "*",
                schema: "public",
                table: "ajustes"
            },
            () => {

                cargarAjustes();

                avisar();

            }
        )

        .on(
            "postgres_changes",
            {
                event: "*",
                schema: "public",
                table: "tareas"
            },
            () => {

                cargarTareas();

                avisar();

            }
        )

        .on(
            "postgres_changes",
            {
                event: "*",
                schema: "public",
                table: "temas"
            },
            () => {

                cargarTemas();

                avisar();

            }
        )

        .on(
            "postgres_changes",
            {
                event: "*",
                schema: "public",
                table: "comentarios"
            },
            () =>
                cargarComentarios()
        )

        .subscribe();


    } catch (err) {

        console.warn(
            "Realtime error:",
            err
        );

    }

}


/* ============================================================
   AJUSTES / IMÁGENES
   ============================================================ */

function aplicarAjustes() {

    const foto =
        ajustes.foto_perfil ||
        AVATAR_POR_DEFECTO;

    const logo =
        ajustes.logo ||
        LOGO_POR_DEFECTO;


    document.getElementById(
        "fotoPerfilImg"
    ).src = foto;


    document.getElementById(
        "logoHeaderImg"
    ).src = logo;


    document.getElementById(
        "logoLoginImg"
    ).src = logo;

}


async function subirImagenAjuste(
    event,
    clave,
    carpeta
) {

    const archivo =
        event.target.files[0];

    event.target.value = "";


    if (!archivo)
        return;


    if (
        !esPropietario ||
        !requiereConfig()
    )
        return;


    if (
        !archivo.type.startsWith(
            "image/"
        )
    ) {

        return alert(
            "Selecciona un archivo de imagen válido."
        );

    }


    if (
        archivo.size >
        5 * 1024 * 1024
    ) {

        return alert(
            "La imagen pesa más de 5 MB."
        );

    }


    toast(
        "⏳ Subiendo imagen..."
    );


    try {

        const urlAnterior =
            ajustes[clave];


        const url =
            await subirArchivoStorage(
                archivo,
                carpeta
            );


        const { error } =
            await db
                .from("ajustes")
                .upsert({

                    clave: clave,

                    valor: url,

                    updated_at:
                        new Date()
                            .toISOString()

                });


        if (error)
            throw error;


        ajustes[clave] =
            url;


        aplicarAjustes();


        if (urlAnterior)
            borrarArchivoStorage(
                urlAnterior
            );


        toast(
            "✅ Imagen actualizada con éxito"
        );


    } catch (err) {

        console.error(err);

        alert(
            "No se pudo guardar la imagen: " +
            (
                err.message ||
                err
            )
        );

    }

}


/* ============================================================
   LOGIN
   ============================================================ */

function abrirModalAuth() {

    limpiarFormulariosAuth();

    document.getElementById(
        "modalAuthScreen"
    ).style.display = "flex";

}


function cerrarModalAuth() {

    document.getElementById(
        "modalAuthScreen"
    ).style.display = "none";

}


function togglePasswordVisibility(
    inputId,
    btn
) {

    const passInput =
        document.getElementById(
            inputId
        );


    if (
        passInput.type ===
        "password"
    ) {

        passInput.type = "text";

        btn.innerText = "🙈";

    } else {

        passInput.type = "password";

        btn.innerText = "👁️";

    }

}


function limpiarFormulariosAuth() {

    document
        .getElementById(
            "formLogin"
        )
        .reset();


    document
        .getElementById(
            "formRegister"
        )
        .reset();

}


function navegarAPantalla(
    idPantalla
) {

    document
        .querySelectorAll(
            ".pantalla-seccion"
        )
        .forEach(
            sec =>
                sec.classList.add(
                    "contenido-oculto"
                )
        );


    document
        .getElementById(
            idPantalla
        )
        .classList
        .remove(
            "contenido-oculto"
        );


    const botones =
        document.querySelectorAll(
            ".nav-screens .nav-screen-btn"
        );


    botones.forEach(
        b =>
            b.classList.remove(
                "active"
            )
    );


    if (
        idPantalla ===
        "pantallaInicio"
    )
        botones[0]?.classList.add(
            "active"
        );


    if (
        idPantalla ===
        "pantallaCursos"
    )
        botones[1]?.classList.add(
            "active"
        );


    if (
        idPantalla ===
        "pantallaFeedback"
    )
        botones[2]?.classList.add(
            "active"
        );


    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });

}


function mostrarTabAuth(tab) {

    limpiarFormulariosAuth();


    if (tab === "login") {

        document
            .getElementById(
                "formLogin"
            )
            .classList
            .remove(
                "contenido-oculto"
            );


        document
            .getElementById(
                "formRegister"
            )
            .classList
            .add(
                "contenido-oculto"
            );


        document
            .getElementById(
                "tabLoginBtn"
            )
            .classList
            .add("active");


        document
            .getElementById(
                "tabRegisterBtn"
            )
            .classList
            .remove("active");


    } else {

        document
            .getElementById(
                "formLogin"
            )
            .classList
            .add(
                "contenido-oculto"
            );


        document
            .getElementById(
                "formRegister"
            )
            .classList
            .remove(
                "contenido-oculto"
            );


        document
            .getElementById(
                "tabRegisterBtn"
            )
            .classList
            .add("active");


        document
            .getElementById(
                "tabLoginBtn"
            )
            .classList
            .remove("active");

    }

}


/* ============================================================
   PROCESAR LOGIN
   ============================================================ */

async function procesarLogin(e) {

    e.preventDefault();


    const nombreEscrito =
        document
            .getElementById(
                "loginUsuario"
            )
            .value
            .trim();


    const pass =
        document
            .getElementById(
                "loginPass"
            )
            .value;


    if (
        NOMBRES_PROPIETARIO.includes(
            nombreEscrito.toLowerCase()
        )
    ) {

        if (!requiereConfig())
            return;


        if (!pass)
            return alert(
                "Ingresa la contraseña de propietario."
            );


        const btn =
            document.getElementById(
                "btnLoginSubmit"
            );


        btn.disabled = true;


        const { error } =
            await db.auth
                .signInWithPassword({

                    email:
                        OWNER_EMAIL,

                    password:
                        pass

                });


        btn.disabled = false;


        if (error)
            return alert(
                "Contraseña incorrecta."
            );


        esPropietario = true;

        usuarioActual =
            "Jhon Benitrez";


        localStorage.removeItem(
            SESSION_KEY
        );


    } else {

        esPropietario = false;

        usuarioActual =
            nombreEscrito;


        localStorage.setItem(

            SESSION_KEY,

            JSON.stringify({

                nombre:
                    usuarioActual,

                esAdmin:
                    false

            })

        );

    }


    limpiarFormulariosAuth();

    cerrarModalAuth();

    actualizarInterfazUsuario();

    toast(
        "✨ Sesión iniciada correctamente"
    );

}


/* ============================================================
   REGISTRO
   ============================================================ */

function procesarRegistro(e) {

    e.preventDefault();


    const nombre =
        document
            .getElementById(
                "regUsuario"
            )
            .value
            .trim();


    if (
        NOMBRES_PROPIETARIO.includes(
            nombre.toLowerCase()
        )
    ) {

        return alert(
            "Ese nombre está reservado. Usa otro."
        );

    }


    esPropietario = false;

    usuarioActual =
        nombre;


    localStorage.setItem(

        SESSION_KEY,

        JSON.stringify({

            nombre:
                usuarioActual,

            esAdmin:
                false

        })

    );


    limpiarFormulariosAuth();

    cerrarModalAuth();

    actualizarInterfazUsuario();

    toast(
        "✨ Cuenta creada con éxito"
    );

}


/* ============================================================
   CERRAR SESIÓN
   ============================================================ */

async function cerrarSesion() {

    if (db)
        await db.auth.signOut();


    localStorage.removeItem(
        SESSION_KEY
    );


    usuarioActual =
        "Invitado";


    esPropietario =
        false;


    limpiarFormulariosAuth();

    actualizarInterfazUsuario();

    navegarAPantalla(
        "pantallaInicio"
    );


    toast(
        "👋 Sesión cerrada"
    );

}


/* ============================================================
   INTERFAZ USUARIO
   ============================================================ */

function actualizarInterfazUsuario() {

    document
        .getElementById(
            "userNameDisplay"
        )
        .innerText =
        usuarioActual;


    document
        .getElementById(
            "saludoBienvenida"
        )
        .innerText =
        usuarioActual === "Invitado"

            ? "👋 ¡Bienvenido/a a mi Portafolio!"

            : `👋 ¡Hola, ${usuarioActual}! Bienvenid@ a mi Portafolio`;


    const badge =
        document.getElementById(
            "userRoleBadge"
        );


    const btnLogin =
        document.getElementById(
            "btnAbrirLoginNav"
        );


    const btnSalir =
        document.getElementById(
            "btnSalirNav"
        );


    const btnLogo =
        document.getElementById(
            "btnCambiarLogo"
        );


    const btnFoto =
        document.getElementById(
            "btnCambiarFoto"
        );


    if (esPropietario) {

        badge.innerText =
            "👑 Propietario";

        badge.className =
            "badge badge-admin";


        btnLogin.classList.add(
            "contenido-oculto"
        );


        btnSalir.classList.remove(
            "contenido-oculto"
        );


        btnLogo.classList.remove(
            "contenido-oculto"
        );


        btnFoto.classList.remove(
            "contenido-oculto"
        );


    } else if (
        usuarioActual !== "Invitado"
    ) {

        badge.innerText =
            "👁️ Visitante (" +
            usuarioActual +
            ")";


        badge.className =
            "badge badge-visitor";


        btnLogin.classList.add(
            "contenido-oculto"
        );


        btnSalir.classList.remove(
            "contenido-oculto"
        );


        btnLogo.classList.add(
            "contenido-oculto"
        );


        btnFoto.classList.add(
            "contenido-oculto"
        );


    } else {

        badge.innerText =
            "👁️ Visitante";


        badge.className =
            "badge badge-visitor";


        btnLogin.classList.remove(
            "contenido-oculto"
        );


        btnSalir.classList.add(
            "contenido-oculto"
        );


        btnLogo.classList.add(
            "contenido-oculto"
        );


        btnFoto.classList.add(
            "contenido-oculto"
        );

    }


    renderizarUnidad();

    renderizarComentarios();

}


/* ============================================================
   CURSOS Y UNIDADES
   ============================================================ */

function cambiarCurso(
    idCurso,
    ev
) {

    cursoActual =
        idCurso;


    document
        .getElementById(
            "tituloCurso"
        )
        .innerText =
        nombresCursos[idCurso];


    document
        .querySelectorAll(
            ".course-tab-btn"
        )
        .forEach(
            btn =>
                btn.classList.remove(
                    "active"
                )
        );


    if (ev)
        ev.currentTarget.classList.add(
            "active"
        );


    mostrarUnidad(1);

}


function mostrarUnidad(
    numUnidad
) {

    unidadActual =
        numUnidad;


    document
        .querySelectorAll(
            ".pill-btn"
        )
        .forEach(
            (btn, index) => {

                btn.classList.toggle(
                    "active",
                    index + 1 ===
                    numUnidad
                );

            }
        );


    renderizarUnidad();

}


/* ============================================================
   RENDERIZAR SEMANAS Y TAREAS
   ============================================================ */

function renderizarUnidad() {

    const container =
        document.getElementById(
            "semanasContainer"
        );


    if (!container)
        return;


    container.innerHTML = "";


    const semanaInicio =
        (unidadActual - 1) * 4 + 1;


    for (
        let i = 0;
        i < 4;
        i++
    ) {

        const numSemana =
            semanaInicio + i;


        const keyTarea =
            `${cursoActual}_U${unidadActual}_S${numSemana}`;


        const tituloClase =
            temasPorClave[
                keyTarea
            ] || "";


        const tareas =
            tareasPorClave[
                keyTarea
            ] || [];


        const semanaCard =
            document.createElement(
                "div"
            );


        semanaCard.className =
            "semana-card-vintage glass-panel";


        let html = `

            <div class="semana-card-header">

                <div class="semana-title font-vintage-title">
                    Semana ${numSemana}
                </div>

                ${
                    tituloClase

                    ? `
                        <div class="clase-tema">
                            📘 ${esc(tituloClase)}
                        </div>
                    `

                    : `
                        <div class="clase-tema-vacio">
                            Sin tema registrado
                        </div>
                    `
                }

                ${
                    esPropietario

                    ? `
                        <button
                            class="btn-xs-edit font-vintage-title"
                            onclick="editarTituloClase('${keyTarea}')"
                        >
                            ✏️ Editar Tema
                        </button>
                    `

                    : ""
                }

            </div>


            <div class="semana-card-body">

        `;


        if (tareas.length > 0) {

            html += `

                <div class="task-count-tag">
                    ✔ ${tareas.length}
                    Tarea(s) publicada(s)
                </div>

                <div class="task-items-list">

            `;


            tareas.forEach(
                task => {

                    const esArchivo =
                        task.tipo ===
                        "archivo";


                    const urlDescarga =
                        esArchivo

                        ? `${task.url}?download=${encodeURIComponent(
                            nombreDescarga(task)
                          )}`

                        : "";


                    html += `

                        <div class="task-row-item">

                            <span class="task-row-title">

                                ${
                                    esArchivo
                                    ? "📄"
                                    : "🔗"
                                }

                                <strong>
                                    ${esc(task.nombre)}
                                </strong>

                            </span>


                            <div class="task-action-buttons-group">

                                <a
                                    href="${esc(task.url)}"
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    class="btn-task-open"
                                >
                                    👁️ Ver / Abrir
                                </a>


                                ${
                                    esArchivo

                                    ? `
                                        <a
                                            href="${esc(urlDescarga)}"
                                            class="btn-task-download"
                                        >
                                            📥 Descargar
                                        </a>
                                    `

                                    : ""
                                }


                                ${
                                    esPropietario

                                    ? `

                                        <button
                                            class="btn-action-edit"
                                            onclick="abrirModalEditarTarea('${keyTarea}','${task.id}')"
                                        >
                                            ✏️
                                        </button>


                                        <button
                                            class="btn-action-del"
                                            onclick="eliminarTareaPropietario('${keyTarea}','${task.id}')"
                                        >
                                            🗑️
                                        </button>

                                    `

                                    : ""
                                }

                            </div>

                        </div>

                    `;

                }
            );


            html += `

                </div>

            `;


        } else {

            html += `

                <p class="empty-task-text">
                    Sin tareas publicadas para esta semana.
                </p>

            `;

        }


        if (esPropietario) {

            html += `

                <button
                    class="btn-add-task-propietario font-vintage-title"
                    onclick="abrirModalSubirTarea('${keyTarea}')"
                >
                    ➕ Agregar Tarea Nueva
                </button>

            `;

        }


        html += `

            </div>

        `;


        semanaCard.innerHTML =
            html;


        container.appendChild(
            semanaCard
        );

    }

}


/* ============================================================
   MODAL TAREAS
   ============================================================ */

function abrirModalSubirTarea(
    keyTarea
) {

    document.getElementById(
        "modalKeyTarea"
    ).value =
        keyTarea;


    document.getElementById(
        "modalEditId"
    ).value = "";


    document.getElementById(
        "modalTareaTituloAccion"
    ).innerText =
        "📤 Agregar Nueva Tarea";


    document.getElementById(
        "taskTitulo"
    ).value = "";


    document.getElementById(
        "taskUrl"
    ).value = "";


    document.getElementById(
        "taskFile"
    ).value = "";


    document.getElementById(
        "taskTipo"
    ).value =
        "archivo";


    cambiarTipoInputTask();


    document.getElementById(
        "modalSubirTarea"
    ).style.display =
        "flex";

}


function abrirModalEditarTarea(
    keyTarea,
    id
) {

    const task =
        (
            tareasPorClave[
                keyTarea
            ] || []
        ).find(
            t => t.id === id
        );


    if (!task)
        return;


    document.getElementById(
        "modalKeyTarea"
    ).value =
        keyTarea;


    document.getElementById(
        "modalEditId"
    ).value =
        id;


    document.getElementById(
        "modalTareaTituloAccion"
    ).innerText =
        "✏️ Editar Tarea";


    document.getElementById(
        "taskTitulo"
    ).value =
        task.nombre;


    document.getElementById(
        "taskFile"
    ).value = "";


    document.getElementById(
        "taskTipo"
    ).value =
        task.tipo;


    document.getElementById(
        "taskUrl"
    ).value =
        task.tipo === "enlace"
            ? task.url
            : "";


    cambiarTipoInputTask();


    document.getElementById(
        "modalSubirTarea"
    ).style.display =
        "flex";

}


function cerrarModalSubirTarea() {

    document.getElementById(
        "modalSubirTarea"
    ).style.display =
        "none";

}


function cambiarTipoInputTask() {

    const tipo =
        document.getElementById(
            "taskTipo"
        ).value;


    document
        .getElementById(
            "boxTaskFile"
        )
        .classList
        .toggle(
            "contenido-oculto",
            tipo !== "archivo"
        );


    document
        .getElementById(
            "boxTaskUrl"
        )
        .classList
        .toggle(
            "contenido-oculto",
            tipo !== "enlace"
        );

}


/* ============================================================
   GUARDAR / EDITAR TAREA
   ============================================================ */

async function guardarNuevaTareaPropietario(e) {

    e.preventDefault();


    if (
        !esPropietario ||
        !requiereConfig()
    )
        return;


    const keyTarea =
        document.getElementById(
            "modalKeyTarea"
        ).value;


    const editId =
        document.getElementById(
            "modalEditId"
        ).value;


    const titulo =
        document.getElementById(
            "taskTitulo"
        ).value.trim();


    const tipo =
        document.getElementById(
            "taskTipo"
        ).value;


    const existente =
        editId

        ? (
            tareasPorClave[
                keyTarea
            ] || []
        ).find(
            t => t.id === editId
        )

        : null;


    const payload = {

        clave:
            keyTarea,

        nombre:
            titulo,

        tipo:
            tipo

    };


    const btn =
        document.getElementById(
            "btnGuardarTarea"
        );


    try {

        btn.disabled = true;

        btn.innerText =
            "⏳ Guardando...";


        if (
            tipo ===
            "enlace"
        ) {

            const url =
                document.getElementById(
                    "taskUrl"
                ).value.trim();


            if (!url)
                throw new Error(
                    "Ingresa el enlace web."
                );


            payload.url =
                url;


        } else {

            const archivo =
                document.getElementById(
                    "taskFile"
                ).files[0];


            if (archivo) {

                payload.url =
                    await subirArchivoStorage(
                        archivo,
                        "tareas"
                    );


            } else if (
                existente &&
                existente.tipo ===
                "archivo"
            ) {

                payload.url =
                    existente.url;


            } else {

                throw new Error(
                    "Selecciona un archivo adjunto."
                );

            }

        }


        if (existente) {

            const { error } =
                await db
                    .from("tareas")
                    .update(payload)
                    .eq(
                        "id",
                        existente.id
                    );


            if (error)
                throw error;


            if (
                existente.tipo ===
                "archivo" &&
                existente.url !==
                payload.url
            ) {

                borrarArchivoStorage(
                    existente.url
                );

            }


        } else {

            const { error } =
                await db
                    .from("tareas")
                    .insert(
                        payload
                    );


            if (error)
                throw error;

        }


        cerrarModalSubirTarea();

        await cargarTareas();

        toast(
            "✅ Tarea guardada con éxito"
        );


    } catch (err) {

        console.error(err);

        alert(
            err.message ||
            "No se pudo guardar la tarea."
        );


    } finally {

        btn.disabled =
            false;

        btn.innerText =
            "Guardar Tarea";

    }

}


/* ============================================================
   ELIMINAR TAREA
   ============================================================ */

async function eliminarTareaPropietario(
    keyTarea,
    id
) {

    if (
        !esPropietario ||
        !requiereConfig()
    )
        return;


    if (
        !confirm(
            "¿Estás seguro de eliminar esta tarea?"
        )
    )
        return;


    const task =
        (
            tareasPorClave[
                keyTarea
            ] || []
        ).find(
            t => t.id === id
        );


    const { error } =
        await db
            .from("tareas")
            .delete()
            .eq(
                "id",
                id
            );


    if (error)
        return alert(
            "No se pudo eliminar: " +
            error.message
        );


    if (
        task &&
        task.tipo ===
        "archivo"
    ) {

        borrarArchivoStorage(
            task.url
        );

    }


    await cargarTareas();


    toast(
        "🗑️ Tarea eliminada"
    );

}


/* ============================================================
   EDITAR TEMA DE SEMANA
   ============================================================ */

async function editarTituloClase(
    keyTarea
) {

    if (
        !esPropietario ||
        !requiereConfig()
    )
        return;


    const actual =
        temasPorClave[
            keyTarea
        ] || "";


    const nuevo =
        prompt(
            "Ingresa el título o tema de esta semana:",
            actual
        );


    if (nuevo === null)
        return;


    const { error } =
        await db
            .from("temas")
            .upsert({

                clave:
                    keyTarea,

                titulo:
                    nuevo.trim()

            });


    if (error)
        return alert(
            "No se pudo guardar el tema: " +
            error.message
        );


    temasPorClave[
        keyTarea
    ] =
        nuevo.trim();


    renderizarUnidad();


    toast(
        "✅ Tema actualizado"
    );

}


/* ============================================================
   COMENTARIOS
   ============================================================ */

async function guardarComentario(e) {

    e.preventDefault();


    if (!requiereConfig())
        return;


    const nombre =
        document
            .getElementById(
                "feedbackNombre"
            )
            .value
            .trim();


    const mensaje =
        document
            .getElementById(
                "feedbackMsg"
            )
            .value
            .trim();


    if (
        !nombre ||
        !mensaje
    )
        return;


    const btn =
        document.getElementById(
            "btnEnviarComentario"
        );


    btn.disabled = true;


    const { error } =
        await db
            .from("comentarios")
            .insert({

                nombre:
                    nombre,

                mensaje:
                    mensaje

            });


    btn.disabled = false;


    if (error)
        return alert(
            "No se pudo enviar la sugerencia: " +
            error.message
        );


    document
        .getElementById(
            "feedbackMsg"
        )
        .value = "";


    await cargarComentarios();


    toast(
        "✅ ¡Gracias por tu sugerencia!"
    );

}


function renderizarComentarios() {

    const list =
        document.getElementById(
            "comentariosList"
        );


    if (!list)
        return;


    if (
        comentarios.length === 0
    ) {

        list.innerHTML = `

            <p class="empty-task-text">
                Aún no hay sugerencias.
                ¡Sé el primero en dejar un mensaje!
            </p>

        `;

        return;
    }


    list.innerHTML =
        comentarios
            .map(
                c => `

                    <div class="comment-item-vintage">

                        <div class="comment-head">

                            <p class="c-author">

                                👤

                                <strong>
                                    ${esc(c.nombre)}
                                </strong>

                                <span class="c-date">

                                    (
                                    ${new Date(
                                        c.creado_en
                                    ).toLocaleDateString(
                                        "es-PE"
                                    )}
                                    )

                                </span>

                            </p>


                            ${
                                esPropietario

                                ? `

                                    <button
                                        class="btn-comment-del"
                                        onclick="eliminarComentario('${c.id}')"
                                    >
                                        🗑️ Eliminar
                                    </button>

                                `

                                : ""
                            }

                        </div>


                        <p class="c-text">

                            "${esc(c.mensaje)}"

                        </p>

                    </div>

                `
            )
            .join("");

}


async function eliminarComentario(id) {

    if (
        !esPropietario ||
        !requiereConfig()
    )
        return;


    if (
        !confirm(
            "¿Eliminar esta sugerencia?"
        )
    )
        return;


    const { error } =
        await db
            .from("comentarios")
            .delete()
            .eq(
                "id",
                id
            );


    if (error)
        return alert(
            "No se pudo eliminar: " +
            error.message
        );


    await cargarComentarios();


    toast(
        "🗑️ Sugerencia eliminada"
    );

}
```
