package com.example.wgpllocation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
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
    private EditText textLinea,textLocation, textMaster ;
    private TextView lineaLabel;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pallets);
        Intent intent =  getIntent();
        iniciarElementos();
        usuario =(Usuario) intent.getSerializableExtra("Usuario");
        cadenaConexion = intent.getStringExtra("cadenaCon");
        conexion = new Conexion(cadenaConexion);
        EMDKManager.getEMDKManager(getApplicationContext(), this);

        textLinea.setInputType(InputType.TYPE_NULL);
        textLocation.setInputType(InputType.TYPE_NULL);
        textMaster.setInputType(InputType.TYPE_NULL);
    }
    private void iniciarElementos(){
        textLinea = findViewById(R.id.editTextFila);
        textLocation = findViewById(R.id.editTextArea);
        textMaster = findViewById(R.id.editTextMaster);
        lineaLabel = findViewById(R.id.lbl_linea);

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




             if (result.matches("[a-zA-Z]+"))
             {
                 textLinea.setText(result);
                 textLocation.setText("");
                 textMaster.setText("");

                 return;

             }
                if (result.matches("[a-zA-Z]+\\d+"))
                {
                    if(!result.startsWith(textLinea.getText().toString().trim())){
                        mensaje("Esta localizacion no pertenece a la linea que escaneaste");
                        return;

                    }
                    textLocation.setText(result);
                    textMaster.setText("");
                    return;

                }

                if (!result.startsWith("4S")) {
                    mensaje("Dato incorrecto");
                    return;
                }

                if (textLinea.getText().toString().equals("") && textLocation.getText().toString().trim().equals("")){
                    if(!conexion.masterValido(result)){
                        mensaje("Master no valido");
                        textMaster.setText("");
                    }
                    else {
                        lineaLabel.setText(conexion.getLocacionPallet(result));
                    }
                    textMaster.setText(result);
                    return;

                }


                 //Una vez lleno los campos ahora si ponemos hacer las validaciones
                if (!textLinea.getText().toString().equals("")  && !textLocation.getText().toString().trim().equals("")){
                    textMaster.setText(result);


                    //validamos que el master sea valido
                    if(!conexion.masterValido(result)){
                        mensaje("Master no valido");
                        textMaster.setText("");
                        return;
                    }

                    if(!conexion.plantaCorrecta(result,textLocation.getText().toString().trim())){
                        mensaje("Este pallet es de otra planta, este no es su lugar");
                        textMaster.setText("");
                        return;
                    }
                    if(conexion.masterYaRegistrado(result,textLocation.getText().toString().trim())){

                      if(conexion.registrarDatos(textLocation.getText().toString().trim(), result,usuario.getUsuarioNick())) {

                          mensaje("Pallet guardado correctamente");

                          textLocation.setText("");
                          textLinea.setText("");
                          textMaster.setText("");
                          lineaLabel.setText("");
                          return;
                      }
                    }

                    if ( conexion.lugarOcupado(textLocation.getText().toString().trim()) ){
                        mensaje("Esta localizacion ya esta ocupada");
                        return;
                    }

                    if ( conexion.validaMasterRegistrado(result) ){
                        mensaje("Master ya registrado en otra localizacion");
                        return;
                    }
                    if(conexion.registrarDatos(textLocation.getText().toString().trim(), result,usuario.getUsuarioNick())) {

                        mensaje("Pallet guardado correctamente");
                        textLocation.setText("");
                        textLinea.setText("");
                        textMaster.setText("");
                        lineaLabel.setText("");
                    }




                }



            } catch (Exception e) {
               mensaje(e.getMessage());
            }
        });
    }

    public void guardaRegistros (String data) {
        try {

        }catch (Exception ex) {
            mensaje(ex.getMessage());
        }

    }
    public void mensaje(String mensaje) {


        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(mensaje);
        dlgAlert.setTitle("WGPL LOCATION");
        dlgAlert.create().show();

    }
}
