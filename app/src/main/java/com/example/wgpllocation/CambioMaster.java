package com.example.wgpllocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
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

public class CambioMaster extends Activity implements EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener{
    public String cadenaConexion, planta;
    public  Usuario usuario;
    Conexion conexion;
    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private EditText textMasterOld, textMasterNew ;
    MediaPlayer sonidoError,sonidoCorrecto = null;
    Button confirmarButton ;
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cambio_master);
        Intent intent =  getIntent();
        iniciarElementos();
        usuario =(Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");
        planta = intent.getStringExtra("Planta");
        conexion = new Conexion(cadenaConexion, planta);
        EMDKManager.getEMDKManager(getApplicationContext(), this);
        sonidoError = MediaPlayer.create(this, R.raw.error);
        sonidoCorrecto = MediaPlayer.create(this, R.raw.correct);
        textMasterOld.setInputType(InputType.TYPE_NULL);
        textMasterNew.setInputType(InputType.TYPE_NULL);
        confirmarButton.setOnClickListener(view -> {
            new AlertDialog.Builder(view.getContext())
                    .setTitle("Confirmation")
                    .setMessage("Are you sure you want to proceed?")
                    .setPositiveButton("Yes", (dialog, which) -> {

                        String oldMaster = textMasterOld.getText().toString().trim();
                        String newMaster = textMasterNew.getText().toString().trim();

                        if (oldMaster.isEmpty() || newMaster.isEmpty()) {
                            mensaje("Please enter both master numbers", android.R.drawable.ic_dialog_alert);
                            return;
                        }
                        try {
                            if (conexion.cambiaMaster(usuario.getUsuarioNick(), oldMaster, newMaster)) {
                                mensaje("Master changed successfully", android.R.drawable.ic_dialog_info);
                                textMasterOld.setText("");
                                textMasterNew.setText("");
                            } else {
                                mensaje("Master change failed", android.R.drawable.ic_delete);
                            }
                        } catch (SQLException e) {
                            mensaje("Database error: " + e.getMessage(), android.R.drawable.ic_dialog_alert);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });


    }
    private void iniciarElementos(){
        textMasterOld = findViewById(R.id.editTextOldMaster);
        textMasterNew = findViewById(R.id.editTextNewMaster);
        confirmarButton = findViewById(R.id.btnConfirm);

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


                if (!result.startsWith("4S") && !result.startsWith("OLD")) {
                    mensaje("Invalid Data", android.R.drawable.ic_delete);
                    textMasterNew.setText("");
                    return;
                }

                if (textMasterOld.getText().toString().trim().equalsIgnoreCase("")) {
                    textMasterOld.setText(result);
                    return;
                }

                if (result.equalsIgnoreCase("OLD")) {
                    textMasterNew.setText("");
                    mensaje("New master incorrect", android.R.drawable.ic_delete);
                    return;
                }


                textMasterNew.setText(result);

                if(!conexion.masterValido(result)){
                    mensaje("Incorrect Master", android.R.drawable.ic_delete);
                    return;
                }
                if (conexion.necesitaASN()) {
                    if (!conexion.masterConASN(result)) {
                        mensaje("This Master does not have an ASN assigned. Please assign one.", android.R.drawable.ic_delete);
                        textMasterNew.setText("");
                        sonidoError.start();
                        return;
                    }

                }

                if (textMasterNew.getText().toString().trim().equalsIgnoreCase(textMasterOld.getText().toString().trim())) {
                    mensaje("The new master must be different from the old master", android.R.drawable.ic_delete);
                    textMasterNew.setText("");
                    textMasterOld.setText("");
                    return;
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
}
