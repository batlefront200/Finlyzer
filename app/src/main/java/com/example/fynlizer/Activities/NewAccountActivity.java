package com.example.fynlizer.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fynlizer.DAOClases.CuentaDAO;
import com.example.fynlizer.Implementaciones.Cuenta;
import com.example.fynlizer.MainActivity;
import com.example.fynlizer.R;
import com.example.fynlizer.Session.SessionController;

public class NewAccountActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_account);

        Button cancel_buton = findViewById(R.id.btnCancelar);
        cancel_buton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NewAccountActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        Button createButton = findViewById(R.id.btnCrear);
        /*createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NewAccountActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });*/

        CuentaDAO cuentaDao = new CuentaDAO(SessionController.dbInstance);

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText cuentaNombre = findViewById(R.id.editTextNombreCuenta);
                EditText balanceCuenta = findViewById(R.id.editTextBalance);

                double finalBalance;
                try {
                    // Meto el campo sustituyendo , por . para evitar errores de parseo
                    double balanceFloat = Double.parseDouble(balanceCuenta.getText().toString().replace(',', '.'));
                    balanceFloat = Math.round(balanceFloat * 100.0) / 100.0; // Redondeo a 2 decimales

                    String editableNombreCuenta = cuentaNombre.getText().toString();
                    if(cuentaDao.existeNombreCuentaParaUsuario(SessionController.usuarioActual.UUID, editableNombreCuenta)) {
                        Toast.makeText(NewAccountActivity.this, "Ya existe una cuenta con este nombre!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if(!editableNombreCuenta.isEmpty() & editableNombreCuenta.length() < 30) {

                        // Hace falta el UUID en session para crear la cuenta
                        Cuenta nuevaCuenta = new Cuenta();
                        nuevaCuenta.UUID = SessionController.usuarioActual.UUID;
                        nuevaCuenta.nombreCuenta = editableNombreCuenta;
                        nuevaCuenta.monedaSeleccionada = "EUR";
                        nuevaCuenta.balanceTotal = balanceFloat;

                        CuentaDAO cuentaDao = new CuentaDAO(SessionController.dbInstance);
                        float operationResponse = cuentaDao.insert(nuevaCuenta);

                        if(operationResponse == -1) {
                            Toast.makeText(NewAccountActivity.this, "No se pudo introducir el usuario", Toast.LENGTH_SHORT).show();
                        } else {
                            Intent intent = new Intent(NewAccountActivity.this, MainActivity.class);
                            startActivity(intent);
                            Toast.makeText(NewAccountActivity.this, "Cuenta creada!", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(NewAccountActivity.this, "El nombre no puede estar vacío ni superar los 30 carácteres", Toast.LENGTH_LONG).show();
                    }

                } catch (NumberFormatException nfe) {
                    Toast.makeText(NewAccountActivity.this, "No se pudo obtener el balance. Asegúrese de que sea un campo numérico.", Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}
