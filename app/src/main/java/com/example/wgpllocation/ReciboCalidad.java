package com.example.wgpllocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

import java.util.ArrayList;

public class ReciboCalidad extends Activity implements EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {
    public String cadenaConexion;
    public  Usuario usuario;
    Conexion conexion;
    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private EditText  textMaster, textLocation ;
    MediaPlayer sonidoError,sonidoCorrecto = null;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recibo_calidad);
        Intent intent =  getIntent();
        iniciarElementos();
        usuario =(Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");
        conexion = new Conexion(cadenaConexion);
        EMDKManager.getEMDKManager(getApplicationContext(), this);
        sonidoError = MediaPlayer.create(this, R.raw.error);
        sonidoCorrecto = MediaPlayer.create(this, R.raw.correct);
        textMaster.setInputType(InputType.TYPE_NULL);
    }

    private void iniciarElementos(){

        textMaster = findViewById(R.id.editTextMaster);
        textLocation = findViewById(R.id.editTextLocation);


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
                String tipo  = "";

                if (result.startsWith("L") && result.endsWith("T")) {
                    // Elimina el primer y último carácter
                    String trimmedResult = result.substring(1, result.length() - 1);

                    textLocation.setText(trimmedResult);
                    textMaster.setText("");
                    return;
                }

                if (textLocation.getText().toString().trim().equals("")) {
                    mensaje("Please scan the location", android.R.drawable.ic_delete);
                    return;
                }

                // Los Master tiene que empezar por 4S
                if (!result.startsWith("4S")) {
                    mensaje("Incorrect data", android.R.drawable.ic_delete);
                    sonidoError.start();
                    return;
                }
                textMaster.setText(result);


                    if (!conexion.palletEnReinspeccionPerPallet(result)){
                        mensaje("This Master has no recorded outbound to reinspection", android.R.drawable.ic_delete);
                        textMaster.setText("");
                        sonidoError.start();
                        return;
                    }

                    switch (textLocation.getText().toString().substring(0,2).trim().toUpperCase()){
                        case "QS" :
                            tipo = "Q";
                            break;
                        case "QI" :
                            tipo = "R";
                            break;
                        case "QR" :
                            tipo = "B";
                            break;
                        case "GO":
                                tipo = "G";
                                break;
                        default:
                            mensaje("Invalid location", android.R.drawable.ic_delete);
                            textMaster.setText("");
                            textLocation.setText("");
                            sonidoError.start();
                            return;


                    }

                    if (tipo.equalsIgnoreCase("G")){
                        if (!conexion.palletEnReinspeccionPerPallet(result)){
                            mensaje("No outbound to reinspection has been registered for this Master", android.R.drawable.ic_delete);
                            textMaster.setText("");
                            sonidoError.start();
                            return;
                        }
                        if(conexion.necesitaReinspeccion(result)){
                            mensaje("This Master has pending reinspections", android.R.drawable.ic_delete);
                            textMaster.setText("");
                            sonidoError.start();
                            return;
                        }
                        if(conexion.palletRegistrosQA(result.trim(),"E")){
                            mensaje("No inbound has been registered for this pallet", android.R.drawable.ic_delete);
                            textMaster.setText(result);
                            sonidoError.start();
                            return;

                        }
                        if(!conexion.palletRegistrosQA(result.trim(),"R")){
                            mensaje("An outbound has already been registered for this pallet", android.R.drawable.ic_delete);
                            textMaster.setText(result);
                            sonidoError.start();
                            return;

                        }


                    }
                  if(conexion.registraDatosReinspeccionQA(result,tipo,usuario.getUsuarioNick(), textLocation.getText().toString().trim())){
                      mensaje("Inbound successfully registered", android.R.drawable.checkbox_on_background);
                      sonidoCorrecto.start();
                      textMaster.setText("");
                      textLocation.setText("");

                }



            } catch (Exception e) {
                mensaje(e.getMessage(),android.R.drawable.ic_delete);
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


}
