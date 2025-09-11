package com.example.wgpllocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Conexion {
    private Connection conn;
    private Statement comm;
    private String cadenaConexion;
    private Activity activiti;
    private String usuarioo;
    private String password, dom;
    private Usuario usuario;
    public Conexion(String cadenaConexion) {
        this.cadenaConexion = cadenaConexion;
    }

    public Conexion(String cadenaConexion, Activity activiti) {
        this.cadenaConexion = cadenaConexion;
        this.activiti = activiti;

    }


    public Conexion(Connection conn, Statement comm, String cadenaConexion, Activity activiti, String usuarioo, String password, String dom) {
        this.conn = conn;
        this.comm = comm;
        this.cadenaConexion = cadenaConexion;
        this.activiti = activiti;


        this.usuarioo = usuarioo;
        this.password = password;
        this.dom = dom;
    }
    public Conexion(String cadenaConexion, Activity activiti, Usuario usuario) {
        this.cadenaConexion = cadenaConexion;
        this.activiti = activiti;

        this.usuario = usuario;
    }
    public ResultSet ConsultaDatos(String query) {
        try {
            initConexion();
            return comm.executeQuery(query);

        } catch (Exception ex) {
            return null;
        }
    }

    public Connection getConn() {
        return conn;
    }

    public Connection initConexion() {
        try {

            Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(cadenaConexion);
            comm = conn.createStatement();
        } catch (Exception e) {
            mensaje(e.getMessage(),"Pull System", android.R.drawable.ic_dialog_alert);
        }
        return conn;

    }
    public void mensaje(String mensaje, String titulo, int iconoResId) {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this.activiti);
        dlgAlert.setMessage(mensaje);
        dlgAlert.setTitle(titulo);
        dlgAlert.setIcon(iconoResId); // Ícono dinámico

        dlgAlert.create().show();
    }


    public boolean validaUsuario(Usuario usuario) throws SQLException {
        boolean correcto = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "SP_CTRL_VALIDA_USUARIO_WGPL '" + usuario.getUsuario() + "' , '" + usuario.getPassword() + "'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                usuario.setNombre(resultSet.getString("Usr_name")); // Obtenemos el nombre del usuario
                usuario.setUsuario(resultSet.getString("USR_ID"));
                usuario.setUsuarioNick(resultSet.getString("Usr_nick"));
                usuario.setUsuarioGrupo(resultSet.getString("GRP_id"));

                correcto = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION" , android.R.drawable.ic_dialog_alert);
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return correcto;

    }

// Este metodo le sugiere al usuario donde poner el pallet dependendiendo de los espacios que tenga libres
    public String getLocacionPallet(String pallet) throws SQLException {
        String mensaje = "";
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "SP_CTRL_WGPL_GET_LOCATION_BY_PALLET'" + pallet.trim() + "'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {

                mensaje =  resultSet.getString("Mensaje");


            }else {

                mensaje = "No tenemos espacio libre o el Master es incorrecto";
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return mensaje;

    }

    public boolean masterValido(String pallet) throws SQLException {
        boolean existe = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "SP_CTRL_WGPL_VALIDA_MASTER'" + pallet.trim() + "'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                existe = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return existe;

    }

    public boolean masterYaRegistrado(String pallet, String area) throws SQLException {
        boolean existe = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "SP_CTRL_WGPL_PALLET_REGISTRADO'" + pallet.trim() + "' ,'" + area.trim() + "'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                existe = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return existe;

    }


    public boolean lugarOcupado(String area) throws SQLException {
        boolean existe = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "SP_CTRL_WGPL_LOCALIZACION_OCUPADA'" + area.trim() + "'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                existe = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return existe;

    }

    public boolean validaMasterRegistrado(String pallet) throws SQLException {
        boolean existe = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "SP_CTRL_WGPL_PALLET_REGISTRADO_MASTER'" + pallet.trim() + "'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                existe = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return existe;

    }
    public boolean registrarDatos(String area, String master, String usuario) throws SQLException {
        boolean registro = false;
        try {
            initConexion(); // Inicia la conexión con la base de datos

            // Ejecuta el stored procedure con los parámetros
            String query = " SP_CTRL_WGPL_REGISTRA_DATOS '" + area + "', '" + master + "', '" + usuario + "'";
            PreparedStatement preparedStatement = conn.prepareStatement(query);

            int resultado = preparedStatement.executeUpdate();

            if (resultado > 0) {
                registro = true;

            }

        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }
        }
        return registro ;
    }

    public boolean enviarAReinspeccion(String area, String master, String usuario) throws SQLException {
        boolean registro = false;
        try {
            initConexion(); // Inicia la conexión con la base de datos

            // Ejecuta el stored procedure con los parámetros
            String query = " SP_CTRL_WGPL_REGISTRA_DATOS_REINSPECCION '" + area + "', '" + master + "', '" + usuario + "'";
            PreparedStatement preparedStatement = conn.prepareStatement(query);

            int resultado = preparedStatement.executeUpdate();

            if (resultado > 0) {
                registro = true;

            }

        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }
        }
        return registro ;
    }

    public boolean eliminarDatosPallet(String area, String master) throws SQLException {
        boolean registro = false;
        try {
            initConexion(); // Inicia la conexión con la base de datos

            // Ejecuta el stored procedure con los parámetros
            String query = " SP_CTRL_WGPL_ELIMINA_DATOS_PALLET '" + area + "', '" + master + "'";
            PreparedStatement preparedStatement = conn.prepareStatement(query);

            int resultado = preparedStatement.executeUpdate();

            if (resultado > 0) {
                registro = true;

            }

        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }
        }
        return registro ;
    }

    public boolean plantaCorrecta(String pallet, String area) throws SQLException {
        boolean existe = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "SP_CTRL_WGPL_VALIDA_PLANTA'" + pallet.trim() + "' ,'" + area.trim() + "'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                existe = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return existe;

    }
    public boolean palletEnReinspeccion(String area) throws SQLException {
        boolean existe = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "select Area from ERP_CTRL_WGPL_LOCATIONS where area = '" + area.trim() + "' and estatus = '-1'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                existe = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return existe;

    }

    public boolean palletEnReinspeccionPerPallet(String master) throws SQLException {
        boolean existe = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "SP_CTRL_WGPL_REINSPECCION_POR_PALLET'" + master.trim() + "'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                existe = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return existe;

    }
    public boolean necesitaASN() throws SQLException {
        boolean existe = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "Select Estatus from ERP_CTRL_WGPL_VALIDA_ASN where Estatus = '1'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                existe = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return existe;

    }
    public boolean masterConASN(String pallet) throws SQLException {
        boolean existe = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "SP_CTRL_WGPL_VALIDA_MASTER_ASN'" + pallet.trim() + "'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                existe = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return existe;

    }
    public boolean registraDatosReinspeccion( String master, String tipo, String usuario) throws SQLException {
        boolean registro = false;
        try {
            initConexion(); // Inicia la conexión con la base de datos

            // Ejecuta el stored procedure con los parámetros
            String query = "SP_CTRL_WGPL_REGISTRA_DATOS_RECIBO_SALIDA_REINSPECCION'" + master.trim() + "', '" + tipo.trim() + "', '" + usuario + "'";
            PreparedStatement preparedStatement = conn.prepareStatement(query);

            int resultado = preparedStatement.executeUpdate();

            if (resultado > 0) {
                registro = true;

            }

        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }
        }
            return registro ;
    }
    public boolean palletRegistrosQA(String master, String tipo) throws SQLException {
        boolean existe = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "SP_CTRL_WGPL_CONSULTA_SALIDAS_ENTRADAS_QA '" + master.trim() + "', '" + tipo.trim() + "'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                existe = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return existe;

    }

    public boolean necesitaReinspeccion(String pallet) throws SQLException {
        boolean existe = false;
        try {
            initConexion(); // Iniciamos la conexion de la base da datos
            String query = "SP_CTRL_WGPL_VALIDA_SORTING'" + pallet.trim() + "'";
            ResultSet resultSet = comm.executeQuery(query);//Obtenemos los datos que traemos de la consulta y lo guardamos en un resultSet
            if (resultSet.next()) {
                existe = true;
            }


        } catch (SQLException ex) {
            mensaje(ex.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
            return false;
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }

        }
        return existe;

    }
    public List<Map<String, String>> getDatosQr(String qr) throws SQLException {
        List<Map<String, String>> lista = new ArrayList<>();

        try {
            initConexion();

            String query = "EXEC SP_CTRL_WGPL_GET_DATOS_QR ?";
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            preparedStatement.setString(1, qr);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                Map<String, String> fila = new HashMap<>();
                fila.put("Sort_Request_Detail", rs.getString("Sort_Request_Detail"));
                fila.put("Value", rs.getString("Value"));
                lista.add(fila);
            }

        } catch (SQLException ex) {
            mensaje(ex.getMessage(), "WGPL LOCATION", android.R.drawable.ic_dialog_alert);
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }
        }

        return lista;
    }


}
