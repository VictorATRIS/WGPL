package com.example.wgpllocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Shipping extends Activity implements EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {
    public String cadenaConexion,planta;
    public  Usuario usuario;
    Conexion conexion;
    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private EditText textLocation, textMaster ;
    private FloatingActionButton btnCloseFloat;

    private TableLayout tableDatos;

    MediaPlayer sonidoError,sonidoCorrecto = null;

    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shipping);
        Intent intent =  getIntent();
        iniciarElementos();
        usuario =(Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");
        planta = intent.getStringExtra("Planta");
        conexion = new Conexion(cadenaConexion,planta);
        sonidoError = MediaPlayer.create(this, R.raw.error);
        sonidoCorrecto = MediaPlayer.create(this, R.raw.correct);
        EMDKManager.getEMDKManager(getApplicationContext(), this);


        textLocation.setInputType(InputType.TYPE_NULL);
        textMaster.setInputType(InputType.TYPE_NULL);
        btnCloseFloat.setVisibility(View.GONE);

        btnCloseFloat.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
               mensaje();
            }
        });
    }

    private void iniciarElementos(){

        textLocation = findViewById(R.id.editTextArea);
        textMaster = findViewById(R.id.editTextMasterCode);
        tableDatos = findViewById(R.id.tableDatos);
        btnCloseFloat = findViewById(R.id.btnCloseFloat);


    }
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
        getOrden();
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

            if(!conexion.existeOrden()){
                mensaje("The Shipping order was closed", android.R.drawable.ic_delete);
                textMaster.setText("");
                textLocation.setText("");
                sonidoError.start();
                return;
            }

            if (!conexion.debeEmbarcar(result)) {
                mensaje("This pallet has no shipping order", android.R.drawable.ic_delete);
                textMaster.setText("");
                textLocation.setText("");
                sonidoError.start();
                return;
            }

            if(conexion.registraEmbarque(textMaster.getText().toString(),usuario.getUsuarioNick(),textLocation.getText().toString())){
                textMaster.setText("");
                textLocation.setText("");
                sonidoCorrecto.start();
                mensaje("Pallet saved successfully", android.R.drawable.checkbox_on_background);
                getOrden();
            }


        }catch (Exception e){
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
    public void mensaje() {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setTitle("WGPL LOCATION");
        dlgAlert.setMessage(" Are you sure you want close the shipping order?");


        dlgAlert.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                try{
                    //conexion.cerrarShippingOrder(usuario.getUsuarioNick());
                    mensaje("Shipping order closed successfully", android.R.drawable.checkbox_on_background);
                    getOrden();
                    dialog.dismiss();


                }catch (Exception ex){
                    mensaje(ex.getMessage(),android.R.drawable.ic_delete);
                }
            }
        });

        dlgAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Acción si el usuario cancela
                Toast.makeText(getApplicationContext(), "Asignación cancelada", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dlgAlert.setCancelable(false); // Opcional: evita que se cierre tocando fuera
        dlgAlert.create().show();
    }
    private void getOrden() {
        try {
            TableLayout tableDatos = findViewById(R.id.tableDatos);
            tableDatos.removeAllViews(); // Limpia solo las filas de datos

            List<Map<String, String>> ordenes = conexion.getDailyOrden(usuario.getUsuarioNick());
            int totalAsignados = 0; // Contador para Status = 1

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

                // Estatus
                TextView txtStatus = new TextView(this);
                txtStatus.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                txtStatus.setGravity(Gravity.CENTER);

                String rawStatus = orden.get("Status");
                String statusClean = rawStatus != null ? rawStatus.trim() : "";

                if ("1".equals(statusClean) || "2".equals(statusClean)) {
                    txtStatus.setText("✅");
                    txtStatus.setTextColor(Color.parseColor("#4CAF50")); // Verde
                    totalAsignados++; // Incrementa si está asignado
                } else {
                    txtStatus.setText(""); // O usa "❌"
                    txtStatus.setTextColor(Color.RED);
                }

                txtStatus.setTextSize(18);
                fila.addView(txtStatus);

                tableDatos.addView(fila);
            }

            // Actualiza el label con formato "asignados/total"
            TextView lblTotales = findViewById(R.id.lblTotales);
            if (totalAsignados >= ordenes.size()){

               // conexion.cerrarShippingOrder(usuario.getUsuarioNick());
                mensaje("Shipping order closed successfully", android.R.drawable.checkbox_on_background);
                if (scanner != null && scanner.isEnabled()) {
                    try {
                        scanner.disable(); // Esto detiene la lectura
                    } catch (ScannerException e) {
                        mensaje("Error deactivating scanner: " + e.getMessage(), android.R.drawable.ic_delete);
                    }
                }
            }
            lblTotales.setText("Total pallets: " + totalAsignados + "/" + ordenes.size());

        } catch (Exception ex) {
            mensaje(ex.getMessage(), android.R.drawable.ic_delete);
        }
    }

    }
