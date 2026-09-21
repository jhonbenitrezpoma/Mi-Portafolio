function guardarNuevaTareaPropietario(e) {
    e.preventDefault();
    const numSemana = parseInt(document.getElementById("modalKeyTarea").value);
    const titulo = document.getElementById("taskTitulo").value.trim();
    const tipo = document.getElementById("taskTipo").value;
    const btnSubmit = document.getElementById("btnGuardarTareaSubmit");

    btnSubmit.innerText = "⏳ Guardando en la nube...";
    btnSubmit.disabled = true;

    // Temporizador de seguridad para evitar bloqueos infinitos
    const timeoutId = setTimeout(() => {
        alert("La conexión con la nube tardó demasiado. Si estás subiendo un PDF/Word pesado, por favor selecciona la opción 'Enlace Web' usando Google Drive.");
        btnSubmit.innerText = "Guardar Tarea en la Nube";
        btnSubmit.disabled = false;
    }, 6000);

    const resetBtn = () => {
        clearTimeout(timeoutId);
        btnSubmit.innerText = "Guardar Tarea en la Nube";
        btnSubmit.disabled = false;
    };

    if (tipo === 'archivo') {
        const fileInput = document.getElementById("taskFile");
        if (fileInput.files && fileInput.files[0]) {
            const archivo = fileInput.files[0];

            // Restricción rigurosa a menos de 300 KB para evitar bloqueos en Firestore
            if (archivo.size > 300 * 1024) {
                clearTimeout(timeoutId);
                alert("El archivo PDF/Word supera los 300 KB. Firestore no puede procesar archivos pesados directamente en la base de datos. Por favor, selecciona 'Enlace Web' y pega el link de tu archivo subido a Google Drive.");
                resetBtn();
                return;
            }

            const reader = new FileReader();
            reader.onload = function(evt) {
                const fileDataUrl = evt.target.result;

                db.collection("tareas").add({
                    curso: cursoActual,
                    unidad: unidadActual,
                    semana: numSemana,
                    nombre: `${titulo} (${archivo.name})`,
                    url: fileDataUrl,
                    tipo: 'archivo',
                    fecha: new Date()
                }).then(() => {
                    resetBtn();
                    cerrarModalSubirTarea();
                    renderizarUnidad();
                }).catch(err => {
                    resetBtn();
                    alert("Error al guardar en Firestore: " + err.message);
                });
            };
            reader.readAsDataURL(archivo);
        } else {
            resetBtn();
            alert("Selecciona un archivo de tu equipo.");
        }
    } else {
        const url = document.getElementById("taskUrl").value.trim();
        db.collection("tareas").add({
            curso: cursoActual,
            unidad: unidadActual,
            semana: numSemana,
            nombre: titulo,
            url: url,
            tipo: 'enlace',
            fecha: new Date()
        }).then(() => {
            resetBtn();
            cerrarModalSubirTarea();
            renderizarUnidad();
        }).catch(err => {
            resetBtn();
            alert("Error al guardar enlace: " + err.message);
        });
    }
}
