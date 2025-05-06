package com.example.fynlizer.Activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fynlizer.R;
import com.example.fynlizer.Session.SessionController;
import android.graphics.Color;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // FOOTER BUTTONS SETUP
        // FOOTER BUTTONS SETUP
        // FOOTER BUTTONS SETUP
        View expensesButton = findViewById(R.id.icon_expenses);
        expensesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, GastosActivity.class);
                startActivity(intent);
            }
        });

        View statsButton = findViewById(R.id.icon_stats);
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, EstadisticasActivity.class);
                startActivity(intent);
            }
        });

        View settingsButton = findViewById(R.id.icon_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, AjustesActivity.class);
                startActivity(intent);
            }
        });

        // HOME
        TextView headerText = findViewById(R.id.user_name);
        headerText.setText(SessionController.cuentaActual.nombreCuenta);

        TextView balanceActual = findViewById(R.id.balance_amount);
        StringBuilder balanceText = new StringBuilder();
        if(SessionController.cuentaActual.balanceTotal < 0) {
            balanceText.append("-");
            balanceActual.setTextColor(Color.RED);
        } else {
            balanceText.append("+");
        }
        balanceText.append(SessionController.cuentaActual.balanceTotal);
        balanceText.append("€");
        balanceActual.setText(balanceText.toString());


    }
}
