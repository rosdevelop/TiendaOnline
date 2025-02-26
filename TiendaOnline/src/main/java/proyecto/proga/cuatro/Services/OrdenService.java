package proyecto.proga.cuatro.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.proga.cuatro.Entities.Cliente;
import proyecto.proga.cuatro.Entities.DetalleOrden;
import proyecto.proga.cuatro.Entities.Orden;
import proyecto.proga.cuatro.Entities.Producto;
import proyecto.proga.cuatro.Repository.DetalleOrdenRepository;
import proyecto.proga.cuatro.Repository.OrdenRepository;
import proyecto.proga.cuatro.Repository.ProductoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrdenService {

    private static final Logger logger = LoggerFactory.getLogger(OrdenService.class);

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private DetalleOrdenRepository detalleOrdenRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Método para crear una nueva orden.
     */
    @Transactional
    public Orden crearOrden(Cliente cliente, Map<Integer, Map<String, Object>> carrito) {
        BigDecimal total = BigDecimal.ZERO;
        List<DetalleOrden> detalles = new ArrayList<>();

        for (Map.Entry<Integer, Map<String, Object>> entry : carrito.entrySet()) {
            Integer productoId = entry.getKey();
            Map<String, Object> data = entry.getValue();
            int cantidad = ((Number) data.get("cantidad")).intValue();
            BigDecimal precio = new BigDecimal(data.get("precio").toString());

            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + productoId));

            if (producto.getCantidadDisponible() < cantidad) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre());
            }

            producto.setCantidadDisponible(producto.getCantidadDisponible() - cantidad);
            productoRepository.save(producto);

            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));
            total = total.add(subtotal);

            DetalleOrden detalle = new DetalleOrden();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precio);
            detalles.add(detalle);
        }

        Orden orden = new Orden();
        orden.setCliente(cliente);
        orden.setFechaOrden(LocalDateTime.now());
        orden.setEstado("Pagado");
        orden.setTotal(total);

        orden = ordenRepository.save(orden);

        for (DetalleOrden det : detalles) {
            det.setOrden(orden);
            detalleOrdenRepository.save(det);
        }

        orden.setDetalles(detalles);

        logger.info("Orden creada: ID = " + orden.getOrdenId() + ", Total = " + orden.getTotal());

        return orden;
    }

    /**
     * Método para obtener órdenes por cliente, incluyendo sus detalles.
     */
    public List<Orden> obtenerOrdenesPorCliente(Cliente cliente) {
        List<Orden> ordenes = ordenRepository.findByClienteWithDetalles(cliente);
        for (Orden orden : ordenes) {
            int numeroDetalles = (orden.getDetalles() != null) ? orden.getDetalles().size() : 0;
            logger.info("Orden ID: " + orden.getOrdenId() + " tiene " + numeroDetalles + " detalles.");
        }
        return ordenes;
    }
}



