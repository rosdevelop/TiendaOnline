package proyecto.proga.cuatro.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import proyecto.proga.cuatro.Entities.Cliente;
import proyecto.proga.cuatro.Entities.Orden;

import java.util.List;

public interface OrdenRepository extends JpaRepository<Orden, Integer> {
    
    // Método existente para encontrar órdenes por cliente
    List<Orden> findByCliente(Cliente cliente);
    
    // Método personalizado para cargar detalles con las órdenes
    @Query("SELECT DISTINCT o FROM Orden o LEFT JOIN FETCH o.detalles WHERE o.cliente = :cliente")
    List<Orden> findByClienteWithDetalles(@Param("cliente") Cliente cliente);
}
