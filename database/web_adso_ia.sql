CREATE DATABASE IF NOT EXISTS web_adso_ia_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE web_adso_ia_db;

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'CLIENTE',
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255),
    precio DECIMAL(12, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    activo BIT(1) NOT NULL DEFAULT b'1',
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS carritos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_carrito_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS carrito_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrito_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_carritoitem_carrito FOREIGN KEY (carrito_id) REFERENCES carritos(id) ON DELETE CASCADE,
    CONSTRAINT fk_carritoitem_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT uq_carrito_producto UNIQUE (carrito_id, producto_id)
);

CREATE TABLE IF NOT EXISTS ventas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    total DECIMAL(12, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'CREADA',
    fecha_venta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_venta_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS venta_detalles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    nombre_producto VARCHAR(120) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_ventadetalle_venta FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE,
    CONSTRAINT fk_ventadetalle_producto FOREIGN KEY (producto_id) REFERENCES productos(id)
);

INSERT INTO productos (nombre, descripcion, precio, stock, activo)
SELECT *
FROM (
    SELECT 'Mouse Gamer', 'Mouse RGB con 6 botones programables', 120000.00, 30, b'1'
) AS data
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Mouse Gamer')
LIMIT 1;

INSERT INTO productos (nombre, descripcion, precio, stock, activo)
SELECT *
FROM (
    SELECT 'Teclado Mecanico', 'Teclado con switches blue', 250000.00, 20, b'1'
) AS data
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Teclado Mecanico')
LIMIT 1;

INSERT INTO productos (nombre, descripcion, precio, stock, activo)
SELECT *
FROM (
    SELECT 'Monitor 24', 'Monitor IPS 24 pulgadas Full HD', 780000.00, 12, b'1'
) AS data
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Monitor 24')
LIMIT 1;
