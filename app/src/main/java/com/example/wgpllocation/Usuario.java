package com.example.wgpllocation;

import java.io.Serializable;

public class Usuario implements Serializable {
    private String usuario;
    private String password;
    private String nombre;
    private String usuarioNick;
    private String usuarioGrupo ;

    public Usuario(String usuario, String password, String nombre) {
        this.usuario = usuario;
        this.password = password;
        this.nombre = nombre;
    }
    public Usuario(){
        this.usuario = "";
        this.password = "";
        this.nombre = "";
        this.usuarioNick = "";
        this.usuarioGrupo = "";
    }

    public Usuario(String usuario, String password, String nombre, String usuarioNick, String usuarioGrupo) {
        this.usuario = usuario;
        this.password = password;
        this.nombre = nombre;
        this.usuarioNick = usuarioNick;
        this.usuarioGrupo = usuarioGrupo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsuarioNick() {
        return usuarioNick;
    }

    public void setUsuarioNick(String usuarioNick) {
        this.usuarioNick = usuarioNick;
    }

    public String getUsuarioGrupo() {
        return usuarioGrupo;
    }

    public void setUsuarioGrupo(String usuarioGrupo) {
        this.usuarioGrupo = usuarioGrupo;
    }
}
