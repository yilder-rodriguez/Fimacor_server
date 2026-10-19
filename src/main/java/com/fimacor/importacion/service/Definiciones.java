package com.fimacor.importacion.service;

import java.util.List;

/**
 * Las columnas exactas de cada plantilla. Es la unica fuente: de aqui salen la plantilla que se descarga y los
 * titulos que el importador busca, asi que lo que SENA llena es justo lo que el sistema lee.
 */
final class Definiciones {

    static final String[] FRECUENCIAS = {"Mensual", "Trimestral", "Semestral", "Anual"};
    static final String[] TIPOS_FALLA = {"Mecánica", "Eléctrica", "Electrónica", "Otro"};
    static final String[] ESTADOS_NOVEDAD = {"Reportada", "Asignada", "En reparación", "Finalizada por técnico", "Cerrada", "Cancelada", "Devuelta"};
    static final String[] ESTADOS_MANTENIMIENTO = {"Programado", "En mantenimiento", "Finalizado", "Cerrado"};
    static final String[] ESTADOS_MAQUINA = {"Funcional", "En mantenimiento", "Fuera de servicio"};
    static final String[] TIPOS_DOCUMENTO = {"CC", "TI", "CE"};
    static final String[] SI_NO = {"Sí", "No"};

    private Definiciones() {
    }

    static List<Columna> columnas(TipoImportacion tipo) {
        return switch (tipo) {
            case USUARIOS -> usuarios();
            case MAQUINAS -> maquinas();
            case NOVEDADES -> novedades();
            case MANTENIMIENTOS -> mantenimientos();
        };
    }

    /** Campos que se muestran en la vista previa de cada tipo (ademas de la fila y la accion). */
    static List<String> camposMuestra(TipoImportacion tipo) {
        return switch (tipo) {
            case USUARIOS -> List.of("nombre", "apellido", "correo", "rol");
            case MAQUINAS -> List.of("codigo", "tipo", "estado", "ambiente");
            case NOVEDADES -> List.of("codigo", "fecha", "tipoFalla", "estado");
            case MANTENIMIENTOS -> List.of("codigo", "tipo", "fecha", "estado");
        };
    }

    private static List<Columna> usuarios() {
        return List.of(
            Columna.texto("nombre", "Nombres", true, 22, "Nombres de la persona.", "María Fernanda", "Carlos Andrés", "nombre", "nombres de la persona"),
            Columna.texto("apellido", "Apellidos", true, 22, "Apellidos de la persona.", "Rojas Gómez", "Pérez Díaz", "apellido", "apellidos de la persona"),
            Columna.lista("tipoDoc", "Tipo de documento", false, 18, "tipoDoc", true, "CC (cédula), TI (tarjeta de identidad) o CE (cédula de extranjería). Si lo dejas vacío se usa CC.", "CC", "CC", "tipo documento", "tipo de identificacion", "tipo identificacion"),
            Columna.texto("documento", "Número de documento", true, 20, "Solo el número, sin puntos ni espacios. No puede repetirse.", "1032456789", "80003566", "documento", "numero documento", "numero de identificacion", "cedula", "identificacion", "documento de identidad", "numero de cedula"),
            Columna.texto("telefono", "Teléfono", false, 16, "Número de celular o fijo.", "3101234567", "3209876543", "telefono", "celular", "numero de telefono", "numero telefono", "movil"),
            Columna.texto("correo", "Correo electrónico", true, 30, "Con él entra la persona al sistema. No puede repetirse.", "maria.rojas@sena.edu.co", "carlos.perez@sena.edu.co", "correo", "email", "e mail", "correo electronico", "mail"),
            Columna.lista("rol", "Rol", true, 22, "rol", true, "Elige uno de la lista (Administrador, Instructor, Cuentadante, etc.).", "Instructor", "Cuentadante", "perfil", "cargo"),
            Columna.lista("actuaTecnico", "Instructor también es técnico", false, 20, "siNo", true, "Solo para Instructores: Sí si además atiende reparaciones y mantenimientos. Para los demás roles déjalo vacío.", "Sí", "", "actua como tecnico", "instructor tecnico", "tambien es tecnico"),
            Columna.texto("contrasena", "Contraseña inicial", false, 22, "Opcional (mínimo 8 caracteres). Si la dejas vacía el sistema genera una y te la entrega al terminar.", "", "", "contrasena", "clave", "password"));
    }

    private static List<Columna> maquinas() {
        return List.of(
            Columna.texto("codigo", "Código SENA", true, 18, "Placa o código de inventario. Es único: si ya existe, la máquina se actualiza.", "92123043", "9212684", "codigo", "cod sena", "placa", "placa sena", "codigo de la maquina", "codigo inventario", "no inventario", "numero de inventario"),
            Columna.texto("descripcion", "Descripción", false, 34, "Descripción de la máquina (hasta 255 caracteres). Si lo dejas vacío se arma con tipo, marca y modelo.", "Plana mecatrónica de aguja", "Máquina dos agujas lubricación automática", "nombre", "descripcion de la maquina", "nombre de la maquina", "maquina"),
            Columna.lista("tipo", "Tipo de máquina", true, 22, "tipoMaquina", false, "Elige de la lista o escribe uno nuevo (se crea automáticamente).", "Plana Mecatronica", "Plana Convencional", "tipo", "tipo maquina", "tipo de equipo", "clase de maquina"),
            Columna.lista("marca", "Marca", false, 18, "marca", false, "Elige de la lista o escribe una nueva. Vacío: se guarda como SIN MARCA.", "Sunstar", "Typical", "marca"),
            Columna.texto("modelo", "Modelo", false, 16, "Referencia del fabricante. Vacío: se guarda como SIN MODELO.", "30KM250", "GC6220B", "modelo", "referencia"),
            Columna.lista("estado", "Estado", true, 20, "estadoMaquina", true, "Funcional, En mantenimiento o Fuera de servicio.", "Funcional", "En mantenimiento", "estado", "estado operativo", "estado de la maquina", "estado de maquina"),
            Columna.lista("sede", "Sede", true, 20, "sede", false, "Elige de la lista o escribe una nueva.", "COMPLEJO SUR", "COMPLEJO SUR", "sede"),
            Columna.lista("area", "Área", false, 24, "area", false, "Área de la sede donde está (opcional).", "PLANTA CONFECCIONES", "PLANTA CONFECCIONES", "area"),
            Columna.lista("ambiente", "Ambiente de aprendizaje", true, 24, "ambiente", false, "Aula o taller donde está la máquina.", "Aula 203", "Aula 201", "ambiente de aprendizaje", "ambiente aprendizaje", "ubicacion fisica", "ubicacion", "aula", "ambiente"),
            Columna.fecha("fechaCompra", "Fecha de compra", false, "Formato 2024-05-31 o 31/05/2024.", "2010-03-25", "2008-06-10", "fecha de compra", "fecha compra", "fecha de adquisicion", "fecha adquisicion", "fecha ingreso"),
            Columna.numero("valor", "Valor", false, "Solo números, por ejemplo 5428800.", "5428800", "2500000", "valor", "valor maquina", "valor de la maquina", "valor de compra", "costo"),
            Columna.fecha("fechaGarantia", "Fecha fin de garantía", false, "Si tiene garantía vigente, hasta cuándo. Vacío: sin garantía.", "", "2027-06-10", "fecha fin garantia", "fecha fin de garantia", "fecha de garantia", "vencimiento garantia", "fin de garantia"),
            Columna.texto("cuentadanteDocumento", "Cuentadante (documento)", false, 22, "Documento de un usuario con rol Cuentadante ya creado en el sistema.", "80003566", "", "cuentadante documento", "documento cuentadante", "documento del cuentadante", "cedula cuentadante", "cc cuentadante"),
            Columna.texto("cuentadanteNombre", "Cuentadante (nombre)", false, 26, "Opcional: si no tienes el documento, el nombre completo del cuentadante.", "Guillermo Linares Puentes", "", "cuentadante nombre", "cuentadante", "nombre cuentadante", "responsable"));
    }

    private static List<Columna> novedades() {
        return List.of(
            Columna.texto("codigo", "Código SENA de la máquina", true, 20, "La máquina debe estar ya en el sistema (importa antes la plantilla de Máquinas).", "92123043", "9212684", "codigo sena", "codigo", "placa", "codigo de la maquina"),
            Columna.fecha("fecha", "Fecha de la novedad", true, "Cuándo se reportó la falla. Formato 2026-03-15 o 15/03/2026.", "2026-03-15", "2026-02-02", "fecha", "fecha novedad", "fecha del reporte", "fecha reporte"),
            Columna.texto("hora", "Hora (HH:MM)", false, 12, "Hora del reporte, por ejemplo 14:30. Vacío: 08:00.", "09:15", "", "hora", "hora novedad", "hora del reporte"),
            Columna.lista("tipoFalla", "Tipo de falla", true, 18, "tipoFalla", true, "Mecánica, Eléctrica, Electrónica u Otro.", "Mecánica", "Eléctrica", "tipo de falla", "falla", "tipo falla"),
            Columna.texto("descripcion", "Descripción de la falla", true, 40, "Qué le pasa a la máquina (máximo 500 caracteres).", "No enciende, se escucha un ruido en el motor", "Se rompe el hilo constantemente", "descripcion", "descripcion novedad", "descripcion detallada", "detalle"),
            Columna.texto("reportadoPor", "Reportado por (correo o documento)", true, 30, "Usuario que reportó. Debe existir en el sistema (importa antes la plantilla de Usuarios).", "maria.rojas@sena.edu.co", "80003566", "reportado por", "reporta", "reportante", "usuario que reporta"),
            Columna.lista("estado", "Estado de la novedad", true, 22, "estadoNovedad", true, "Reportada, Asignada, En reparación, Finalizada por técnico, Cerrada, Cancelada o Devuelta.", "Reportada", "Cerrada", "estado", "estado novedad", "estado de la novedad"),
            Columna.texto("tecnico", "Técnico asignado (correo o documento)", false, 30, "Obligatorio si el estado es Asignada, En reparación o Finalizada por técnico.", "", "carlos.perez@sena.edu.co", "tecnico", "tecnico asignado", "responsable"),
            Columna.fechaHora("fechaAsignacion", "Fecha de asignación", false, "Opcional. Si falta se usa la fecha de la novedad.", "", "2026-02-03", "fecha asignacion", "fecha de asignacion"),
            Columna.fechaHora("fechaInicio", "Fecha de inicio de la reparación", false, "Opcional.", "", "2026-02-04", "fecha inicio reparacion", "fecha inicio de la reparacion", "inicio reparacion"),
            Columna.fechaHora("fechaFin", "Fecha de fin de la reparación", false, "Opcional. Para novedades Finalizadas o Cerradas con técnico.", "", "2026-02-06", "fecha fin reparacion", "fecha fin de la reparacion", "fin reparacion"),
            Columna.texto("obsReparacion", "Observación de la reparación", false, 36, "Qué hizo el técnico (para Finalizadas o Cerradas).", "", "Se cambió el motor y se lubricó", "observacion reparacion", "observacion de la reparacion", "observacion tecnica"),
            Columna.texto("obsGestion", "Observación de gestión", false, 30, "Nota de quien asignó, cerró o canceló (opcional).", "", "Aprobada y puesta en servicio", "observacion gestion", "observacion de gestion", "observacion"));
    }

    private static List<Columna> mantenimientos() {
        return List.of(
            Columna.texto("codigo", "Código SENA de la máquina", true, 20, "La máquina debe estar ya en el sistema (importa antes la plantilla de Máquinas).", "92123043", "9212684", "codigo sena", "codigo", "placa", "codigo de la maquina"),
            Columna.lista("tipo", "Tipo de mantenimiento", true, 22, "tipoMantenimiento", true, "Elige de la lista (son los tipos que ya existen en el sistema).", "Preventivo", "Correctivo", "tipo", "tipo de mantenimiento", "tipo mantenimiento"),
            Columna.fecha("fecha", "Fecha del mantenimiento", true, "Fecha programada o en que se hizo. Formato 2026-03-15 o 15/03/2026.", "2026-11-10", "2026-03-15", "fecha", "fecha mantenimiento", "fecha programada", "fecha del mantenimiento"),
            Columna.texto("hora", "Hora (HH:MM)", false, 12, "Opcional, por ejemplo 08:30.", "08:30", "", "hora", "hora programada"),
            Columna.lista("frecuencia", "Frecuencia", true, 16, "frecuencia", true, "Mensual, Trimestral, Semestral o Anual. Define el próximo mantenimiento.", "Semestral", "Trimestral", "frecuencia"),
            Columna.texto("tecnico", "Técnico responsable (correo o documento)", true, 32, "Debe existir en el sistema y poder atender mantenimientos (importa antes la plantilla de Usuarios).", "carlos.perez@sena.edu.co", "carlos.perez@sena.edu.co", "tecnico", "responsable", "tecnico responsable", "tecnico encargado"),
            Columna.texto("tareas", "Tareas", true, 40, "Qué se debe hacer o se hizo (máximo 255 caracteres).", "Limpieza, lubricación y revisión de correas", "Cambio de aguja y ajuste de tensión", "tareas", "actividades", "descripcion"),
            Columna.lista("estado", "Estado", true, 20, "estadoMantenimiento", true, "Programado, En mantenimiento, Finalizado o Cerrado.", "Programado", "Cerrado", "estado", "estado del mantenimiento"),
            Columna.fechaHora("fechaFin", "Fecha de finalización", false, "Para Finalizado o Cerrado. Si falta se usa la fecha del mantenimiento.", "", "2026-03-15", "fecha fin", "fecha de finalizacion", "fecha finalizacion", "fecha de fin"),
            Columna.texto("observacion", "Observación del técnico", false, 36, "Qué encontró o hizo (para Finalizado o Cerrado).", "", "Quedó funcionando sin novedad", "observacion", "observacion tecnica", "observacion del tecnico"),
            Columna.fecha("proximo", "Próximo mantenimiento", false, "Opcional. Si falta se calcula con la frecuencia.", "", "", "proximo mantenimiento", "fecha proximo mantenimiento", "proxima fecha"));
    }
}
