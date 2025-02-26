package proyecto.proga.cuatro.Controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity; 
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import proyecto.proga.cuatro.Entities.Cliente;
import proyecto.proga.cuatro.Entities.Contacto;
import proyecto.proga.cuatro.Entities.DetalleOrden;
import proyecto.proga.cuatro.Entities.Orden;
import proyecto.proga.cuatro.Entities.Producto;
import proyecto.proga.cuatro.DTO.AdminLoginRequest;
import proyecto.proga.cuatro.Entities.Administrador;
import proyecto.proga.cuatro.Services.ClienteService;
import proyecto.proga.cuatro.Services.OrdenService;
import proyecto.proga.cuatro.Services.ProductoService;
import proyecto.proga.cuatro.Repository.OrdenRepository;
import proyecto.proga.cuatro.Services.AdministradorService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;

@Controller
public class AuthController {

    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final AdministradorService administradorService;

    @Autowired
    private OrdenService ordenService;

    @Autowired
    private OrdenRepository ordenRepository;
    
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    public AuthController(ClienteService clienteService, ProductoService productoService, AdministradorService administradorService) {
        this.clienteService = clienteService;
        this.productoService = productoService;
        this.administradorService = administradorService;
    }
    
    /**
     * DTO para la solicitud de recuperación de contraseña.
     */
    static class RecoverPasswordRequest {
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /**
     * DTO para la solicitud de cambio de contraseña.
     */
    static class ChangePasswordRequest {
        private String email;
        private String oldPassword;
        private String newPassword;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    /**
     * Endpoint para manejar la solicitud de recuperación de contraseña.
     */
    @PostMapping("/recover-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recoverPassword(@RequestBody RecoverPasswordRequest request) {
        Map<String, Object> response = new HashMap<>();

        Optional<Cliente> clienteOpt = clienteService.findByEmail(request.getEmail());
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();

            try {
                // Crear el mensaje de correo
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);

                helper.setTo(cliente.getCorreo());
                helper.setSubject("Recuperación de Contraseña");

                String cuerpo = "<p>Hola " + cliente.getNombre() + ",</p>"
                        + "<p>Has solicitado recuperar tu contraseña.</p>"
                        + "<p>Tu contraseña actual es: <strong>" + cliente.getContraseña() + "</strong></p>"
                        + "<p>Si no solicitaste este cambio, por favor contacta con soporte.</p>";

                helper.setText(cuerpo, true);

                // Enviar el correo
                mailSender.send(message);

                response.put("success", true);
                response.put("message", "La contraseña ha sido enviada a tu correo electrónico: " + cliente.getCorreo());
                return ResponseEntity.ok(response);
            } catch (MailException | MessagingException e) {
                e.printStackTrace();
                response.put("success", false);
                response.put("message", "Error al enviar el correo electrónico. Por favor, inténtalo nuevamente.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } else {
            response.put("success", false);
            response.put("message", "No se encontró un usuario con ese correo electrónico.");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Endpoint para manejar el cambio de contraseña.
     */
    @PostMapping("/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody ChangePasswordRequest request) {
        Map<String, Object> response = new HashMap<>();

        Optional<Cliente> clienteOpt = clienteService.findByEmail(request.getEmail());
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();

            // Verificar la contraseña antigua
            if (!cliente.getContraseña().equals(request.getOldPassword())) {
                response.put("success", false);
                response.put("message", "La contraseña antigua es incorrecta.");
                return ResponseEntity.ok(response);
            }

            // Actualizar la contraseña
            cliente.setContraseña(request.getNewPassword());
            clienteService.actualizarCliente(cliente);

            // Enviar correo de confirmación de cambio de contraseña
            try {
                sendPasswordChangeConfirmationEmail(cliente);
            } catch (MailException | MessagingException e) {
                e.printStackTrace();
                // Aunque la contraseña se haya cambiado, notificamos que hubo un error al enviar el correo
                response.put("success", false);
                response.put("message", "Contraseña actualizada, pero hubo un error al enviar el correo de confirmación.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            response.put("success", true);
            response.put("message", "La contraseña ha sido actualizada exitosamente y se ha enviado un correo de confirmación.");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "No se encontró un usuario con ese correo electrónico.");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Método para enviar un correo de confirmación de cambio de contraseña.
     */
    private void sendPasswordChangeConfirmationEmail(Cliente cliente) throws MailException, MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(cliente.getCorreo());
        helper.setSubject("Confirmación de Cambio de Contraseña");

        String cuerpo = "<p>Hola " + cliente.getNombre() + ",</p>"
                + "<p>Este es un correo de confirmación para informarte que tu contraseña ha sido cambiada exitosamente.</p>"
                + "<p>Si no realizaste este cambio, por favor contacta con soporte inmediatamente.</p>";

        helper.setText(cuerpo, true);

        // Enviar el correo
        mailSender.send(message);
    } 

    /**
     * Método global para agregar atributos comunes al modelo.
     * Esto asegura que 'usuarioAutenticado' y 'isAdminAuthenticated' estén disponibles en todas las vistas.
     */
    @ModelAttribute
    public void agregarAtributosComunes(Model model, HttpSession session) {
        Cliente usuarioAutenticado = (Cliente) session.getAttribute("usuarioAutenticado");
        model.addAttribute("usuarioAutenticado", usuarioAutenticado != null ? usuarioAutenticado.getNombre() : null);
        
        Administrador adminAutenticado = (Administrador) session.getAttribute("adminAutenticado");
        model.addAttribute("isAdminAuthenticated", adminAutenticado != null);
        model.addAttribute("adminNombre", adminAutenticado != null ? adminAutenticado.getNombre() : "");
    }

    /**
     * Endpoint para mostrar la página de inicio.
     */
    @GetMapping("/")
    public String mostrarInicio(Model model, HttpSession session) {
        // 'usuarioAutenticado' ya está agregado por @ModelAttribute
        model.addAttribute("currentPage", "inicio");

        // Obtener productos y codificar imágenes en Base64
        List<Producto> productos = productoService.obtenerTodosLosProductos().stream().map(producto -> {
            if (producto.getImagen() != null) {
                producto.setImagenBase64(Base64.getEncoder().encodeToString(producto.getImagen()));
            }
            return producto;
        }).collect(Collectors.toList());
        model.addAttribute("productos", productos);

        // Obtener categorías únicas
        List<String> categorias = productoService.obtenerTodasLasCategorias();
        model.addAttribute("categorias", categorias);

        return "index";
    }

    /**
     * Endpoint para buscar productos por nombre.
     */
    @GetMapping("/productos/buscar")
    public String buscarProductos(@RequestParam("nombre") String nombre, Model model, HttpSession session) {
        // 'usuarioAutenticado' ya está agregado por @ModelAttribute
        model.addAttribute("currentPage", "busqueda");

        List<Producto> productos = productoService.buscarProductosPorNombre(nombre).stream().map(producto -> {
            if (producto.getImagen() != null) {
                producto.setImagenBase64(Base64.getEncoder().encodeToString(producto.getImagen()));
            }
            return producto;
        }).collect(Collectors.toList());
        model.addAttribute("productos", productos);

        List<String> categorias = productoService.obtenerTodasLasCategorias();
        model.addAttribute("categorias", categorias);

        return "index";
    }

    /**
     * Endpoint para filtrar productos por categoría.
     */
    @GetMapping("/productos/filtrar")
    public String filtrarProductosPorCategoria(@RequestParam("categoria") String categoria, Model model, HttpSession session) {
        // 'usuarioAutenticado' ya está agregado por @ModelAttribute
        model.addAttribute("currentPage", "filtro");

        List<Producto> productos = productoService.buscarProductosPorCategoria(categoria).stream().map(producto -> {
            if (producto.getImagen() != null) {
                producto.setImagenBase64(Base64.getEncoder().encodeToString(producto.getImagen()));
            }
            return producto;
        }).collect(Collectors.toList());
        model.addAttribute("productos", productos);

        List<String> categorias = productoService.obtenerTodasLasCategorias();
        model.addAttribute("categorias", categorias);

        return "index";
    }
    
    /**
     * Endpoint para mostrar la página "Nosotros".
     */
    @GetMapping("/nosotros")
    public String mostrarNosotros(Model model){
        model.addAttribute("currentPage", "nosotros");
        return "nosotros";
    }

    /**
     * Endpoint para procesar el inicio de sesión de clientes.
     */
    @PostMapping("/login")
    public String procesarLogin(@RequestParam("username") String username,
                                 @RequestParam("password") String password,
                                 Model model,
                                 HttpSession session) {
        Optional<Cliente> clienteOpt = clienteService.autenticarCliente(username, password);

        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();
            session.setAttribute("usuarioAutenticado", cliente);
            return "redirect:/";
        }

        model.addAttribute("errorLogin", "Correo o contraseña incorrectos");
        model.addAttribute("currentPage", "inicio");
        return "index";
    }

    /**
     * Endpoint para procesar el cierre de sesión de clientes.
     */
    @PostMapping("/logout")
    public String procesarLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/?logout";
    }

    /**
     * Endpoint para mostrar el formulario de registro.
     */
    @GetMapping("/registrarse")
    public String mostrarFormularioRegistro(Model model) {
        // 'usuarioAutenticado' ya está agregado por @ModelAttribute
        model.addAttribute("currentPage", "registrarse");
        model.addAttribute("cliente", new Cliente());
        return "register";
    }

    /**
     * Endpoint para procesar el registro de nuevos clientes.
     */
    @PostMapping("/registrarse")
    public String procesarRegistro(@ModelAttribute Cliente cliente, Model model, RedirectAttributes redirectAttributes) {
        // 'usuarioAutenticado' ya está agregado por @ModelAttribute
        model.addAttribute("currentPage", "registrarse");

        if (clienteService.existePorCorreo(cliente.getCorreo())) {
            model.addAttribute("errorRegistro", "El correo electrónico ya está en uso.");
            return "register";
        }

        if (cliente.getTelefono() != null && !cliente.getTelefono().isEmpty() &&
                clienteService.existePorTelefono(cliente.getTelefono())) {
            model.addAttribute("errorRegistroTelefono", "El teléfono ya está en uso.");
            return "register";
        }

        clienteService.registrarCliente(cliente);
        return "redirect:/?registerSuccess";
    }

    /**
     * Endpoint para ver los detalles de un producto.
     */
    @GetMapping("/productos/{id}")
    public String verDetallesProducto(@PathVariable("id") Integer id, Model modelo) {
        // 'usuarioAutenticado' ya está agregado por @ModelAttribute
        modelo.addAttribute("currentPage", "detallesProducto");

        Producto producto = productoService.obtenerProductoPorId(id).orElse(null);
        if (producto != null && producto.getImagen() != null) {
            producto.setImagenBase64(Base64.getEncoder().encodeToString(producto.getImagen()));
        }
        modelo.addAttribute("producto", producto);
        return "detallesProducto";
    }

    /**
     * Endpoint para mostrar la administración de productos.
     */
    @GetMapping("/admin/productos")
    public String mostrarProductosAdmin(Model model) {
        // 'isAdminAuthenticated' y 'adminNombre' ya están agregados por @ModelAttribute
        model.addAttribute("currentPage", "adminProductos");

        List<Producto> productos = productoService.obtenerTodosLosProductos().stream().map(producto -> {
            if (producto.getImagen() != null) {
                producto.setImagenBase64(Base64.getEncoder().encodeToString(producto.getImagen()));
            }
            return producto;
        }).collect(Collectors.toList());
        model.addAttribute("productos", productos);
        model.addAttribute("nuevoProducto", new Producto());

        // Obtener categorías únicas
        List<String> categorias = productoService.obtenerTodasLasCategorias();
        model.addAttribute("categorias", categorias);

        return "adminProductos";
    }

    /**
     * Endpoint para agregar un nuevo producto.
     */
    @PostMapping("/admin/productos/agregar")
    public String agregarProducto(
            @ModelAttribute Producto producto,
            @RequestParam("imagenArchivo") MultipartFile imagenArchivo,
            RedirectAttributes redirectAttributes,
            Model model) {

        // 'isAdminAuthenticated' y 'adminNombre' ya están agregados por @ModelAttribute
        model.addAttribute("currentPage", "adminProductos");

        if (!imagenArchivo.isEmpty()) {
            try {
                producto.setImagen(imagenArchivo.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error", "Error al cargar la imagen.");
                return "redirect:/admin/productos";
            }
        }

        if (producto.getCategoria() == null || producto.getCategoria().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Debe seleccionar una categoría.");
            return "redirect:/admin/productos";
        }

        productoService.guardarProducto(producto);
        redirectAttributes.addFlashAttribute("success", "Producto agregado correctamente.");
        return "redirect:/admin/productos";
    }

    /**
     * Endpoint para eliminar un producto por su ID.
     */
    @PostMapping("/admin/productos/eliminar/{id}")
    public String eliminarProducto(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        // 'isAdminAuthenticated' y 'adminNombre' ya están agregados por @ModelAttribute

        productoService.eliminarProducto(id);
        redirectAttributes.addFlashAttribute("success", "Producto eliminado correctamente.");
        return "redirect:/admin/productos";
    }

    /**
     * Método para generar el PDF de la factura de una orden.
     */
    private byte[] generarPdfFactura(Orden orden) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Factura - Orden #" + orden.getOrdenId()));
            document.add(new Paragraph("Cliente: " + orden.getCliente().getNombre()));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            document.add(new Paragraph("Fecha: " + orden.getFechaOrden().format(formatter)));
            document.add(new Paragraph("Estado: " + orden.getEstado()));
            document.add(new Paragraph(" "));

            for (DetalleOrden det : orden.getDetalles()) {
                BigDecimal subtotal = det.getPrecioUnitario().multiply(new BigDecimal(det.getCantidad()));
                String linea = det.getProducto().getNombre() + " x "
                        + det.getCantidad() + " = ₡ "
                        + subtotal.toString();
                document.add(new Paragraph(linea));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total: ₡ " + orden.getTotal().toString()));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Endpoint para procesar el pago.
     */
    @PostMapping(value = "/pagar", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<byte[]> procesarPago(@RequestBody Map<Integer, Map<String, Object>> carrito, HttpSession session) {
        Cliente usuarioAutenticado = (Cliente) session.getAttribute("usuarioAutenticado");
        if (usuarioAutenticado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        try {
            Orden orden = ordenService.crearOrden(usuarioAutenticado, carrito);

            // Recargar la orden con detalles completos
            orden = ordenRepository.findById(orden.getOrdenId())
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada después de crearla"));

            byte[] pdfBytes = generarPdfFactura(orden);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename("Factura-Orden-" + orden.getOrdenId() + ".pdf").build());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Endpoint para mostrar el historial de compras del usuario autenticado.
     */
    @GetMapping("/historial")
    public String mostrarHistorialCompras(Model model, HttpSession session) {
        // 'usuarioAutenticado' ya está agregado por @ModelAttribute
        model.addAttribute("currentPage", "compras");

        Cliente usuarioAutenticado = (Cliente) session.getAttribute("usuarioAutenticado");
        if (usuarioAutenticado == null) {
            return "redirect:/";
        }

        List<Orden> ordenes = ordenService.obtenerOrdenesPorCliente(usuarioAutenticado);
        model.addAttribute("ordenes", ordenes);
        return "historial";
    }

    /**
     * Endpoint para mostrar la página de contacto.
     */
    @GetMapping("/contacto")
    public String contacto(Model model) {
        model.addAttribute("currentPage", "contacto");
        return "contacto"; 
    }

    /**
     * Endpoint para manejar la solicitud de contacto.
     */
    @PostMapping("/contacto/enviar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enviarContacto(@RequestBody Contacto contacto) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Crear el mensaje de correo
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // Destinatario (puede ser tu propio correo o el de soporte)
            helper.setTo("proyectoprogramacion2024@gmail.com");
            helper.setSubject("Nuevo Mensaje de Contacto de " + contacto.getNombre());

            // Cuerpo del correo en formato HTML
            String cuerpo = "<h3>Nuevo Mensaje de Contacto</h3>"
                    + "<p><strong>Nombre:</strong> " + contacto.getNombre() + "</p>"
                    + "<p><strong>Correo:</strong> " + contacto.getCorreo() + "</p>"
                    + "<p><strong>Teléfono:</strong> " + contacto.getTelefono() + "</p>"
                    + "<p><strong>Mensaje:</strong><br/>" + contacto.getMensaje().replaceAll("\n", "<br/>") + "</p>";

            helper.setText(cuerpo, true);

            // Enviar el correo
            mailSender.send(message);

            // Respuesta de éxito
            response.put("exito", true);
            response.put("mensaje", "¡Tu mensaje ha sido enviado exitosamente!");

            return ResponseEntity.ok(response);

        } catch (MailException | MessagingException e) {
            e.printStackTrace();
            // Respuesta de error
            response.put("exito", false);
            response.put("mensaje", "Hubo un error al enviar tu mensaje. Por favor, inténtalo nuevamente más tarde.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Método para enviar un correo de confirmación de contacto.
     */
    private void sendContactoConfirmationEmail(Contacto contacto) throws MailException, MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(contacto.getCorreo());
        helper.setSubject("Confirmación de Recepción de tu Mensaje");

        String cuerpo = "<p>Hola " + contacto.getNombre() + ",</p>"
                + "<p>Hemos recibido tu mensaje y nos pondremos en contacto contigo pronto.</p>"
                + "<p>Gracias por comunicarte con nosotros.</p>";

        helper.setText(cuerpo, true);

        // Enviar el correo
        mailSender.send(message);
    }

    /**
     * Endpoint para procesar el inicio de sesión de administradores.
     */
    @PostMapping("/admin/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loginAdministrador(@RequestBody AdminLoginRequest request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        Optional<Administrador> adminOpt = administradorService.findByCorreo(request.getCorreo());
        if (adminOpt.isPresent()) {
            Administrador admin = adminOpt.get();

            // Verificar la contraseña en texto plano
            if (admin.getContraseña().equals(request.getContraseña())) {
                // Establecer atributo de sesión para indicar que el admin está autenticado
                session.setAttribute("adminAutenticado", admin);

                response.put("success", true);
                response.put("message", "Inicio de sesión exitoso.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Contraseña incorrecta.");
                return ResponseEntity.ok(response);
            }
        } else {
            response.put("success", false);
            response.put("message", "No se encontró un administrador con ese correo electrónico.");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Endpoint para procesar el cierre de sesión de administradores.
     */
    @PostMapping("/admin/logout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logoutAdministrador(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        session.invalidate();
        response.put("success", true);
        response.put("message", "Cierre de sesión exitoso.");
        return ResponseEntity.ok(response);
    }
}

