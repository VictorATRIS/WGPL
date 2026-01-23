package com.example.wgpllocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalidaCalidad extends Activity implements EMDKManager.EMDKListener,
        Scanner.StatusListener, Scanner.DataListener {

    public String cadenaConexion, planta;
    public Usuario usuario;
    Conexion conexion;

    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;

    private EditText textMaster;
    private TableLayout tableDatosReturn;

    private boolean masterEscaneado = false;

    MediaPlayer sonidoError, sonidoCorrecto = null;
    private List<Map<String, String>> serialBoxesEsperados = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.return_master);

        Intent intent = getIntent();
        iniciarElementos();
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
        tableDatosReturn = findViewById(R.id.tableDatosReturn);
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
                if (!masterEscaneado) {
                    // Escaneo de Master
                    textMaster.setText(result);

                    // VALIDACIONES ORIGINALES
                    if (!conexion.palletEnReinspeccionPerPallet(result)) {
                        mensaje("No outbound to reinspection has been registered for this Master", android.R.drawable.ic_delete);
                        sonidoError.start();
                        return;
                    }
                    if (conexion.necesitaReinspeccion(result)) {
                        mensaje("This Master has pending reinspections", android.R.drawable.ic_delete);
                         serialBoxesEsperados.clear();
                         serialBoxesEsperados = conexion.getSerialBoxesPorMaster(result);
                         mostrarSerialBoxesEnTabla(serialBoxesEsperados);
                         masterEscaneado = false;
                         textMaster.setText("");
                        sonidoError.start();
                        return;
                    }
                    if (conexion.palletRegistrosQA(result.trim(), "E")) {
                        mensaje("No inbound has been registered for this pallet", android.R.drawable.ic_delete);
                        sonidoError.start();
                        return;
                    }
                    if (!conexion.palletRegistrosQA(result.trim(), "R")) {
                        mensaje("An outbound has already been registered for this pallet", android.R.drawable.ic_delete);
                        sonidoError.start();
                        return;
                    }

                    // Si pasa todas las validaciones, cargamos Serial Boxes
                    serialBoxesEsperados.clear();
                     serialBoxesEsperados = conexion.getSerialBoxesPorMaster(result);
                    mostrarSerialBoxesEnTabla(serialBoxesEsperados);
                    masterEscaneado = true;

                } else {
                    // Escaneo de Serial Box físico
                    if (contieneSerialBox(result)) {
                        actualizarEstatusSerial(result, "Checked");
                       // sonidoCorrecto.start();

                        // Verificar si todas están escaneadas
                        if (todasSerialesEscaneadas()) {
                            if (conexion.registraDatosReinspeccion(textMaster.getText().toString(),"R", usuario.getUsuarioNick())) {
                                mensaje("Outbound successfully registered", android.R.drawable.checkbox_on_background);
                                sonidoCorrecto.start();
                                textMaster.setText("");
                                masterEscaneado = false;
                            }


                        }

                    } else {
                        mensaje("Serial no corresponde al Master", android.R.drawable.ic_delete);
                        sonidoError.start();
                    }
                }
            } catch (Exception e) {
                mensaje(e.getMessage(), android.R.drawable.ic_dialog_alert);
            }
        });
    }
    private boolean contieneSerialBox(String serial) {
        for (Map<String, String> fila : serialBoxesEsperados) {
            if (fila.get("Serial_Box").equalsIgnoreCase(serial)) {
                return true;
            }
        }
        return false;
    }


    private void mostrarSerialBoxesEnTabla(List<Map<String, String>> seriales) {
        tableDatosReturn.removeAllViews();

        for (Map<String, String> filaData : seriales) {
            TableRow fila = new TableRow(this);

            // Columna Serial
            TextView txtSerial = new TextView(this);
            txtSerial.setText(filaData.get("Serial_Box"));
            txtSerial.setGravity(Gravity.CENTER);
            txtSerial.setPadding(8, 8, 8, 8);
            txtSerial.setLayoutParams(new TableRow.LayoutParams(
                    0, TableRow.LayoutParams.WRAP_CONTENT, 1f)); // ancho proporcional

            // Columna Estatus
            TextView txtEstatus = new TextView(this);
            String status = filaData.get("Status");
            if ("1".equals(status)) {
                txtEstatus.setText("OK");
                txtEstatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if ("0".equals(status)) {
                txtEstatus.setText("Pendiente");
                txtEstatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else if ("-1".equals(status)) {
                txtEstatus.setText("Rechazado");
                txtEstatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                txtEstatus.setText("Desconocido");
                txtEstatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }
            txtEstatus.setGravity(Gravity.CENTER);
            txtEstatus.setPadding(8, 8, 8, 8);
            txtEstatus.setLayoutParams(new TableRow.LayoutParams(
                    0, TableRow.LayoutParams.WRAP_CONTENT, 1f)); // ancho proporcional

            fila.addView(txtSerial);
            fila.addView(txtEstatus);

            tableDatosReturn.addView(fila);
        }
    }

    private void actualizarEstatusSerial(String serial, String nuevoEstatus) {
        int childCount = tableDatosReturn.getChildCount();
        for (int i = 0; i < childCount; i++) {
            TableRow fila = (TableRow) tableDatosReturn.getChildAt(i);
            TextView txtSerial = (TextView) fila.getChildAt(0);
            TextView txtEstatus = (TextView) fila.getChildAt(1);

            if (txtSerial.getText().toString().equalsIgnoreCase(serial)) {
                txtEstatus.setText(nuevoEstatus);
                txtEstatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            }
        }
    }

    private boolean todasSerialesEscaneadas() {
        int childCount = tableDatosReturn.getChildCount();
        for (int i = 0; i < childCount; i++) {
            TableRow fila = (TableRow) tableDatosReturn.getChildAt(i);
            TextView txtEstatus = (TextView) fila.getChildAt(1);
            if (!"Checked".equals(txtEstatus.getText().toString())) {
                return false;
            }
        }
        return true;
    }

    public void mensaje(String mensaje, int iconoResId) {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(mensaje);
        dlgAlert.setTitle("WGPL LOCATION");
        dlgAlert.setIcon(iconoResId);
        dlgAlert.create().show();
    }
}