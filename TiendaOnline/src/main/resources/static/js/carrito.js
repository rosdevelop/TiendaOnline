const usuarioAutenticado = window.usuarioAutenticado || false;
const correoUsuario = window.correoUsuario || 'correo@example.com';

let carrito = {};
let total = 0;

// Función para guardar el carrito en localStorage
function guardarCarrito() {
    localStorage.setItem('carrito', JSON.stringify(carrito));
}

// Función para cargar el carrito desde localStorage
function cargarCarrito() {
    const carritoGuardado = localStorage.getItem('carrito');
    if (carritoGuardado) {
        carrito = JSON.parse(carritoGuardado);
        actualizarCarrito();
    }
}

// Función para mostrar una alerta de Bootstrap
function showBootstrapAlert(message, type='success') {
    const alertContainer = document.getElementById('alertContainer');
    const alertId = 'alert-' + Date.now();

    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.role = 'alert';
    alertDiv.id = alertId;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;
    alertContainer.appendChild(alertDiv);

    // Eliminar la alerta automáticamente después de 3 segundos
    setTimeout(() => {
        const alertElement = document.getElementById(alertId);
        if (alertElement) {
            const alert = bootstrap.Alert.getOrCreateInstance(alertElement);
            alert.close();
        }
    }, 3000);
}

// Función para agregar productos al carrito
function agregarProducto(button) {
    console.log("Función agregarProducto llamada");
    if (!usuarioAutenticado) {
        // Obtener el modal actual (modal de detalles del producto)
        const currentModalElement = button.closest('.modal');
        if (currentModalElement) {
            // Obtener la instancia del modal actual
            const currentModal = bootstrap.Modal.getInstance(currentModalElement);
            if (currentModal) {
                // Escuchar el evento 'hidden.bs.modal' para abrir el modal de login después de que se cierre el modal actual
                currentModalElement.addEventListener('hidden.bs.modal', function handler() {
                    // Mostrar el modal de login
                    var loginModal = new bootstrap.Modal(document.getElementById('loginModal'));
                    loginModal.show();

                    // Remover el event listener para evitar múltiples llamadas
                    currentModalElement.removeEventListener('hidden.bs.modal', handler);
                });

                // Cerrar el modal actual
                currentModal.hide();
            } else {
                // Si no se puede obtener la instancia del modal, simplemente mostrar el modal de login
                var loginModal = new bootstrap.Modal(document.getElementById('loginModal'));
                loginModal.show();
            }
        } else {
            // Si no se encuentra el modal actual, simplemente mostrar el modal de login
            var loginModal = new bootstrap.Modal(document.getElementById('loginModal'));
            loginModal.show();
        }
        return;
    }

    const productoId = button.getAttribute('data-producto-id'); 
    console.log("Producto ID:", productoId);
    const cantidadInput = document.getElementById('cantidad-' + productoId);
    console.log("Cantidad Input:", cantidadInput);

    if (!cantidadInput) {
        showBootstrapAlert('No se pudo encontrar el campo de cantidad. Verifica que el ID del producto sea correcto.', 'warning');
        return;
    }

    let cantidad = parseInt(cantidadInput.value, 10); 
    console.log("Cantidad seleccionada:", cantidad);
    if (isNaN(cantidad) || cantidad <= 0) {
        showBootstrapAlert('La cantidad debe ser mayor a 0.', 'warning');
        return;
    }

    // Encontrar la tarjeta .card dentro del contenedor
    const card = button.closest('.card');
    if (!card) {
        showBootstrapAlert('No se encontró la tarjeta del producto. Verifica la estructura del HTML.', 'warning');
        return;
    }

    // Obtener y parsear el precio correctamente
    const precioElement = card.querySelector('.precio');
    if (!precioElement) {
        showBootstrapAlert('No se pudo encontrar el elemento del precio.', 'warning');
        return;
    }

    const precioText = precioElement.textContent;
    console.log("Precio Text:", precioText);
    const precioSanitized = precioText.replace(/[^0-9.]/g, '');
    console.log("Precio Sanitizado:", precioSanitized);
    const precio = parseFloat(precioSanitized);
    console.log("Precio Parseado:", precio);

    if (isNaN(precio)) {
        showBootstrapAlert('Error al obtener el precio del producto.', 'danger');
        return;
    }

    const stock = parseInt(card.getAttribute('data-stock'), 10);
    console.log("Stock Disponible:", stock);
    const nombre = card.querySelector('.card-title').textContent; 
    console.log("Nombre del Producto:", nombre);

    if (isNaN(stock)) {
        showBootstrapAlert('Error al obtener el stock del producto.', 'danger');
        return;
    }

    if (carrito[productoId]) {
        if (carrito[productoId].cantidad + cantidad > stock) {
            showBootstrapAlert('No puedes agregar más de lo disponible.', 'danger');
            return;
        }
        carrito[productoId].cantidad += cantidad;
    } else {
        carrito[productoId] = { nombre, precio, cantidad, stock };
    }

    actualizarCarrito();
    guardarCarrito(); // Guardar el carrito actualizado
    const modalElement = button.closest('.modal');
    const modal = bootstrap.Modal.getInstance(modalElement);
    modal.hide();
    showBootstrapAlert('Producto agregado al carrito!', 'success');
}

// Función para eliminar un producto del carrito
function eliminarProducto(productoId) {
    if (carrito[productoId]) {
        // Eliminar el producto del objeto carrito
        delete carrito[productoId];

        // Actualizar la interfaz de usuario
        actualizarCarrito();

        // Guardar los cambios en localStorage
        guardarCarrito();

        showBootstrapAlert('Producto eliminado del carrito.', 'info');
    } else {
        showBootstrapAlert('El producto no existe en el carrito.', 'warning');
    }
}

// Actualiza el contenido visual del carrito
function actualizarCarrito() {
    const carritoItems = document.getElementById('carritoItems');
    const carritoContador = document.getElementById('carritoContador');
    const carritoTotal = document.getElementById('carritoTotal');

    carritoItems.innerHTML = '';
    total = 0;

    Object.keys(carrito).forEach(id => {
        const item = carrito[id];
        const subtotal = item.precio * item.cantidad;
        total += subtotal;

        // Crear un elemento div para el producto
        const itemDiv = document.createElement('div');
        itemDiv.classList.add('list-group-item', 'd-flex', 'justify-content-between', 'align-items-center');

        // Contenido del producto
        const contenido = document.createElement('div');
        contenido.textContent = `${item.nombre} - ₡ ${item.precio.toFixed(2)} x ${item.cantidad} = ₡ ${subtotal.toFixed(2)}`;

        // Botón de eliminación
        const btnEliminar = document.createElement('button');
        btnEliminar.classList.add('btn', 'btn-danger', 'btn-sm');
        btnEliminar.textContent = 'Eliminar';
        btnEliminar.setAttribute('data-producto-id', id);
        btnEliminar.onclick = function() {
            eliminarProducto(id);
        };

        // Agregar contenido y botón al div del producto
        itemDiv.appendChild(contenido);
        itemDiv.appendChild(btnEliminar);

        // Agregar el div del producto al carritoItems
        carritoItems.appendChild(itemDiv);
    });

    carritoContador.textContent = Object.keys(carrito).reduce((acc, id) => acc + carrito[id].cantidad, 0);
    carritoTotal.textContent = isNaN(total) ? '0.00' : total.toFixed(2);
}

// Función para simular el pago
function pagarCarrito() {
    if (Object.keys(carrito).length === 0) {
        showBootstrapAlert('El carrito está vacío.', 'warning');
        return;
    }

    fetch('/pagar', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(carrito)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Error al procesar el pago");
        }
        return response.blob();
    })
    .then(blob => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'Factura.pdf';
        link.click();

        carrito = {};
        actualizarCarrito();
        guardarCarrito();

        showBootstrapAlert('Compra realizada exitosamente. Se ha descargado tu factura.', 'success');
    })
    .catch(err => {
        console.error(err);
        showBootstrapAlert('Hubo un error al procesar el pago.', 'danger');
    });
}

// Cargar el carrito al cargar la página
document.addEventListener('DOMContentLoaded', function() {
    cargarCarrito();
});

