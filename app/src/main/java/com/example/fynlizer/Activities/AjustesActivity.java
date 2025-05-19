package com.example.fynlizer.Activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fynlizer.DAOClases.CuentaDAO;
import com.example.fynlizer.DAOClases.UsuarioDAO;
import com.example.fynlizer.Implementaciones.Cuenta;
import com.example.fynlizer.MainActivity;
import com.example.fynlizer.R;
import com.example.fynlizer.Session.SessionController;

public class AjustesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);

        CuentaDAO cuentaDAO = new CuentaDAO(SessionController.dbInstance);

        View homeButton = findViewById(R.id.icon_home);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AjustesActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        View expensesButton = findViewById(R.id.icon_expenses);
        expensesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AjustesActivity.this, GastosActivity.class);
                startActivity(intent);
            }
        });

        View statsButton = findViewById(R.id.icon_stats);
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AjustesActivity.this, EstadisticasActivity.class);
                startActivity(intent);
            }
        });

        // Update Switches
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch autoSync = findViewById(R.id.sync_switch);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch autoComplete = findViewById(R.id.autocomplete_switch);

        autoSync.setChecked(SessionController.configInstance.isSyncEnabled());
        autoComplete.setChecked(SessionController.configInstance.isAutocompleteEnabled());

        autoSync.setOnCheckedChangeListener((buttonView, isChecked) -> { SessionController.configInstance.setSyncEnabled(isChecked); });
        autoComplete.setOnCheckedChangeListener((buttonView, isChecked) -> { SessionController.configInstance.setAutocompleteEnabled(isChecked); });


        Button changeBalanceBtn = findViewById(R.id.change_balance_btn);
        EditText nuevoBalance = findViewById(R.id.new_balance);

        changeBalanceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new androidx.appcompat.app.AlertDialog.Builder(AjustesActivity.this)
                        .setTitle("¿Cambiar Balance?")
                        .setMessage("Esta acción cambiará el balance de la cuenta sin dejar un registro de movimiento. Puede llegar a afectar las estadísticas ¿Estás seguro?")
                        .setPositiveButton("Sí, cambiar", (dialog, which) -> {

                            double finalBalance;
                            try {
                                // Meto el campo sustituyendo , por . para evitar errores de parseo
                                double balanceFloat = Double.parseDouble(nuevoBalance.getText().toString().replace(',', '.'));
                                balanceFloat = Math.round(balanceFloat * 100.0) / 100.0; // Redondeo a 2 decimales

                                SessionController.cuentaActual.balanceTotal = balanceFloat;
                                cuentaDAO.update(SessionController.cuentaActual);

                                Toast.makeText(AjustesActivity.this, "Balance modificado correctamente!", Toast.LENGTH_SHORT).show();

                            } catch (Exception e) {
                                Toast.makeText(AjustesActivity.this, "No se pudo modificar el balance", Toast.LENGTH_SHORT).show();
                            }

                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            }
        });


        Button deleteButton = findViewById(R.id.delete_account);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new androidx.appcompat.app.AlertDialog.Builder(AjustesActivity.this)
                        .setTitle("¿Borrar cuenta?")
                        .setMessage("Esta acción eliminará permanentemente tu cuenta y todos los movimientos asociados. ¿Estás seguro?")
                        .setPositiveButton("Sí, borrar", (dialog, which) -> {

                            int deleteResult = cuentaDAO.delete(SessionController.cuentaActual.idCuenta);
                            if(deleteResult == -1) {
                                Toast.makeText(AjustesActivity.this, "No se ha podido borrar la cuenta", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AjustesActivity.this, "Cuenta borrada correctamente!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(AjustesActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            }
        });

    }
}