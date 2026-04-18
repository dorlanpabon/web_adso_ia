# Web ADSO IA

Aplicacion web de comercio construida con Jakarta EE, JAX-RS, JPA (Hibernate) y MySQL.
Incluye flujo completo de usuarios, productos, carrito y ventas con interfaz multipagina.

## Funcionalidades

- Login y registro de usuarios
- Gestion de productos
- Carrito de compras por usuario
- Checkout y generacion de ventas
- Consulta y administracion de ventas

## Tecnologias

- Java 11
- Maven
- Jakarta EE 10
- Jersey (REST)
- Hibernate ORM 6
- MySQL Connector J
- Payara Micro

## Requisitos previos

1. Java JDK 11 o superior
2. Maven 3.9 o superior
3. MySQL o MariaDB ejecutandose en localhost:3306
4. Git (opcional, para publicacion)

## Instalacion y configuracion

### 1) Clonar el proyecto

    git clone <URL_DEL_REPOSITORIO>
    cd web_adso_ia

### 2) Crear base de datos y tablas

Opcion recomendada (cargar script completo):

Si tu usuario root tiene contrasena:

    mysql -u root -p < database/web_adso_ia.sql

Si root no tiene contrasena:

    mysql -u root < database/web_adso_ia.sql

### 3) Configurar credenciales de base de datos

Por defecto el proyecto usa:

- URL: jdbc:mysql://localhost:3306/web_adso_ia_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
- Usuario: root
- Contrasena: vacia

Si deseas usar otras credenciales, puedes definir variables o propiedades:

- APP_DB_URL
- APP_DB_USER
- APP_DB_PASSWORD

Ejemplo en PowerShell:

    $env:APP_DB_URL="jdbc:mysql://localhost:3306/web_adso_ia_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    $env:APP_DB_USER="root"
    $env:APP_DB_PASSWORD="tu_password"

## Compilar el proyecto

    mvn clean package

## Instalar Payara Micro (si no esta descargado)

    mvn dependency:get -Dartifact=fish.payara.extras:payara-micro:6.2025.11

## Ejecutar el proyecto

Desde la raiz del proyecto:

    java -jar "$env:USERPROFILE\.m2\repository\fish\payara\extras\payara-micro\6.2025.11\payara-micro-6.2025.11.jar" --port 8082 --deploy "target\web_adso_ia-1.0-SNAPSHOT.war"

## URLs de uso

Aplicacion principal:

- http://localhost:8082/web_adso_ia-1.0-SNAPSHOT/

Paginas:

- http://localhost:8082/web_adso_ia-1.0-SNAPSHOT/login.html
- http://localhost:8082/web_adso_ia-1.0-SNAPSHOT/registro.html
- http://localhost:8082/web_adso_ia-1.0-SNAPSHOT/productos.html
- http://localhost:8082/web_adso_ia-1.0-SNAPSHOT/ventas.html

API base:

- http://localhost:8082/web_adso_ia-1.0-SNAPSHOT/resources

## Publicar en GitHub

Si aun no tienes repositorio git inicializado:

    git init
    git add .
    git commit -m "feat: proyecto ecommerce jakarta ee"
    git branch -M main
    git remote add origin <URL_DEL_REPOSITORIO>
    git push -u origin main

## Solucion de problemas

1. Si el puerto 8082 esta ocupado, cambia el valor de --port.
2. Si aparece Access denied en MySQL, revisa usuario y contrasena.
3. Si no ves cambios en frontend, ejecuta nuevamente:

       mvn clean package

   y reinicia Payara Micro.
