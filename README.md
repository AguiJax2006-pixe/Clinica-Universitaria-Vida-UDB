# Sistema de Gestión de Citas — Clínica Universitaria Vida UDB

Aplicación de escritorio desarrollada en Java con interfaz Swing y persistencia en MySQL mediante JDBC. El sistema organiza pacientes, doctores y citas, e incluye consultas de reportes.

## Funcionalidades

- Gestión de pacientes: registrar, consultar, editar y cambiar estado.
- Gestión de doctores: registrar, consultar, editar y cambiar estado.
- Gestión de citas: registrar y consultar citas, con validación de disponibilidad del doctor para evitar dos citas programadas en la misma fecha y hora.
- Reportes: citas por fecha, citas por doctor e historial del paciente.
- Validaciones de datos y excepciones para informar errores de entrada, persistencia y conexión.

## Tecnologías

- Java (interfaz Swing)
- MySQL
- JDBC y MySQL Connector/J
- Patrón DAO para separar el acceso a datos de la interfaz

## Requisitos

- JDK compatible con el entorno de desarrollo.
- MySQL Server en ejecución.
- IDE Java, por ejemplo Visual Studio Code, NetBeans o IntelliJ IDEA.
- Biblioteca JDBC incluida en `lib/mysql-connector-j-26.7.0.jar`.

## Configuración de la base de datos

1. Abre MySQL/phpMyAdmin.
2. Ejecuta el script `BD_clinica_universitaria.sql` para crear la base y sus tablas. Si ya tienes datos importantes en una base con ese nombre, haz una copia de seguridad antes de importar el script.
3. Revisa `db.properties` y ajusta `db.url`, `db.usuario` y `db.clave` a la configuración de tu MySQL local.
4. No publiques credenciales reales. Antes de subir el proyecto, confirma que `db.properties` solo contenga datos de desarrollo y que no incluya contraseñas personales.

## Ejecutar

1. Extrae o clona el repositorio.
2. Abre la carpeta del proyecto en tu IDE.
3. Asegúrate de que `lib/mysql-connector-j-26.7.0.jar` esté agregado al classpath del proyecto.
4. Configura MySQL y `db.properties`.
5. Ejecuta `src/SistemaGestion.java`.

Si el IDE no permite ejecutar el archivo directamente, configura el proyecto Java para incluir todos los archivos de `src` y la biblioteca JDBC de `lib`.

## Estructura

- `src/`: clases de dominio, interfaz, validaciones, DAOs y excepciones.
- `lib/`: controlador JDBC de MySQL.
- `BD_clinica_universitaria.sql`: estructura y datos de ejemplo de la base de datos.
- `db.properties`: parámetros de conexión local.

## Nota sobre el estado del proyecto

Las pantallas de pacientes y doctores utilizan sus respectivos DAO para leer y guardar datos en MySQL. La gestión de citas y los reportes también se apoyan en `CitaDAO`. El funcionamiento completo depende de que MySQL esté iniciado, que el script SQL se haya ejecutado y que los parámetros de conexión sean correctos. Las pruebas deben realizarse en el entorno local antes de presentar el proyecto.

## Integrantes

Andrea Michelle Jimenez Aguilar - JA262091
Kimberly Alexandra Rivera Lopez - RL261989
Maria Ester Lopez Samour - LS253257
Edwin Andres Crespin Rivera - CR260027
Carlos Rodrigo Hernández Guardado - HG262490

## Proyecto académico

Universidad Don Bosco (UDB) — Clínica Universitaria Vida UDB.
