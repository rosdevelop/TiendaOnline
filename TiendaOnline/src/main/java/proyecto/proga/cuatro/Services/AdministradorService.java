package proyecto.proga.cuatro.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import proyecto.proga.cuatro.Entities.Administrador;
import proyecto.proga.cuatro.Repository.AdministradorRepository;

import java.util.Optional;

@Service
public class AdministradorService {

    private final AdministradorRepository administradorRepository;

    @Autowired
    public AdministradorService(AdministradorRepository administradorRepository) {
        this.administradorRepository = administradorRepository;
    }

    /**
     * Encuentra un administrador por su correo electrónico.
     *
     * @param correo Correo electrónico del administrador.
     * @return Optional de Administrador.
     */
    public Optional<Administrador> findByCorreo(String correo) {
        return administradorRepository.findByCorreo(correo);
    }

    /**
     * Guarda un nuevo administrador en la base de datos.
     *
     * @param administrador Objeto Administrador a guardar.
     * @return Administrador guardado.
     */
    public Administrador saveAdministrador(Administrador administrador) {
        return administradorRepository.save(administrador);
    }

}

