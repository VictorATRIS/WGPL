package com.example.wgpllocation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import java.sql.SQLException;

public class Menu extends Activity {
    private  Usuario usuario;
    public String cadenaConexion, planta;
    Button btnRecibo, btnInspeccion, btnDelete, btnShipping, btnExit;
    Conexion conexion;
    @Override



    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        Intent intent =  getIntent();
        iniciarElementos();
        usuario =(Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");
        planta = intent.getStringExtra("Planta");
        bloqueaBotones();
       conexion = new Conexion(cadenaConexion,planta);
        try {
            bloqueandoBotonesShipping();
        } catch (SQLException e) {
            conexion.mensaje(e.getMessage(),"WGPL", android.R.drawable.ic_delete);
        }


        btnRecibo.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(Menu.this, Pallets.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                intent2.putExtra("Planta",planta);
                startActivity(intent2);
            }
        });


        btnInspeccion.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(Menu.this, Inspeccion.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                intent2.putExtra("Planta",planta);
                startActivity(intent2);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(Menu.this, Delete.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                intent2.putExtra("Planta",planta);
                startActivity(intent2);
            }
        });
        btnShipping.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(Menu.this, Shipping.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                intent2.putExtra("Planta",planta);
                startActivity(intent2);
            }
        });
        btnExit.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(Menu.this, Exit.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                intent2.putExtra("Planta",planta);
                startActivity(intent2);
            }
        });



    }

    private void iniciarElementos(){
        btnRecibo = findViewById(R.id.btnEnvio);
        btnInspeccion = findViewById(R.id.btnEmbarque);
        btnDelete = findViewById(R.id.btnRetorno);
        btnShipping = findViewById(R.id.btnShipping);
        btnExit = findViewById(R.id.btnShippingExtra);

    }

    private void bloqueaBotones () {
        switch (usuario.getUsuarioGrupo()) {
            case "1":
                btnRecibo.setEnabled(true);
                btnInspeccion.setEnabled(true);
                btnDelete.setEnabled(true);
                break;
            case "3":
                btnRecibo.setEnabled(false);
                btnInspeccion.setEnabled(true);
                btnDelete.setEnabled(false);
                break;
            case "2":
                btnRecibo.setEnabled(true);
                btnInspeccion.setEnabled(false);
                btnDelete.setEnabled(false);
                break;

            case "4":
                btnRecibo.setEnabled(false);
                btnInspeccion.setEnabled(false);
                btnDelete.setEnabled(true);
                break;
            case "5":
                btnRecibo.setEnabled(true);
                btnInspeccion.setEnabled(true);
                btnDelete.setEnabled(false);
                break;
        }
    }
    private void bloqueandoBotonesShipping() throws SQLException {
      try {
          if(!conexion.existeOrden()) {
              btnShipping.setEnabled(false);
          }
      }catch (Exception ex) {
          conexion.mensaje(ex.getMessage(),"WGPL", android.R.drawable.ic_delete);

      }

    }

}
