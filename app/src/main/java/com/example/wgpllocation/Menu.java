package com.example.wgpllocation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

public class Menu extends Activity {
    private  Usuario usuario;
    public String cadenaConexion;
    Button btnRecibo, btnInspeccion, btnDelete;
    @Override



    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        Intent intent =  getIntent();
        iniciarElementos();
        usuario =(Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");
        bloqueaBotones();


        btnRecibo.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(Menu.this, Pallets.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                startActivity(intent2);
            }
        });


        btnInspeccion.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(Menu.this, Inspeccion.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                startActivity(intent2);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(Menu.this, Delete.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                startActivity(intent2);
            }
        });

    }

    private void iniciarElementos(){
        btnRecibo = findViewById(R.id.btnEnvio);
        btnInspeccion = findViewById(R.id.btnEmbarque);
        btnDelete = findViewById(R.id.btnRetorno);

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
        }
    }
}
