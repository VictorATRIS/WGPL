package com.example.wgpllocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

import java.util.ArrayList;

public class Pallets extends Activity implements EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener  {
    public String cadenaConexion;
    public  Usuario usuario;
    Conexion conexion;
    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private EditText textLocation, textMaster ;
    private TextView lineaLabel;
    private ProgressBar progressBar;
    MediaPlayer sonidoError,sonidoCorrecto = null;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pallets);
        Intent intent =  getIntent();
        iniciarElementos();
        usuario =(Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");
        conexion = new Conexion(cadenaConexion);
        sonidoError = MediaPlayer.create(this, R.raw.error);
        sonidoCorrecto = MediaPlayer.create(this, R.raw.correct);
        EMDKManager.getEMDKManager(getApplicationContext(), this);


        textLocation.setInputType(InputType.TYPE_NULL);
        textMaster.setInputType(InputType.TYPE_NULL);
    }
    private void iniciarElementos(){

        textLocation = findViewById(R.id.editTextArea);
        textMaster = findViewById(R.id.editTextMaster);
        lineaLabel = findViewById(R.id.lbl_linea);
        progressBar = findViewById(R.id.progressBar);


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


        if (scanner != null && scanner.isEnabled()) {
            try {
                scanner.disable(); // Esto detiene la lectura
            } catch (ScannerException e) {
                mensaje("Error al desactivar escáner: " + e.getMessage());
            }
        }

        runOnUiThread(() -> {


            try {
                if (scanner != null && scanner.isEnabled()) {
                    try {
                        scanner.disable(); // Esto detiene la lectura
                    } catch (ScannerException e) {
                        mensaje("Error al desactivar escáner: " + e.getMessage());
                    }
                }

                //Validamos que escaneen la linea
                /*
                if (result.matches("[a-zA-Z]+")) {
                    textLinea.setText(result);
                    textLocation.setText("");
                    textMaster.setText("");
                    return;
                }
                */
                if (result.startsWith("L") && result.endsWith("T")) {
                    // Elimina el primer y último carácter
                    String trimmedResult = result.substring(1, result.length() - 1);

                    textLocation.setText(trimmedResult);
                    textMaster.setText("");
                    return;
                }

                // Los Master tiene que empezar por 4S
                if (!result.startsWith("4S")) {
                    mensaje("Dato incorrecto");
                    sonidoError.start();
                    return;
                }
                //Esta parte es cuando estan en el area de teminado y ya van y a guardar el pallet, para que les diga las areas disponibles
                if ( textLocation.getText().toString().trim().equals("")) {

                    if (!conexion.masterValido(result)) {
                        mensaje("Master no válido");
                        textMaster.setText("");
                        sonidoError.start();
                        return;
                    }
                    if (conexion.necesitaReinspeccion(result)) {
                        mensaje("Por favor lleve este pallet a la area de Reinspeccion ");
                        textMaster.setText("");
                        sonidoError.start();
                        return;
                    }
                    if (conexion.necesitaASN()) {
                        if (!conexion.masterConASN(result)) {
                            mensaje("Este Master no tiene ASN, favor de asignar uno");
                            textMaster.setText("");
                            sonidoError.start();
                            return;
                        }

                    }
                    lineaLabel.setText(conexion.getLocacionPallet(result));
                    textMaster.setText(result);
                    return;

                }
                //Si ya empezo a llenar los campos
                if (!textLocation.getText().toString().trim().equals("")) {
                    textMaster.setText(result);

                    if (!conexion.masterValido(result)) {
                        mensaje("Master no válido");
                        sonidoError.start();
                        textMaster.setText("");
                        return;
                    }
                    if (conexion.necesitaReinspeccion(result)) {
                        mensaje("Por favor lleve este pallet a la area de Reinspeccion ");
                        textMaster.setText("");
                        sonidoError.start();
                        return;
                    }

                    if (conexion.necesitaASN()) {
                        if (!conexion.masterConASN(result)) {
                            mensaje("Este Master no tiene ASN, favor de asignar uno");
                            textMaster.setText("");
                            sonidoError.start();
                            return;
                        }

                    }

                    if (!conexion.plantaCorrecta(result, textLocation.getText().toString().trim())) {
                        mensaje("Este pallet es de otra planta, este no es su lugar");
                        textMaster.setText("");
                        sonidoError.start();
                        return;
                    }

                    if (conexion.masterYaRegistrado(result, textLocation.getText().toString().trim())) {
                        if(conexion.palletRegistrosQA(result.trim(),"R")){
                            mensaje("Calidad aun no registro una salida para este pallet");
                            textMaster.setText("");
                            sonidoError.start();
                            return;

                        }
                        if (conexion.registrarDatos(textLocation.getText().toString().trim(), result, usuario.getUsuarioNick())) {
                            mensaje("Pallet guardado correctamente");
                            textLocation.setText("");
                            textMaster.setText("");
                            lineaLabel.setText("");
                            sonidoCorrecto.start();
                            return;
                        }
                    }

                    if (conexion.lugarOcupado(textLocation.getText().toString().trim())) {
                        mensaje("Esta localización ya está ocupada");
                        sonidoError.start();
                        return;
                    }

                    if (conexion.validaMasterRegistrado(result)) {
                        mensaje("Master ya registrado en otra localización");
                        textMaster.setText("");
                        sonidoError.start();
                        return;
                    }

                    if (conexion.registrarDatos(textLocation.getText().toString().trim(), result, usuario.getUsuarioNick())) {
                        mensaje("Pallet guardado correctamente");
                        textLocation.setText("");
                        textMaster.setText("");
                        lineaLabel.setText("");
                        sonidoCorrecto.start();
                    }
                }

            } catch (Exception e) {
                mensaje(e.getMessage());
            } finally {

                if (scanner != null && !scanner.isEnabled()) {
                    try {
                        scanner.enable(); // Vuelve a permitir escaneo
                    } catch (ScannerException e) {
                        mensaje("Error al activar escáner: " + e.getMessage());
                    }
                }

            }
        });
    }


    public void mensaje(String mensaje) {


        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(mensaje);
        dlgAlert.setTitle("WGPL LOCATION");
        dlgAlert.create().show();

    }
}
