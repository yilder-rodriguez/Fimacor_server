package com.fimacor.comun.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Sin esto, maximumSessions(1) no se entera cuando una sesion vieja expira o hace
     * logout, y con el tiempo cuenta sesiones fantasma como si siguieran activas. */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/Estyle.css/**", "/Script.js/**", "/Img/**", "/webjars/**", "/error").permitAll()
                .requestMatchers("/login", "/registro", "/registro/**", "/recuperar", "/recuperar/**").permitAll()
                .requestMatchers("/ValidarDatoRegistroServlet", "/AdminShortcutAuthServlet").permitAll()
                // El acta y la firma se abren escaneando el QR del traslado desde el
                // celular, sin haber iniciado sesion.
                .requestMatchers("/ActaReubicacionPdfServlet", "/FirmaReubicacionServlet").permitAll()
                .requestMatchers("/FichaTecnicaPublicaServlet", "/FichaTecnicaPublicaPdfServlet", "/MaquinaQrServlet").permitAll()
                .requestMatchers("/AbrirReporteNovedadQrServlet", "/ReporteNovedadQrServlet").permitAll()
                .anyRequest().authenticated()
            )
            // AnalizarCarnetServlet/AdminShortcutAuthServlet se llaman por fetch()
            // sin token CSRF, asi que quedan exentos de la validacion.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/AdminShortcutAuthServlet"))
            // Token CSRF vencido (pagina abierta con la sesion ya caducada) -> vuelve a la pantalla con aviso, no 403 en blanco.
            .exceptionHandling(ex -> ex.accessDeniedHandler(new SesionCaducadaHandler()))
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("correo")
                .passwordParameter("contrasena")
                .defaultSuccessUrl("/menu", true)
                .failureUrl("/login?error=1")
                .permitAll()
            )
            .logout(logout -> logout
                // GET ademas de POST: el modal de confirmacion navega a este enlace
                // con <a href> en vez de un formulario.
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?salida=1")
                .permitAll()
            )
            // Una sola sesion activa por cuenta: si alguien inicia sesion en otro
            // dispositivo/navegador, la sesion anterior se invalida (no se bloquea el
            // login nuevo). Al primero le aparece el aviso "sesion iniciada en otro
            // lugar" la proxima vez que intente usar la pagina.
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expirado=1")
            );
        return http.build();
    }
}
