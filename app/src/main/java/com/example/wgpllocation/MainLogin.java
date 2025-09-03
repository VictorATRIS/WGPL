package com.example.wgpllocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

public class MainLogin extends Activity {
    private Usuario usuario;
    private TextView textUsuario;
    private TextView textContrasena;
    private String cadenaConexion;
    private Button butIngresar ;
    private Conexion conexion ;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        butIngresar = findViewById(R.id.butIngresar); //Hacemos referencia al boton del activity
        usuario = new Usuario();

        //Hacemos la referencia a los campos de texto del activity
        textUsuario = findViewById(R.id.textUsuario2);
        textContrasena = findViewById(R.id.textContrasena);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

          //textUsuario.setText("victor.avalos");
         // textContrasena.setText("321");
        //En esta variable esta la cadena que usaremos para la conexion a la base de datos
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            checkConfig();
        }




        conexion = new Conexion(cadenaConexion,this);


        butIngresar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Checamos que ingrese usuario y contrasena
                if(!textUsuario.getText().toString().trim().isEmpty() && !textUsuario.getText().toString().trim().isEmpty() ){
                    //Seteamos lo que hay en los campos de texto al objeto usuario
                    usuario.setUsuario(textUsuario.getText().toString().trim());
                    usuario.setPassword(textContrasena.getText().toString());
                    //Validamos que el usuario  y contrasena sean correctos
                    try {
                        if(conexion.validaUsuario(usuario)){



                            Intent intent = null;
                            try {
                                //Ya cuando el usuario sea correcto abrimos el siguiente activity y le enviamos el usuario y nombre del usuario
                                try {
                                    if (!usuario.getUsuarioGrupo().equals("99")) {
                                    intent = new Intent(MainLogin.this, Menu.class);
                                    }else {
                                        intent = new Intent(MainLogin.this, MenuCalidad.class);
                                    }
                                }catch (Exception ex){
                                    ex.printStackTrace();
                                }
                                textUsuario.setText("");
                                textContrasena.setText("");
                                textUsuario.requestFocus();
                                intent.putExtra("Usuario", usuario);
                                intent.putExtra("cadenaCon",cadenaConexion);

                                startActivity(intent);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                        }else {
                            conexion.mensaje("Usuario incorrecto","WGPL LOCATION", android.R.drawable.ic_dialog_alert);
                        }
                    } catch (SQLException e) {
                        conexion.mensaje(e.getMessage(),"WGPL LOCATION", android.R.drawable.ic_dialog_alert);
                    }
                }else {
                    conexion.mensaje("Ingrese usuario y contrasena ","WGPL LOCATION", android.R.drawable.ic_dialog_alert);
                }
            }
        });
    }

    //Esto es para ocultar el teclado cuando le das click sobre la pantalla
    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null ) {
            hideKeyboard(view);
        }
        return super.dispatchTouchEvent(ev);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResult);
        checkConfig();
    }
    public void checkConfig() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {

            Uri uri = Uri.parse("package:" + "com.example.snapshot_srs");
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri));
        }
        String dir = Environment.getExternalStorageDirectory() + "/ATR-APPS/";
        File path = new File(dir);

        try {
            if (!path.exists()) {
                if (!path.mkdirs()) {
                    showToastMsg("Error al crear el directorio");
                }
            }
            File file = new File(path.getAbsolutePath(), "configWGPL.txt");
            if (!file.exists()) {
                AlertDialog mBuilder = new AlertDialog.Builder(this)
                        .setTitle("Pull System")
                        .setMessage("Ingrese el archivo de configuracion antes de continuar.")
                        .setPositiveButton("Ok", null)
                        .show();

                Button mPositiveButton = mBuilder.getButton(AlertDialog.BUTTON_POSITIVE);
                mPositiveButton.setOnClickListener(v -> finish());
            }
        } catch (Exception e) {
            showToastMsg(e.getMessage());
        }

        File configFile = new File(Environment.getExternalStorageDirectory() + "/ATR-APPS/configWGPL.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(configFile));
            String line;

            while ((line = br.readLine()) != null) {
                if (line.equals("CadenaConexion")) {
                    cadenaConexion = br.readLine();

                }


            }
        } catch (IOException e) {
            showToastMsg(e.getMessage());
        }
    }

    private void showToastMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}
