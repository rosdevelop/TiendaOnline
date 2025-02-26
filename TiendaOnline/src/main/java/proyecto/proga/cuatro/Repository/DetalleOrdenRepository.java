package proyecto.proga.cuatro.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.proga.cuatro.Entities.DetalleOrden;

public interface DetalleOrdenRepository extends JpaRepository<DetalleOrden, Integer> {
}
