package com.example.wgpllocation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

public class MenuCalidad extends Activity {
    private  Usuario usuario;
    public String cadenaConexion;
    private Button btnRecibo , btnRetorno;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_calidad);
        Intent intent =  getIntent();
        iniciarElementos();
        usuario =(Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");

        btnRecibo.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(MenuCalidad.this, ReciboCalidad.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                startActivity(intent2);
            }
        });
        btnRetorno.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(MenuCalidad.this, SalidaCalidad.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                startActivity(intent2);
            }
        });
    }
    public void iniciarElementos(){
        btnRecibo = findViewById(R.id.btnEnvio);
        btnRetorno = findViewById(R.id.btnEmbarque);
    }
}
