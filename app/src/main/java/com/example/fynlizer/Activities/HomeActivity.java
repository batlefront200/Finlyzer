package com.example.fynlizer.Activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fynlizer.DAOClases.MovimientoDAO;
import com.example.fynlizer.Implementaciones.Movimiento;
import com.example.fynlizer.R;
import com.example.fynlizer.Session.SessionController;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    private static double ingresos = 0;
    private static double gastos = 0;

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Reseteamos los valores para que no se acumulen
        ingresos = 0;
        gastos = 0;

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
            balanceActual.setTextColor(Color.RED);
            balanceActual.setText(String.format("%.2f€", SessionController.cuentaActual.balanceTotal));
        } else {
            balanceActual.setText(String.format("%.2f€", SessionController.cuentaActual.balanceTotal));
        }

        /* Obtener Ingresos y Gastos este mes */
        MovimientoDAO mvDao = new MovimientoDAO(SessionController.dbInstance);
        List<Movimiento> movimientosEsteMes = mvDao.getMovimientosByCuentaEnMesActual(SessionController.cuentaActual.idCuenta);

        TextView ingresosTotal = findViewById(R.id.ingresos_total);
        TextView gastosTotal = findViewById(R.id.gastos_total);

        for (Movimiento movimientoActual : movimientosEsteMes) {
            if(movimientoActual.esUnGasto) {
                gastos += movimientoActual.cantidadMovida;
            } else {
                ingresos += movimientoActual.cantidadMovida;
            }
        }

        ingresosTotal.setText(String.format("+ %.2f €", ingresos));
        gastosTotal.setText(String.format("- %.2f €", gastos));

        /* HACER LA BARRA QUE SEA REAL E INTERACTIVA */
        ProgressBar progressBarGastos = findViewById(R.id.progressBarIngresosGastos);

        if(ingresos == 0 & gastos == 0) {
            progressBarGastos.setProgress(50);
        } else if(ingresos == 0 & gastos != 0) {
            progressBarGastos.setProgress(0);
        } else if(ingresos != 0 & gastos == 0) {
            progressBarGastos.setProgress(100);
        } else {
            double totalMovida = ingresos + gastos;
            progressBarGastos.setProgress((int) Math.round(ingresos * 100 / totalMovida));
        }


        /* Último movimiento */
        Movimiento ultimoGasto = mvDao.getUltimoMovimientoById(SessionController.cuentaActual.idCuenta);
        TextView uGastoContent = findViewById(R.id.last_movement_detail);
        TextView uGastoLabel = findViewById(R.id.last_movement_label);

        if(ultimoGasto == null) {
            uGastoContent.setTextSize(0);
            uGastoLabel.setText("No hay gastos este mes");
            // uGastoLabel.setTextSize(28);
        } else {
            StringBuilder gastoString = new StringBuilder();

            gastoString.append(ultimoGasto.nombre);
            gastoString.append(" (");

            if(ultimoGasto.esUnGasto) {
                gastoString.append("-");
                uGastoContent.setTextColor(Color.RED);
            } else {
                gastoString.append("+");
                uGastoContent.setTextColor(Color.GREEN);
            }
            gastoString.append(String.format("%.2f €)", ultimoGasto.cantidadMovida));
            uGastoContent.setText(gastoString.toString());
        }

        /* ESTADISTICAS GRAFICOS */
        /* ESTADISTICAS GRAFICOS */
        /* ESTADISTICAS GRAFICOS */

        /* Porcentaje de ingresos respecto al total de movimientos */
        TextView porcentajeIngresosLabel = findViewById(R.id.circle1_text);
        ProgressBar barraPorcentajeIngresos = findViewById(R.id.circle1_progress);

        List<Movimiento> ingresosMes = mvDao.getOnlyIngresosByCuentaEnMesActual(SessionController.cuentaActual.idCuenta);
        List<Movimiento> todosMovimientosMes = mvDao.getMovimientosByCuentaEnMesActual(SessionController.cuentaActual.idCuenta);

        double sumaIngresos = ingresos;
        double totalMovido = ingresos + gastos;

        int porcentajeIngresos = 0;
        if (totalMovido > 0) {
            porcentajeIngresos = (int) Math.round((sumaIngresos * 100.0) / totalMovido);
        }

        if(sumaIngresos > 99.99) {
            porcentajeIngresosLabel.setTextSize(26);
        }

        barraPorcentajeIngresos.setProgress(porcentajeIngresos);
        porcentajeIngresosLabel.setText(String.format(Locale.US, "+%.2f€", sumaIngresos, porcentajeIngresos));


        /* Porcentaje de ingresos respecto al total del año */
        TextView porcentajeAnualLabel = findViewById(R.id.circle2_text);
        ProgressBar barraPorcentajeAnual = findViewById(R.id.circle2_progress);

        List<Movimiento> movimientosAnuales = mvDao.getMovimientosByCuentaEnAnoActual(SessionController.cuentaActual.idCuenta);
        double ingresosAnuales = 0.0;
        double gastosAnuales = 0.0;

        for (Movimiento movimiento : movimientosAnuales) {
            if (movimiento.esUnGasto) {
                gastosAnuales += movimiento.cantidadMovida;
            } else {
                ingresosAnuales += movimiento.cantidadMovida;
            }
        }

        double totalAnualMovido = ingresosAnuales + gastosAnuales;
        int porcentajeAnual = 0;

        if (totalAnualMovido > 0) {
            porcentajeAnual = (int) Math.round((ingresosAnuales * 100.0) / totalAnualMovido);
        }

        if(ingresosAnuales > 99.99) {
            porcentajeAnualLabel.setTextSize(24);
        }

        barraPorcentajeAnual.setProgress(porcentajeAnual);
        porcentajeAnualLabel.setText(String.format(Locale.US, "+%.2f€", ingresosAnuales));




        /* Mes Completado */
        TextView mesProgresoText = findViewById(R.id.progreso_mes);
        ProgressBar pbMes = findViewById(R.id.linear_progress_bar);

        Calendar calendario = Calendar.getInstance();
        int diaDelMes = calendario.get(Calendar.DAY_OF_MONTH);
        int diasEnEsteMes = calendario.getActualMaximum(Calendar.DAY_OF_MONTH);

        int porcentajeMes = (int) Math.round((diaDelMes * 100.0) / diasEnEsteMes);
        mesProgresoText.setText("Mes Completado: " + porcentajeMes + "%");
        pbMes.setProgress(porcentajeMes);

    }
}
