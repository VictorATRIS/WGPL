package com.example.wgpllocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

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
import java.util.Objects;

public class InventoryQty extends Activity implements EMDKManager.EMDKListener,
        Scanner.StatusListener, Scanner.DataListener{

    public String cadenaConexion, planta;
    public Usuario usuario;
    Conexion conexion;

    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;

    private EditText textMaster, textArea;
    private TableLayout tableDatos;

    private boolean masterEscaneado = false;

    MediaPlayer sonidoError, sonidoCorrecto = null;
    private List<Map<String, String>> serialBoxesEsperados = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inventory_qty);

        Intent intent = getIntent();
        iniciarElementos();
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
        usuario = (Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");
        planta = intent.getStringExtra("Planta");
        conexion = new Conexion(cadenaConexion, planta);

        EMDKManager.getEMDKManager(getApplicationContext(), this);
        sonidoError = MediaPlayer.create(this, R.raw.error);
        sonidoCorrecto = MediaPlayer.create(this, R.raw.correct);
        textMaster.setInputType(InputType.TYPE_NULL);
    }

    private void iniciarElementos() {
        textMaster = findViewById(R.id.editTextMaster);
        textArea = findViewById(R.id.editTextLocation);
        tableDatos = findViewById(R.id.tableDatosReturn);
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

    @Override
    public void onStatus(StatusData statusData) {
        if (statusData.getState() == StatusData.ScannerStates.IDLE) {
            try {
                scanner.read();
            } catch (ScannerException ignored) {}
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
            } catch (Exception ignored) {}
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
                    String trimmedResult = result.substring(1, result.length() - 1);

                    textArea.setText(trimmedResult);
                    textMaster.setText("");
                    tableDatos.removeAllViews();
                    return;
                }

                String upperResult = result.toUpperCase();

                if (upperResult.startsWith("4S")
                        || upperResult.startsWith("S5V")
                        || upperResult.startsWith("S6P")
                        || upperResult.startsWith("S6L")
                        || upperResult.startsWith("S6J")
                        || upperResult.startsWith("S6M")
                        || upperResult.startsWith("S52")) {


                if (textArea.getText().toString().trim().isEmpty()) {
                    mensaje("Set the location first", android.R.drawable.ic_delete);
                    return;
                }

                if (textMaster.getText().toString().trim().isEmpty()) {
                    textMaster.setText(result);
                    getDatos(textMaster.getText().toString());
                    return;
                }

                if (!conexion.existeCaja(result)){
                    mensaje("The box does not exist", android.R.drawable.ic_delete);
                    return;
                }

                if (conexion.cajaRegistrada(result)) {
                    mensaje("Serial Box Was registered", android.R.drawable.ic_delete);
                    return;
                }
                
            if ( ! conexion.registrarBox(usuario.getUsuarioNick(), result, textMaster.getText().toString(),textArea.getText().toString())){
                mensaje("Error registering box", android.R.drawable.ic_delete);
                return ;
                }
            getDatos(textMaster.getText().toString());
                }

                else {
                    mensaje("Incorrect data", android.R.drawable.ic_delete);
                    sonidoError.start();

                }

            } catch (Exception e) {
                mensaje(e.getMessage(), android.R.drawable.ic_dialog_alert);
            }
        });
    }

    private void getDatos (String master) {
        try {
            serialBoxesEsperados.clear();
            serialBoxesEsperados = conexion.getSerialMasterInventory(master);
            mostrarSerialBoxesEnTabla(serialBoxesEsperados);
        }catch (Exception ex) {
            mensaje(ex.getMessage(), android.R.drawable.ic_delete);
        }

    }

    private void mostrarSerialBoxesEnTabla(List<Map<String, String>> seriales) {
        // Limpia solo las filas de datos
        tableDatos.removeAllViews();

        int index = 0;
        for (Map<String, String> filaData : seriales) {
            TableRow fila = new TableRow(this);
            fila.setPadding(8, 8, 8, 8);
            String upperResult = textMaster.getText().toString().toUpperCase();
            String sinPrimerosDos = upperResult.substring(2);

            // Alternar colores de fondo (efecto zebra)
            if (!filaData.get("Master").toUpperCase().equalsIgnoreCase(sinPrimerosDos)) {
                fila.setBackgroundColor(Color.parseColor("#F87C63"));

            } else {
                fila.setBackgroundColor(Color.parseColor("#E3F2FD"));
            }

            // Columna Serial
            TextView txtSerial = new TextView(this);
            txtSerial.setText(filaData.get("Serial_Box"));
            txtSerial.setTextColor(Color.BLACK);
            txtSerial.setGravity(Gravity.CENTER);
            txtSerial.setPadding(8, 8, 8, 8);
            txtSerial.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

            // Columna Master
            TextView txtMaster = new TextView(this);
            txtMaster.setText(filaData.get("Master"));
            txtMaster.setGravity(Gravity.CENTER);
            txtMaster.setTextColor(Color.BLACK);
            txtMaster.setPadding(8, 8, 8, 8);
            txtMaster.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

            // Columna Location
            TextView txtLocation = new TextView(this);
            txtLocation.setText(filaData.get("Location"));
            txtLocation.setGravity(Gravity.CENTER);
            txtLocation.setTextColor(Color.BLACK);
            txtLocation.setPadding(8, 8, 8, 8);
            txtLocation.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

            // Agregar columnas a la fila
            fila.addView(txtSerial);
            fila.addView(txtMaster);
            fila.addView(txtLocation);

            // Agregar fila a la tabla
            tableDatos.addView(fila);
            index++;
        }
    }

    private void actualizarEstatusSerial(String serial, String nuevoEstatus) {
        int childCount = tableDatos.getChildCount();
        for (int i = 0; i < childCount; i++) {
            TableRow fila = (TableRow) tableDatos.getChildAt(i);
            TextView txtSerial = (TextView) fila.getChildAt(0);
            TextView txtEstatus = (TextView) fila.getChildAt(1);

            if (txtSerial.getText().toString().equalsIgnoreCase(serial)) {
                txtEstatus.setText(nuevoEstatus);
                txtEstatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            }
        }
    }



    public void mensaje(String mensaje, int iconoResId) {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(mensaje);
        dlgAlert.setTitle("WGPL LOCATION");
        dlgAlert.setIcon(iconoResId);
        dlgAlert.create().show();
    }

}
