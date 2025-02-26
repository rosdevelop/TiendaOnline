package proyecto.proga.cuatro.Services;

import proyecto.proga.cuatro.Entities.Producto;
import proyecto.proga.cuatro.Repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    // Obtener todos los productos
    public List<Producto> obtenerTodosLosProductos() {
        return productoRepository.findAll();
    }

    // Obtener un producto por ID
    public Optional<Producto> obtenerProductoPorId(Integer id) {
        return productoRepository.findById(id);
    }

    // Guardar un nuevo producto
    public Producto guardarProducto(Producto producto) {
        return productoRepository.save(producto);
    }

    // Eliminar un producto por ID
    public void eliminarProducto(Integer id) {
        productoRepository.deleteById(id);
    }

    // Obtener todas las categorías únicas
    public List<String> obtenerTodasLasCategorias() {
        return productoRepository.findAll()
                .stream()
                .map(Producto::getCategoria) // Mapeo a categoría
                .distinct() // Eliminar duplicados
                .sorted() // Ordenar alfabéticamente
                .collect(Collectors.toList());
    }

    // Buscar productos por nombre
    public List<Producto> buscarProductosPorNombre(String nombre) {
        return productoRepository.findAll()
                .stream()
                .filter(producto -> producto.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .collect(Collectors.toList());
    }

    // Buscar productos por categoría
    public List<Producto> buscarProductosPorCategoria(String categoria) {
        return productoRepository.findAll()
                .stream()
                .filter(producto -> producto.getCategoria().equalsIgnoreCase(categoria))
                .collect(Collectors.toList());
    }
}
