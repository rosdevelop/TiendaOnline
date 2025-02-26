package proyecto.proga.cuatro.Repository;

import proyecto.proga.cuatro.Entities.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
}
