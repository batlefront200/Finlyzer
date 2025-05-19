package com.example.fynlizer.Activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fynlizer.DAOClases.CuentaDAO;
import com.example.fynlizer.DAOClases.MovimientoDAO;
import com.example.fynlizer.Implementaciones.Movimiento;
import com.example.fynlizer.R;
import com.example.fynlizer.Session.SessionController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.w3c.dom.Text;

import java.util.List;

public class GastosActivity extends AppCompatActivity {

    /* Si es negativo la suma es blanca, si es positivo es Verde Llamativo
    * Hay que calcular la suma total, se hace de forma descendiente para ello */

    private String formatFriendlyDate(String fechaMovimiento) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date date = dateFormat.parse(fechaMovimiento);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            SimpleDateFormat sdf = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
            String mesEnSpanish = sdf.format(date);
            mesEnSpanish = mesEnSpanish.substring(0, 1).toUpperCase() + mesEnSpanish.substring(1); // Capitaliza

            int dia = calendar.get(Calendar.DAY_OF_MONTH);
            int anio = calendar.get(Calendar.YEAR);
            int anioActual = Calendar.getInstance().get(Calendar.YEAR);

            if (anio == anioActual) {
                return dia + " de " + mesEnSpanish;
            } else {
                return dia + " de " + mesEnSpanish + " de " + anio;
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return fechaMovimiento;
        }
    }



    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gastos);

        CuentaDAO cuentaDao = new CuentaDAO(SessionController.dbInstance);

        View homeButton = findViewById(R.id.icon_home);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GastosActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        View statsButton = findViewById(R.id.icon_stats);
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GastosActivity.this, EstadisticasActivity.class);
                startActivity(intent);
            }
        });

        View settingsButton = findViewById(R.id.icon_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GastosActivity.this, AjustesActivity.class);
                startActivity(intent);
            }
        });

        View addbutton = findViewById(R.id.add_button);
        addbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GastosActivity.this, NewGastoActivity.class);
                startActivity(intent);
            }
        });


        /* Balance de cuenta */
        TextView balanceCuenta = findViewById(R.id.balance_amount);
        balanceCuenta.setText(String.format("%.2f €", SessionController.cuentaActual.balanceTotal));


        /* LISTADO DE GASTOS */
        LinearLayout transactionsContainer = findViewById(R.id.transactions_container);
        MovimientoDAO movimientoDAO = new MovimientoDAO(SessionController.dbInstance); /* Poner que se vayan cogiendo de 30 en 30 por optimizar */
        List<Movimiento> todosLosMovimientos = movimientoDAO.getAllOrderedByDateDescForAccount(SessionController.cuentaActual.idCuenta);
        double auxCurrency = SessionController.cuentaActual.balanceTotal;

        if(!todosLosMovimientos.isEmpty()) {
            for (Movimiento movaux : todosLosMovimientos) {
                LayoutInflater inflater = LayoutInflater.from(this);
                View transactionView = inflater.inflate(R.layout.transaction_item, transactionsContainer, false);

                TextView dateView = transactionView.findViewById(R.id.transaction_date);
                TextView descriptionView = transactionView.findViewById(R.id.transaction_description);
                TextView amountView = transactionView.findViewById(R.id.transaction_amount);
                TextView transaction_amount2 = transactionView.findViewById(R.id.transaction_amount2);

                dateView.setText(formatFriendlyDate(movaux.fechaMovimiento));
                descriptionView.setText(movaux.nombre);

                if(movaux.esUnGasto) {
                    amountView.setText(String.format("-%.2f €", movaux.cantidadMovida));
                    amountView.setTextColor(Color.WHITE);
                } else {
                    amountView.setText(String.format("+%.2f €", movaux.cantidadMovida));
                    amountView.setTextColor(Color.rgb(189, 243, 82));
                }


                if(auxCurrency < 0) {
                    transaction_amount2.setText(String.format("-%.2f€", Math.abs(auxCurrency)));
                } else {
                    transaction_amount2.setText(String.format("%.2f€", auxCurrency));
                }

                if(movaux.esUnGasto) {
                    auxCurrency += movaux.cantidadMovida;
                } else {
                    auxCurrency -= movaux.cantidadMovida;
                }


                transactionsContainer.addView(transactionView);

                transactionView.setOnLongClickListener(new View.OnLongClickListener() { // Bindeo mantener pulsado a su eliminación
                    @SuppressLint("UnsafeIntentLaunch")
                    @Override
                    public boolean onLongClick(View v) {
                        new android.app.AlertDialog.Builder(GastosActivity.this)
                                .setTitle("Confirmar eliminación")
                                .setMessage("¿Estás seguro de que deseas borrar este gasto?")
                                .setPositiveButton("Sí", (dialog, which) -> {

                                    if(movaux.esUnGasto) {
                                        SessionController.cuentaActual.balanceTotal += movaux.cantidadMovida;
                                    } else {
                                        SessionController.cuentaActual.balanceTotal -= movaux.cantidadMovida;
                                    }
                                    cuentaDao.update(SessionController.cuentaActual);

                                    movimientoDAO.delete(movaux.idMovimiento);
                                    finish();
                                    startActivity(getIntent());
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();

                        return true; // Indica que el evento fue consumido
                    }
                });

            }
        } /*else {
            LayoutInflater inflater = LayoutInflater.from(this);
            View transactionView = inflater.inflate(R.layout.transaction_item, transactionsContainer, false);

            TextView dateView = transactionView.findViewById(R.id.transaction_date);
            TextView descriptionView = transactionView.findViewById(R.id.transaction_description);
            TextView amountView = transactionView.findViewById(R.id.transaction_amount);
            TextView transaction_amount2 = transactionView.findViewById(R.id.transaction_amount2);

            dateView.setTextSize(0);
            amountView.setTextSize(0);
            descriptionView.setTextSize(26);
            transaction_amount2.setTextSize(0);
            descriptionView.setText("No hay gastos!");

            transactionsContainer.addView(transactionView);
        }*/

    }
}