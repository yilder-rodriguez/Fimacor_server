package com.fimacor.comun.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.util.UriComponentsBuilder;

/** Sin esto, adjuntar un archivo mas grande de lo permitido (una foto de celular sin
 * comprimir, por ejemplo) no mostraba ningun mensaje: Tomcat no llega a devolver una
 * respuesta HTTP limpia si el cuerpo de la peticion que le sobra por "tragar" supera su
 * limite por defecto (server.tomcat.max-swallow-size, 2MB), asi que la conexion se corta
 * a medias y el navegador muestra su propia pantalla generica de "esta pagina no esta
 * disponible" en vez de un error nuestro. Con max-swallow-size en -1 (ver application.yml)
 * Tomcat siempre puede devolver esta respuesta, que manda de vuelta al formulario con un
 * mensaje explicando que el archivo es muy grande. */
@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionAdvice {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoDemasiadoGrande(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        String destino = (referer != null && !referer.isBlank()) ? referer : "/menu";
        return "redirect:" + UriComponentsBuilder.fromUriString(destino)
                .replaceQueryParam("error", "archivo_grande")
                .build().toUriString();
    }
}
