package com.example.wgpllocation;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class MenuCalidad extends Activity {
    private  Usuario usuario;
    public String cadenaConexion,planta;
    private Button btnRecibo , btnRetorno, btnSort, btnChangeMaster;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_calidad);
        Intent intent =  getIntent();
        iniciarElementos();
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
        usuario =(Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");
        planta = intent.getStringExtra("Planta");

        btnRecibo.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(MenuCalidad.this, ReciboCalidad.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                intent2.putExtra("Planta",planta);
                startActivity(intent2);
            }
        });
        btnRetorno.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(MenuCalidad.this, SalidaCalidad.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                intent2.putExtra("Planta",planta);
                startActivity(intent2);
            }
        });

        btnSort.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(MenuCalidad.this, SortRequest.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                intent2.putExtra("Planta",planta);
                startActivity(intent2);
            }
        });
        btnChangeMaster.setOnClickListener(new View.OnClickListener() {
            Intent intent2 = null;
            public void onClick(View view) {
                intent2 = new Intent(MenuCalidad.this, CambioMaster.class);
                intent2.putExtra("Usuario", usuario);
                intent2.putExtra("cadenaCon",cadenaConexion);
                intent2.putExtra("Planta",planta);
                startActivity(intent2);
            }
        });
    }
    public void iniciarElementos(){
        btnRecibo = findViewById(R.id.btnEnvio);
        btnRetorno = findViewById(R.id.btnEmbarque);
        btnSort = findViewById(R.id.btnSortRequest);
        btnChangeMaster = findViewById(R.id.btnRemplaceMaster);

    }
}
