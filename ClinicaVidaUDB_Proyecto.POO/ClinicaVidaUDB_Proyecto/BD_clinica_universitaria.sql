CREATE DATABASE clinica_universitaria;
USE clinica_universitaria;

CREATE TABLE pacientes (
    codigo_paciente INT NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    edad INT NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    correo_electronico VARCHAR(100) NOT NULL,
    tipo_paciente VARCHAR(20) NOT NULL,
    estado_paciente VARCHAR(20) NOT NULL
);

CREATE TABLE doctores (
    codigo_doctor INT NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    especialidad VARCHAR(50) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    correo_electronico VARCHAR(100) NOT NULL,
    estado_doctor VARCHAR(20) NOT NULL
);

CREATE TABLE citas (
    codigo_cita INT NOT NULL,
    codigo_paciente INT NOT NULL,
    codigo_doctor INT NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    estado_cita VARCHAR(20) NOT NULL
);

ALTER TABLE pacientes ADD CONSTRAINT PK_pacientes PRIMARY KEY (codigo_paciente);
ALTER TABLE doctores ADD CONSTRAINT PK_doctores PRIMARY KEY (codigo_doctor);
ALTER TABLE citas ADD CONSTRAINT PK_citas PRIMARY KEY (codigo_cita);


ALTER TABLE citas ADD CONSTRAINT FK_citas_pacientes FOREIGN KEY (codigo_paciente) REFERENCES pacientes(codigo_paciente);
ALTER TABLE citas ADD CONSTRAINT FK_citas_doctores FOREIGN KEY (codigo_doctor)REFERENCES doctores(codigo_doctor);

ALTER TABLE pacientes ADD CONSTRAINT CHK_tipo_paciente 
CHECK (tipo_paciente IN (
    'Estudiante',
    'Docente',
    'Administrativo',
    'Visitante'
));

ALTER TABLE citas ADD CONSTRAINT CHK_estado_cita
CHECK (estado_cita IN (
    'Programada',
    'Atendida',
    'Cancelada'
));


INSERT INTO pacientes (
    codigo_paciente,
    nombre_completo,
    edad,
    telefono,
    correo_electronico,
    tipo_paciente,
    estado_paciente
) VALUES
(1, 'Carlos Hernandez', 21, '7012-3456', 'carlos.hernandez@gmail.com', 'Estudiante', 'Activo'),
(2, 'Maria Lopez', 34, '7123-4567', 'maria.lopez@gmail.com', 'Docente', 'Activo'),
(3, 'Jose Martinez', 28, '7234-5678', 'jose.martinez@gmail.com', 'Administrativo', 'Activo'),
(4, 'Ana Garcia', 19, '7345-6789', 'ana.garcia@gmail.com', 'Estudiante', 'Activo'),
(5, 'Luis Ramirez', 45, '7456-7890', 'luis.ramirez@gmail.com', 'Visitante', 'Activo');




INSERT INTO doctores (
    codigo_doctor,
    nombre_completo,
    especialidad,
    telefono,
    correo_electronico,
    estado_doctor
) VALUES
(1, 'Dr. Roberto Martinez', 'Medicina General', '7011-2233', 'roberto.martinez@clinica.com', 'Activo'),
(2, 'Dra. Laura Hernandez', 'Psicología', '7022-3344', 'laura.hernandez@clinica.com', 'Activo'),
(3, 'Dr. Carlos Ramirez', 'Nutrición', '7033-4455', 'carlos.ramirez@clinica.com', 'Activo'),
(4, 'Dra. Sofia Garcia', 'Fisioterapia', '7044-5566', 'sofia.garcia@clinica.com', 'Activo');



INSERT INTO citas (
    codigo_cita,
    codigo_paciente,
    codigo_doctor,
    fecha,
    hora,
    motivo,
    estado_cita
) VALUES
(1, 1, 1, '2026-10-12', '08:00:00', 'Consulta médica general', 'Programada'),
(2, 2, 2, '2026-10-12', '09:00:00', 'Evaluación psicológica', 'Programada'),
(3, 3, 3, '2026-10-13', '10:00:00', 'Consulta nutricional', 'Atendida'),
(4, 4, 4, '2026-10-13', '11:00:00', 'Evaluación de fisioterapia', 'Programada'),
(5, 5, 1, '2026-10-14', '14:00:00', 'Consulta médica general', 'Cancelada');



SELECT * FROM pacientes;
SELECT * FROM doctores;
SELECT * FROM citas;

SELECT c.codigo_cita, p.nombre_completo AS paciente, d.nombre_completo AS doctor, d.especialidad, c.fecha, c.hora, c.motivo, c.estado_cita FROM citas c INNER JOIN pacientes p ON c.codigo_paciente = p.codigo_paciente INNER JOIN doctores d ON c.codigo_doctor = d.codigo_doctor;
SELECT * FROM citas WHERE estado_cita = 'Programada';
SELECT * FROM citas WHERE codigo_doctor = 1;
SELECT * FROM citas WHERE codigo_paciente = 1 ORDER BY fecha, hora;

