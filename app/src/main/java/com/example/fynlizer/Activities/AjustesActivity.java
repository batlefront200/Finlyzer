package com.example.fynlizer.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

        Button deleteButton = findViewById(R.id.delete_account);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CuentaDAO cuentaDAO = new CuentaDAO(SessionController.dbInstance);
                int deleteResult = cuentaDAO.delete(SessionController.cuentaActual.idCuenta);
                if(deleteResult == -1) {
                    Toast.makeText(AjustesActivity.this, "No se ha podido borrar la cuenta", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AjustesActivity.this, "Cuenta borrada correctamente!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AjustesActivity.this, MainActivity.class);
                    startActivity(intent);
                }

            }
        });

    }
}