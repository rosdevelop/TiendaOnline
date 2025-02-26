//JavaScript para manejar las solicitudes AJAX 
document.addEventListener('DOMContentLoaded', function () {
                // Inicializar instancias de Bootstrap Modal
                const loginModal = new bootstrap.Modal(document.getElementById('loginModal'));
                const recoverPasswordModal = new bootstrap.Modal(document.getElementById('recoverPasswordModal'));
                const changePasswordModal = new bootstrap.Modal(document.getElementById('changePasswordModal'));

                // Variable para almacenar el correo electrónico del usuario
                let userEmail = '';

                // Manejar el clic en "¿Has olvidado la contraseña?"
                const forgotPasswordLink = document.getElementById('forgotPasswordLink');
                forgotPasswordLink.addEventListener('click', function (e) {
                    e.preventDefault();
                    // Cerrar el modal de login si está abierto
                    loginModal.hide();
                    // Abrir el modal de recuperar contraseña
                    recoverPasswordModal.show();
                });

                // Manejar la solicitud de recuperación de contraseña
                const recoverPasswordForm = document.getElementById('recoverPasswordForm');
                const recoverAlert = document.getElementById('recoverAlert');

                recoverPasswordForm.addEventListener('submit', function (e) {
                    e.preventDefault();
                    const email = document.getElementById('recoverEmail').value;
                    userEmail = email; // Guardar el correo electrónico para el cambio de contraseña

                    fetch('/recover-password', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ email: email })
                    })
                    .then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            recoverAlert.classList.remove('d-none', 'alert-danger');
                            recoverAlert.classList.add('alert-success');
                            recoverAlert.textContent = data.message;

                            // Mostrar alerta y luego abrir el modal de cambiar contraseña
                            setTimeout(() => {
                                recoverAlert.classList.remove('alert-success');
                                recoverAlert.classList.add('d-none');

                                // Cerrar el modal de recuperación
                                recoverPasswordModal.hide();

                                // Establecer el correo electrónico en el formulario de cambio de contraseña
                                document.getElementById('changeEmail').value = userEmail;

                                // Mostrar el modal de cambio de contraseña
                                changePasswordModal.show();
                            }, 2000);
                        } else {
                            recoverAlert.classList.remove('d-none', 'alert-success');
                            recoverAlert.classList.add('alert-danger');
                            recoverAlert.textContent = data.message;
                        }
                    })
                    .catch(error => {
                        console.error('Error:', error);
                        recoverAlert.classList.remove('d-none', 'alert-success');
                        recoverAlert.classList.add('alert-danger');
                        recoverAlert.textContent = 'Ocurrió un error. Por favor, inténtalo nuevamente.';
                    });
                });

                // Manejar la solicitud de cambio de contraseña
                const changePasswordForm = document.getElementById('changePasswordForm');
                const changePasswordAlert = document.getElementById('changePasswordAlert');

                changePasswordForm.addEventListener('submit', function (e) {
                    e.preventDefault();
                    const email = document.getElementById('changeEmail').value;
                    const oldPassword = document.getElementById('oldPassword').value;
                    const newPassword = document.getElementById('newPassword').value;
                    const confirmPassword = document.getElementById('confirmPassword').value;

                    if (newPassword !== confirmPassword) {
                        changePasswordAlert.classList.remove('d-none', 'alert-success');
                        changePasswordAlert.classList.add('alert-danger');
                        changePasswordAlert.textContent = 'Las nuevas contraseñas no coinciden.';
                        return;
                    }

                    fetch('/change-password', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ email: email, oldPassword: oldPassword, newPassword: newPassword })
                    })
                    .then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            changePasswordAlert.classList.remove('d-none', 'alert-danger');
                            changePasswordAlert.classList.add('alert-success');
                            changePasswordAlert.textContent = data.message;

                            // Cerrar el modal después de un breve retraso
                            setTimeout(() => {
                                changePasswordAlert.classList.remove('alert-success');
                                changePasswordAlert.classList.add('d-none');
                                changePasswordForm.reset();
                                changePasswordModal.hide();
                            }, 2000);
                        } else {
                            changePasswordAlert.classList.remove('d-none', 'alert-success');
                            changePasswordAlert.classList.add('alert-danger');
                            changePasswordAlert.textContent = data.message;
                        }
                    })
                    .catch(error => {
                        console.error('Error:', error);
                        changePasswordAlert.classList.remove('d-none', 'alert-success');
                        changePasswordAlert.classList.add('alert-danger');
                        changePasswordAlert.textContent = 'Ocurrió un error. Por favor, inténtalo nuevamente.';
                    });
                });

                //Manejar el cierre de los modales y limpiar formularios/alertas
                document.getElementById('recoverPasswordModal').addEventListener('hidden.bs.modal', function () {
                    recoverPasswordForm.reset();
                    recoverAlert.classList.add('d-none');
                });

                document.getElementById('changePasswordModal').addEventListener('hidden.bs.modal', function () {
                    changePasswordForm.reset();
                    changePasswordAlert.classList.add('d-none');
                });

                document.getElementById('loginModal').addEventListener('hidden.bs.modal', function () {
                    // Limpiar formularios o alertas si los hay
                });
            });