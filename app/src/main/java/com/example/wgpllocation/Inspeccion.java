package com.example.wgpllocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Inspeccion extends Activity  implements EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {
    public String cadenaConexion, planta;
    public  Usuario usuario;
    Conexion conexion;
    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private EditText textLocation, textMaster ;
    MediaPlayer sonidoError,sonidoCorrecto = null;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reinspeccion);
        Intent intent =  getIntent();
        iniciarElementos();
        usuario =(Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");
        planta = intent.getStringExtra("Planta");
        conexion = new Conexion(cadenaConexion,planta);
        EMDKManager.getEMDKManager(getApplicationContext(), this);
        sonidoError = MediaPlayer.create(this, R.raw.error);
        sonidoCorrecto = MediaPlayer.create(this, R.raw.correct);
        textLocation.setInputType(InputType.TYPE_NULL);
        textMaster.setInputType(InputType.TYPE_NULL);
        getOrden();
    }

    private void iniciarElementos(){
        textLocation = findViewById(R.id.editTextArea);
        textMaster = findViewById(R.id.editTextMaster);


    }

    @Override
    public void onClosed() {
        if (this.emdkManager != null) {
            this.emdkManager.release();
            this.emdkManager = null;
        }
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;
        initBarcodeManager();
        initScanner();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.emdkManager != null) {
            barcodeManager = (BarcodeManager) this.emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
            initScanner();
        }
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        String dataStr = "";
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();
            for (ScanDataCollection.ScanData data : scanData) {
                dataStr = data.getData();
            }
            updateData(dataStr);
        }
    }

    public void onStatus(StatusData statusData) {
        StatusData.ScannerStates state = statusData.getState();
        if (state == StatusData.ScannerStates.IDLE) {
            try {
                scanner.read();
            } catch (ScannerException ignored) {
            }
        }
    }

    private void initBarcodeManager() {
        barcodeManager = (BarcodeManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
        if (barcodeManager == null) {
            Toast.makeText(this, "Barcode scanning is not supported.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initScanner() {
        if (scanner == null) {
            scanner = barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);
            if (scanner != null) {
                scanner.addDataListener(this);
                scanner.addStatusListener(this);
                scanner.triggerType = Scanner.TriggerType.HARD;
                try {
                    scanner.enable();
                } catch (ScannerException e) {
                    deInitScanner();
                }
            }
        }
    }

    private void deInitScanner() {
        if (scanner != null) {
            try {
                scanner.release();
            } catch (Exception ignored) {
            }
            scanner = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (scanner != null) {
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
                scanner.disable();
                scanner = null;
            }
        } catch (ScannerException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SetTextI18n")
    public void updateData(final String result) {
        runOnUiThread(() -> {
            try {

                if (result.startsWith("L") && result.endsWith("T")) {
                    // Elimina el primer y último carácter
                    String trimmedResult = result.substring(1, result.length() - 1);

                    textLocation.setText(trimmedResult);
                    textMaster.setText("");
                    return;
                }
                //Una vez lleno los campos ahora si ponemos hacer las validaciones
                if (!textLocation.getText().toString().trim().equals("")) {
                    textMaster.setText(result);
                    //validamos que el master sea valido
                    if (!conexion.masterValido(result)) {
                        mensaje("Invalid Master", android.R.drawable.ic_delete);
                        textMaster.setText("");
                        sonidoError.start();
                        return;
                    }
                  if(!conexion.necesitaReinspeccion(result)){
                      mensaje("This pallet has no reinspection redquirements", android.R.drawable.ic_delete);
                      textMaster.setText("");
                      sonidoError.start();
                      return;
                  }
                  if (!conexion.masterYaRegistrado(result,textLocation.getText().toString().trim())){
                      mensaje("The pallet does not match the area you indicated", android.R.drawable.ic_delete);
                      textMaster.setText("");
                      sonidoError.start();
                      return;

                  }
                  if (conexion.palletEnReinspeccion(textLocation.getText().toString().trim())){

                      mensaje("This pallet already has a reinspection status", android.R.drawable.ic_delete);
                      textLocation.setText("");
                      textMaster.setText("");
                      sonidoError.start();
                      return;
                  }

                    if(conexion.enviarAReinspeccion(textLocation.getText().toString().trim(), result, usuario.getUsuarioNick())) {

                        mensaje("You can take the pallet to reinspection without any issue", android.R.drawable.checkbox_on_background);
                        textLocation.setText("");
                        textMaster.setText("");
                        sonidoCorrecto.start();
                        getOrden();

                    }

                }


            } catch (Exception e) {
                mensaje(e.getMessage(), android.R.drawable.ic_delete);
            }
        });
    }
    public void mensaje(String mensaje, int iconoResId) {


        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(mensaje);
        dlgAlert.setTitle("WGPL LOCATION");
        dlgAlert.setIcon(iconoResId); // Ícono dinámico
        dlgAlert.create().show();

    }
    private void getOrden() {
        try {
            TableLayout tableDatos = findViewById(R.id.tableDatos);
            tableDatos.removeAllViews();


            TableRow encabezado = new TableRow(this);
            encabezado.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT));
            encabezado.setPadding(8, 8, 8, 8);

            // Título Área
            TextView tituloArea = new TextView(this);
            tituloArea.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tituloArea.setGravity(Gravity.CENTER);
            tituloArea.setText("Área");
            tituloArea.setTypeface(null, Typeface.BOLD);
            tituloArea.setTextColor(Color.BLACK);
            tituloArea.setTextSize(16);
            encabezado.addView(tituloArea);

            // Título Master
            TextView tituloMaster = new TextView(this);
            tituloMaster.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tituloMaster.setGravity(Gravity.CENTER);
            tituloMaster.setText("Master");
            tituloMaster.setTypeface(null, Typeface.BOLD);
            tituloMaster.setTextColor(Color.BLACK);
            tituloMaster.setTextSize(16);
            encabezado.addView(tituloMaster);

            tableDatos.addView(encabezado); // Agrega encabezado antes de los datos

            //  Cargar datos
            List<Map<String, String>> ordenes = conexion.getSendToQuality(usuario.getUsuarioNick());

            for (Map<String, String> orden : ordenes) {
                TableRow fila = new TableRow(this);
                fila.setLayoutParams(new TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.WRAP_CONTENT));
                fila.setPadding(8, 8, 8, 8);

                // Área
                TextView txtArea = new TextView(this);
                txtArea.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                txtArea.setGravity(Gravity.CENTER);
                txtArea.setText(orden.get("Location"));
                txtArea.setTextColor(Color.BLACK);
                txtArea.setTextSize(14);
                fila.addView(txtArea);

                // Master
                TextView txtMaster = new TextView(this);
                txtMaster.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                txtMaster.setGravity(Gravity.CENTER);
                txtMaster.setText(orden.get("Master"));
                txtMaster.setTextColor(Color.BLACK);
                txtMaster.setTextSize(14);
                fila.addView(txtMaster);

                tableDatos.addView(fila);
            }

        } catch (Exception ex) {
            mensaje(ex.getMessage(), android.R.drawable.ic_delete);
        }
    }


}
