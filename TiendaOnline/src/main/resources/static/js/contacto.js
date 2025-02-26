document.addEventListener('DOMContentLoaded', function() {
    const formulario = document.getElementById('contactoForm');

    formulario.addEventListener('submit', function(event) {
        event.preventDefault(); // Evita el envío tradicional del formulario

        // Capturar los valores de los campos
        const nombre = document.getElementById('nombre').value.trim();
        const correo = document.getElementById('correo').value.trim();
        const telefono = document.getElementById('telefono').value.trim();
        const mensaje = document.getElementById('mensaje').value.trim();

        // Validaciones básicas
        if (!nombre || !correo || !telefono || !mensaje) {
            showBootstrapAlert('Por favor, completa todos los campos.', 'warning');
            return;
        }

        // Crear el objeto con los datos del formulario
        const datosContacto = {
            nombre: nombre,
            correo: correo,
            telefono: telefono,
            mensaje: mensaje
        };

        // Enviar los datos al backend mediante Fetch API
        fetch('/contacto/enviar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(datosContacto)
        })
        .then(response => response.json())
        .then(data => {
            if (data.exito) {
                showBootstrapAlert(data.mensaje, 'success');
                formulario.reset(); // Limpiar el formulario
            } else {
                showBootstrapAlert(data.mensaje, 'danger');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showBootstrapAlert('Hubo un error al enviar el mensaje. Inténtalo nuevamente más tarde.', 'danger');
        });
    });
});

/**
 * Función para mostrar una alerta de Bootstrap
 * @param {string} message - El mensaje a mostrar
 * @param {string} type - El tipo de alerta (success, danger, warning, info)
 */
function showBootstrapAlert(message, type='success') {
    const alertContainer = document.getElementById('alertContainer');
    const alertId = 'alert-' + Date.now();

    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.role = 'alert';
    alertDiv.id = alertId;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Cerrar"></button>
    `;
    alertContainer.appendChild(alertDiv);

    // Eliminar la alerta automáticamente después de 5 segundos
    setTimeout(() => {
        const alertElement = document.getElementById(alertId);
        if (alertElement) {
            const alert = bootstrap.Alert.getOrCreateInstance(alertElement);
            alert.close();
        }
    }, 5000);
}
