package proyecto.proga.cuatro.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import proyecto.proga.cuatro.Entities.Cliente;
import proyecto.proga.cuatro.Repository.ClienteRepository;

import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Autowired
    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Optional<Cliente> autenticarCliente(String username, String contraseña) {
        // Busca el cliente en la base de datos por correo
        Optional<Cliente> clienteOpt = clienteRepository.findByCorreo(username);
        if (!clienteOpt.isPresent()) {
            // Si no encuentra por correo, busca por teléfono
            clienteOpt = clienteRepository.findByTelefono(username);
        }

        // Verifica la contraseña directamente
        if (clienteOpt.isPresent() && clienteOpt.get().getContraseña().equals(contraseña)) {
            return clienteOpt;
        }

        return Optional.empty();
    }

    public Cliente registrarCliente(Cliente cliente) {
        // Guarda al cliente directamente sin cifrar la contraseña
        return clienteRepository.save(cliente);
    }

    public boolean existePorCorreo(String correo) {
        return clienteRepository.findByCorreo(correo).isPresent();
    }

    public boolean existePorTelefono(String telefono) {
        return clienteRepository.findByTelefono(telefono).isPresent();
    }

    public Optional<Cliente> findByEmail(String email) {
        return clienteRepository.findByCorreo(email);
    }

    public Cliente actualizarCliente(Cliente cliente) {
        // Guarda los cambios del cliente, incluyendo la nueva contraseña
        return clienteRepository.save(cliente);
    }
}



