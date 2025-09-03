package com.example.wgpllocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import java.util.List;
import java.util.Map;

public class SortRequest extends Activity implements EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {
    public String cadenaConexion;
    public  Usuario usuario;
    Conexion conexion;
    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private EditText textQr ;
    private TableLayout tableSortRequest;

    MediaPlayer sonidoError,sonidoCorrecto = null;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sort_request);
        iniciarElementos();
        Intent intent =  getIntent();
        iniciarElementos();
        usuario =(Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");
        conexion = new Conexion(cadenaConexion);
        EMDKManager.getEMDKManager(getApplicationContext(), this);
        sonidoError = MediaPlayer.create(this, R.raw.error);
        sonidoCorrecto = MediaPlayer.create(this, R.raw.correct);

    }
    private void iniciarElementos(){
        textQr = findViewById(R.id.editTextMaster);
        tableSortRequest = findViewById(R.id.tableSortRequest);
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
                textQr.setText(result);

                if (!result.endsWith("-QR")){
                    mensaje("The code is incorrect", android.R.drawable.ic_delete);
                    textQr.setText("");
                    sonidoError.start();
                    return;
                }

               llenarTabla(result);
                textQr.setText("");

            } catch (Exception e) {
                mensaje(e.getMessage(), android.R.drawable.ic_delete);
            }
        });
    }
    public void llenarTabla(String qr){
        try{
            List<Map<String, String>> datos = conexion.getDatosQr(qr);
            tableSortRequest.removeAllViews();

// Encabezado
            TableRow header = new TableRow(this);
            header.addView(createCell("Sort Request", true));
            header.addView(createCell("F. Reinspección", true));
            header.addView(createCell("Quality_Issue", true));
            tableSortRequest.addView(header);

// Filas
            for (Map<String, String> fila : datos) {
                TableRow row = new TableRow(this);
                row.addView(createCell(fila.get("SortRequest_Id"), false));
                row.addView(createCell(fila.get("Fecha_Actualiza"), false));
                row.addView(createCell(fila.get("Quality_Issue"), false));
                tableSortRequest.addView(row);
            }

        }catch (Exception e){
            mensaje(e.getMessage(),android.R.drawable.ic_delete);
        }

    }
    private TextView createCell(String text, boolean isHeader) {
        TextView cell = new TextView(this);
        cell.setText(text);
        cell.setPadding(8, 8, 8, 8);
        cell.setTextSize(isHeader ? 16 : 15);
        cell.setTextColor(Color.parseColor(isHeader ? "#000000" : "#333333"));
        cell.setTypeface(null, isHeader ? Typeface.BOLD : Typeface.NORMAL);
        return cell;
    }


    public void mensaje(String mensaje, int iconoResId) {


        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(mensaje);
        dlgAlert.setTitle("WGPL LOCATION");
        dlgAlert.setIcon(iconoResId); // Ícono dinámico
        dlgAlert.create().show();

    }

}
