
(() => {
  "use strict";

  // Evitar crear dos robots
  if (document.getElementById("botDock")) return;

  // API externa opcional
  // Nunca coloques aquí tu clave secreta.
  const API_URL = "";

  const RECHAZO =
    "No puedo realizar esa actividad. Soy RONIN y solo " +
    "puedo ayudarte con Algoritmos y Estructuras de Datos " +
    "y Desarrollo de Aplicaciones.";

  // Crear interfaz automáticamente
  const dock = document.createElement("div");
  dock.id = "botDock";

  dock.innerHTML = `
    <button id="botLauncher" type="button"
      aria-label="Abrir chatbot RONIN">🤖</button>

    <section id="botPanel" hidden>
      <header id="botHandle">
        <strong>浪人 RONIN · IA ACADÉMICA</strong>
        <button id="botClose" type="button">✕</button>
      </header>

      <div id="botMessages"
        role="log" aria-live="polite"></div>

      <label class="bot-file-label">
        📎 Adjuntar código
        <input id="botFile" type="file"
          accept=".java,.js,.html,.css,.sql,.json,.txt,.py,.xml,.md,.psc,.pse">
      </label>

      <form id="botForm">
        <textarea id="botInput" rows="2"
          maxlength="16000"
          placeholder="Pregunta o pega tu código..."></textarea>
        <button type="submit">➤</button>
      </form>

      <div id="botStatus">
        RONIN · Tutor académico
      </div>
    </section>
  `;

  document.body.appendChild(dock);

  const $ = id => document.getElementById(id);
  const panel = $("botPanel");
  const input = $("botInput");
  const messages = $("botMessages");
  const status = $("botStatus");

  // Conocimientos locales
  const conocimientos = [
    {
      palabras: ["algoritmo", "pseudocodigo", "pseint"],
      respuesta: `Un algoritmo es una secuencia finita
de instrucciones para resolver un problema.

Ejemplo PSeInt:

Algoritmo Sumar
  Definir a, b Como Entero
  Leer a, b
  Escribir a + b
FinAlgoritmo`
    },
    {
      palabras: ["arreglo", "vector", "array", "matriz"],
      respuesta: `Un arreglo almacena varios elementos.

Ejemplo Java:

int[] numeros = {10, 20, 30};

for (int n : numeros) {
    System.out.println(n);
}

Acceso por índice: O(1).
Búsqueda secuencial: O(n).`
    },
    {
      palabras: ["pila", "stack", "lifo"],
      respuesta: `Una pila funciona mediante LIFO:
último en entrar, primero en salir.

Ejemplo Java:

Deque<Integer> pila = new ArrayDeque<>();
pila.push(10);
pila.push(20);
System.out.println(pila.pop());

// Resultado: 20`
    },
    {
      palabras: ["cola", "queue", "fifo"],
      respuesta: `Una cola utiliza FIFO:
primero en entrar, primero en salir.

Ejemplo Java:

Queue<Integer> cola = new ArrayDeque<>();
cola.add(10);
cola.add(20);
System.out.println(cola.remove());

// Resultado: 10`
    },
    {
      palabras: ["lista enlazada", "nodo"],
      respuesta: `Una lista enlazada contiene nodos.

Cada nodo almacena un dato y una referencia
al siguiente nodo.

La búsqueda normalmente cuesta O(n).`
    },
    {
      palabras: ["arbol", "grafo", "bfs", "dfs"],
      respuesta: `Los árboles representan jerarquías.

Los grafos representan conexiones entre
vértices mediante aristas.

BFS: búsqueda por amplitud.
DFS: búsqueda por profundidad.`
    },
    {
      palabras: ["complejidad", "big o"],
      respuesta: `La notación Big O expresa el crecimiento
del costo de un algoritmo.

O(1): constante.
O(log n): logarítmico.
O(n): lineal.
O(n log n): lineal-logarítmico.
O(n²): cuadrático.`
    },
    {
      palabras: ["ordenamiento", "burbuja", "quicksort"],
      respuesta: `Los algoritmos de ordenamiento
organizan elementos.

Burbuja: O(n²) promedio.
Merge Sort: O(n log n).
Quick Sort: O(n log n) promedio.`
    },
    {
      palabras: ["busqueda", "secuencial", "binaria"],
      respuesta: `Búsqueda secuencial:
revisa elementos uno por uno. O(n).

Búsqueda binaria:
requiere datos ordenados. O(log n).`
    },
    {
      palabras: ["recursion", "factorial"],
      respuesta: `Una función recursiva se llama
a sí misma.

Ejemplo Java:

static int factorial(int n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1);
}`
    },
    {
      palabras: ["java", "netbeans", "herencia", "clase"],
      respuesta: `Java permite crear aplicaciones
mediante programación orientada a objetos.

Conceptos:
• Clases
• Objetos
• Herencia
• Polimorfismo
• Encapsulamiento
• Interfaces

Puedes pegar código Java para consultar
conceptos. El análisis detallado requiere
conectar una API de IA.`
    },
    {
      palabras: ["html", "css", "javascript", "web"],
      respuesta: `En desarrollo web:

HTML crea la estructura.
CSS define los estilos.
JavaScript agrega interactividad.

Las aplicaciones también pueden utilizar
servidores, APIs y bases de datos.`
    },
    {
      palabras: ["sql", "supabase", "base de datos"],
      respuesta: `SQL permite consultar y modificar datos.

Ejemplo:

SELECT * FROM estudiantes;

Supabase ofrece PostgreSQL,
autenticación, almacenamiento y APIs.

Para corregir consultas concretas,
conecta el servicio de IA.`
    },
    {
      palabras: ["maven", "pom.xml"],
      respuesta: `Maven permite gestionar proyectos Java.

El archivo pom.xml define:
• Dependencias
• Versión del proyecto
• Plugins
• Configuración de compilación`
    }
  ];

  function normalizar(texto) {
    return texto.toLowerCase()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "");
  }

  const palabrasPermitidas = [
    "algoritmo", "estructura", "arreglo", "vector",
    "matriz", "pila", "cola", "lista", "nodo",
    "arbol", "grafo", "complejidad", "ordenamiento",
    "busqueda", "recursion", "pseint", "java",
    "netbeans", "programacion", "codigo", "error",
    "funcion", "variable", "bucle", "condicional",
    "clase", "objeto", "herencia", "interfaz",
    "metodo", "html", "css", "javascript",
    "desarrollo", "aplicacion", "frontend",
    "backend", "api", "supabase", "sql",
    "base de datos", "web", "github", "maven",
    "proyecto", "ejercicio"
  ];

  function esDelCurso(texto) {
    const t = normalizar(texto);
    return palabrasPermitidas.some(p =>
      new RegExp(
        "(^|[^a-z0-9])" + p + "([^a-z0-9]|$)"
      ).test(t)
    );
  }

  function responderLocal(pregunta) {
    const texto = normalizar(pregunta);

    if (/^(hola|buenas|hey|saludos)\b/.test(texto)) {
      return "¡Hola! Soy RONIN. Pregúntame sobre " +
        "Algoritmos y Estructuras de Datos o " +
        "Desarrollo de Aplicaciones.";
    }

    if (!esDelCurso(pregunta)) return RECHAZO;

    let mejor = null;
    let puntos = 0;

    for (const tema of conocimientos) {
      let actual = 0;

      for (const palabra of tema.palabras) {
        const clave = normalizar(palabra);

        if (texto.includes(clave)) {
          actual += clave.length;
        }
      }

      if (actual > puntos) {
        puntos = actual;
        mejor = tema;
      }
    }

    return mejor
      ? mejor.respuesta
      : "Ese tema pertenece a mis cursos, pero " +
        "necesito la API de IA para darte una " +
        "explicación personalizada.";
  }

  function agregarMensaje(texto, clase) {
    const div = document.createElement("div");
    div.className = "bot-message " + clase;
    div.textContent = texto;
    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
    return div;
  }

  agregarMensaje(
    "¡Hola! Soy RONIN 🤖\n\n" +
    "Tu tutor de Algoritmos y Estructuras de Datos " +
    "y Desarrollo de Aplicaciones.\n\n" +
    "Puedes preguntarme o adjuntar código.",
    "bot-answer"
  );

  let codigoAdjunto = "";

  $("botFile").addEventListener("change", async e => {
    const archivo = e.target.files[0];
    if (!archivo) return;

    if (archivo.size > 100000) {
      codigoAdjunto = "";
      e.target.value = "";
      agregarMensaje(
        "El archivo supera el límite de 100 KB.",
        "bot-answer"
      );
      return;
    }

    codigoAdjunto = await archivo.text();
    status.textContent = "Archivo: " + archivo.name;
  });

  $("botForm").addEventListener("submit", async e => {
    e.preventDefault();

    const pregunta = input.value.trim();
    const codigo = codigoAdjunto;

    if (!pregunta && !codigo) return;

    input.value = "";
    codigoAdjunto = "";
    $("botFile").value = "";

    agregarMensaje(
      pregunta || "Analiza mi archivo",
      "bot-user"
    );

    if (!codigo && !esDelCurso(pregunta) &&
        !/^(hola|buenas|hey|saludos)\b/i.test(pregunta)) {
      agregarMensaje(RECHAZO, "bot-answer");
      return;
    }

    if (!API_URL) {
      let respuesta = responderLocal(pregunta);

      if (codigo) {
        respuesta =
          "He recibido tu archivo de código.\n\n" +
          "Para analizarlo línea por línea y detectar " +
          "errores necesito que conectes la API de IA.";
      }

      agregarMensaje(respuesta, "bot-answer");
      return;
    }

    const salida = agregarMensaje(
      "Analizando tu consulta...",
      "bot-answer"
    );

    try {
      const respuesta = await fetch(API_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          question: pregunta,
          code: codigo
        })
      });

      if (!respuesta.ok) {
        throw new Error("HTTP " + respuesta.status);
      }

      const datos = await respuesta.json();
      salida.textContent =
        datos.answer || "No se recibió respuesta.";

    } catch (error) {
      salida.textContent =
        "No pude conectar con la IA: " + error.message;
    }
  });

  $("botClose").addEventListener("click", () => {
    panel.hidden = true;
  });

  // Robot arrastrable
  let inicio = null;
  let arrastrado = false;
  let bloquearClick = false;

  function iniciar(e) {
    if (e.button !== 0) return;
    if (e.target.closest("#botClose")) return;

    const rect = dock.getBoundingClientRect();

    inicio = {
      x: e.clientX,
      y: e.clientY,
      left: rect.left,
      top: rect.top
    };

    arrastrado = false;
    e.currentTarget.setPointerCapture(e.pointerId);
  }

  function mover(e) {
    if (!inicio) return;

    const dx = e.clientX - inicio.x;
    const dy = e.clientY - inicio.y;

    if (Math.hypot(dx, dy) > 7) {
      arrastrado = true;
    }

    if (!arrastrado) return;

    dock.style.left = Math.max(
      0,
      Math.min(
        innerWidth - dock.offsetWidth,
        inicio.left + dx
      )
    ) + "px";

    dock.style.top = Math.max(
      0,
      Math.min(
        innerHeight - dock.offsetHeight,
        inicio.top + dy
      )
    ) + "px";

    dock.style.right = "auto";
    dock.style.bottom = "auto";
  }

  function terminar() {
    if (arrastrado) bloquearClick = true;
    inicio = null;
  }

  [$("botLauncher"), $("botHandle")].forEach(el => {
    el.addEventListener("pointerdown", iniciar);
    el.addEventListener("pointermove", mover);
    el.addEventListener("pointerup", terminar);
    el.addEventListener("pointercancel", terminar);
  });

  $("botLauncher").addEventListener("click", () => {
    if (bloquearClick) {
      bloquearClick = false;
      return;
    }

    panel.hidden = !panel.hidden;

    if (!panel.hidden) input.focus();
  });

})();
