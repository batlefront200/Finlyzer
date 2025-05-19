package com.example.fynlizer.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.fynlizer.DAOClases.CuentaDAO;
import com.example.fynlizer.DAOClases.MovimientoDAO;
import com.example.fynlizer.Implementaciones.Cuenta;
import com.example.fynlizer.Implementaciones.Movimiento;
import com.example.fynlizer.MainActivity;
import com.example.fynlizer.R;
import com.example.fynlizer.Session.SessionController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewGastoActivity extends AppCompatActivity {

    private static boolean isGastoSelected = false;
    private static float alphaValue = 0.2f;
    private static void updateButtonsState(boolean isGastoSelected, LinearLayout ingresoButton, LinearLayout gastoButton) {
        if(isGastoSelected) {
            gastoButton.setAlpha(1);
            ingresoButton.setAlpha(alphaValue);
        } else {
            gastoButton.setAlpha(alphaValue);
            ingresoButton.setAlpha(1);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_gasto);

        MovimientoDAO movimientoDAO = new MovimientoDAO(SessionController.dbInstance);

        View cancelButton = findViewById(R.id.btnCancelar);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NewGastoActivity.this, GastosActivity.class);
                startActivity(intent);
            }
        });

        LinearLayout ingresoBtn = findViewById(R.id.ingreso_btn);
        LinearLayout gastoBtn = findViewById(R.id.gasto_btn);
        // boolean isGastoSelected = false; -> La declaro arriba para poder acceder en el click listener
        updateButtonsState(isGastoSelected, ingresoBtn, gastoBtn);

        ingresoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isGastoSelected = !isGastoSelected;
                updateButtonsState(isGastoSelected, ingresoBtn, gastoBtn);
            }
        });

        gastoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isGastoSelected = !isGastoSelected;
                updateButtonsState(isGastoSelected, ingresoBtn, gastoBtn);
            }
        });

        // CARGAR CAMPO DE AUTO_COMPLETADO
        if(SessionController.configInstance.isAutocompleteEnabled()) {
            AutoCompleteTextView nombreGastoView = findViewById(R.id.editTextNombreGasto);

            List<String> nombresUsados = movimientoDAO.getAllMovementNames(SessionController.cuentaActual.idCuenta);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, nombresUsados);
            nombreGastoView.setAdapter(adapter);
        }


        AppCompatButton createGastoBtn = findViewById(R.id.btnCrearMovimiento);
        EditText nombreGastoField = findViewById(R.id.editTextNombreGasto);
        EditText cantidadMovidaField = findViewById(R.id.editTextGasto);
        double finalBalance;

        createGastoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Meto el campo sustituyendo , por . para evitar errores de parseo
                    double balanceFloat = Double.parseDouble(cantidadMovidaField.getText().toString().replace(',', '.'));
                    balanceFloat = Math.round(balanceFloat * 100.0) / 100.0; // Redondeo a 2 decimales

                    String editableNombreMovimiento = nombreGastoField.getText().toString();

                    if(!editableNombreMovimiento.isEmpty() & editableNombreMovimiento.length() < 30) {

                        Movimiento nuevoMovimiento = new Movimiento();
                        nuevoMovimiento.idCuenta = SessionController.cuentaActual.idCuenta;
                        nuevoMovimiento.nombre = editableNombreMovimiento;
                        nuevoMovimiento.cantidadMovida = balanceFloat;
                        nuevoMovimiento.esUnGasto = isGastoSelected;

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                        nuevoMovimiento.fechaMovimiento = dateFormat.format(new Date());

                        // Quitamos o sumamos la cantidad correspondiente del balance total
                        if(isGastoSelected) {
                            SessionController.cuentaActual.balanceTotal -= balanceFloat;
                        } else {
                            SessionController.cuentaActual.balanceTotal += balanceFloat;
                        }
                        if(!SessionController.guardarCuentaActual()) { return; }


                        float operationResponse = movimientoDAO.insert(nuevoMovimiento);

                        if(operationResponse == -1) {
                            Toast.makeText(NewGastoActivity.this, "No se pudo introducir el gasto", Toast.LENGTH_SHORT).show();
                        } else {
                            Intent intent = new Intent(NewGastoActivity.this, GastosActivity.class);
                            startActivity(intent);
                            Toast.makeText(NewGastoActivity.this, "Gasto creado!", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(NewGastoActivity.this, "El nombre no puede estar vacío ni superar los 30 carácteres", Toast.LENGTH_LONG).show();
                    }

                } catch (NumberFormatException nfe) {
                    Toast.makeText(NewGastoActivity.this, "No se pudo obtener el balance. Asegúrese de que sea un valor numérico.", Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}