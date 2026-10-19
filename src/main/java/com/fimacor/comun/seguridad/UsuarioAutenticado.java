package com.fimacor.comun.seguridad;

import com.fimacor.usuarios.entity.Usuario;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Envuelve Usuario para que Spring Security pueda usarlo como principal autenticado. */
public class UsuarioAutenticado implements UserDetails {

    private final Usuario usuario;

    public UsuarioAutenticado(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String rol = usuario.getRol() != null ? usuario.getRol().getDescripcionRol() : "SIN_ROL";
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase().replace(" ", "_")));
    }

    @Override
    public String getPassword() {
        return usuario.getContrasenaUsuario();
    }

    @Override
    public String getUsername() {
        return usuario.getCorreo();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }

    /** Cada login crea una instancia nueva de este wrapper. Sin equals/hashCode basados en un
     * identificador estable, Spring Security nunca reconoce que dos logins son el mismo
     * principal (usa equals() para buscar sesiones existentes en el SessionRegistry), y
     * sessionManagement().maximumSessions(1) nunca se activaba -- permitia sesiones dobles
     * en cualquier cantidad de navegadores/dispositivos. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UsuarioAutenticado otro)) return false;
        return usuario.getIdUsuario() != null && usuario.getIdUsuario().equals(otro.usuario.getIdUsuario());
    }

    @Override
    public int hashCode() {
        return usuario.getIdUsuario() != null ? usuario.getIdUsuario().hashCode() : 0;
    }
}
