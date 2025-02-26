CREATE DATABASE tecnosmart_db;

use tecnosmart_db;
-- Esquema de la base de datos `tecnosmart_db`
select * from clientes;
-- Tabla: `clientes`
CREATE TABLE clientes (
    cliente_id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) UNIQUE NOT NULL,
    telefono VARCHAR(15),
    direccion VARCHAR(255),
    contraseña VARCHAR(255) NOT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP
);

select * from productos;
-- Tabla: `productos`
CREATE TABLE productos (
    producto_id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    categoria VARCHAR(100) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    cantidad_disponible INT NOT NULL,
    fecha_agregado DATETIME DEFAULT CURRENT_TIMESTAMP,
    imagen LONGBLOB
);

-- Tabla: `ordenes`
CREATE TABLE ordenes (
    orden_id INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id INT,
    fecha_orden DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(50) DEFAULT 'Pendiente',
    total DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES clientes(cliente_id)
);
SELECT * FROM detalle_orden;

-- Tabla: `detalle_orden`
CREATE TABLE detalle_orden (
    detalle_id INT AUTO_INCREMENT PRIMARY KEY,
    orden_id INT,
    producto_id INT,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (orden_id) REFERENCES ordenes(orden_id),
    FOREIGN KEY (producto_id) REFERENCES productos(producto_id)
);
INSERT INTO administradores (nombre, correo, contraseña) 
VALUES ('Administrador', 'rosbin.vasquez@ulatina.net', 'admi');

select * from administradores;
-- Tabla: `administradores`
CREATE TABLE administradores (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) UNIQUE NOT NULL,
    contraseña VARCHAR(255) NOT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP
);
