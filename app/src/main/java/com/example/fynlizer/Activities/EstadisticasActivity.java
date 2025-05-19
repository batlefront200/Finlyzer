package com.example.fynlizer.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fynlizer.DAOClases.MovimientoDAO;
import com.example.fynlizer.Implementaciones.Movimiento;
import com.example.fynlizer.R;
import com.example.fynlizer.Session.SessionController;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Arrays;


public class EstadisticasActivity extends AppCompatActivity {

    private String convertirMes(String yyyyMM) {
        try {
            SimpleDateFormat sdfEntrada = new SimpleDateFormat("yyyy-MM", Locale.US);
            Date date = sdfEntrada.parse(yyyyMM);
            SimpleDateFormat sdfSalida = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
            return sdfSalida.format(date).substring(0, 1).toUpperCase() + sdfSalida.format(date).substring(1);
            // Esta salida es para poner la primera letra en mayusuca
        } catch (ParseException e) {
            return yyyyMM;
        }
    }

    private String[] calcularMayorFuenteIngreso(MovimientoDAO movimientoDAO, int idCuenta) {
        List<Movimiento> movimientos = movimientoDAO.getOnlyIngresosByCuentaEnMesActual(idCuenta);
        if (movimientos.isEmpty()) {
            return new String[]{"No hay ingresos", ""};
        }

        // Map para sumar los ingresos por nombre
        Map<String, Double> sumaPorNombre = new HashMap<>();
        double sumaTotalIngresos = 0;

        for (Movimiento m : movimientos) {
            sumaTotalIngresos += m.cantidadMovida;

            if (sumaPorNombre.containsKey(m.nombre)) {
                double sumaActual = sumaPorNombre.get(m.nombre);
                sumaPorNombre.put(m.nombre, sumaActual + m.cantidadMovida);
            } else {
                sumaPorNombre.put(m.nombre, m.cantidadMovida);
            }
        }

        // Encontrar la mayor fuente de ingresos
        String mayorFuenteIngreso = null;
        double mayorSuma = 0;

        for (Map.Entry<String, Double> entry : sumaPorNombre.entrySet()) {
            if (entry.getValue() > mayorSuma) {
                mayorFuenteIngreso = entry.getKey();
                mayorSuma = entry.getValue();
            }
        }

        // Calcular el porcentaje
        double porcentaje = (sumaTotalIngresos > 0) ? (mayorSuma / sumaTotalIngresos) * 100 : 0;

        // Formatear el porcentaje con dos decimales
        String porcentajeFormateado = String.format(Locale.US, "%.2f%%", porcentaje);

        return new String[]{mayorFuenteIngreso, porcentajeFormateado};
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estadisticas);

        // FOOTER BUTTONS SETUP
        // FOOTER BUTTONS SETUP
        // FOOTER BUTTONS SETUP
        View homeButton = findViewById(R.id.icon_home);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EstadisticasActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        View expensesButton = findViewById(R.id.icon_expenses);
        expensesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EstadisticasActivity.this, GastosActivity.class);
                startActivity(intent);
            }
        });

        View settingsButton = findViewById(R.id.icon_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EstadisticasActivity.this, AjustesActivity.class);
                startActivity(intent);
            }
        });


        MovimientoDAO movimientoDAO = new MovimientoDAO(SessionController.dbInstance);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.US);

        Map<String, Integer> progresoPorMes = new LinkedHashMap<>();
        for (int i = 3; i >= 0; i--) {
            Calendar mesTarget = (Calendar) calendar.clone();
            mesTarget.add(Calendar.MONTH, -i);
            String mes = sdf.format(mesTarget.getTime());

            double ingresos = 0;
            double gastos = 0;

            for (Movimiento m : movimientoDAO.getAllOrderedByDateDescForAccount(SessionController.cuentaActual.idCuenta)) {
                if (m.fechaMovimiento.startsWith(mes)) {
                    if (m.esUnGasto) {
                        gastos += m.cantidadMovida;
                    } else {
                        ingresos += m.cantidadMovida;
                    }
                }
            }

            int porcentaje = (ingresos + gastos == 0) ? 50 : (int) Math.round((ingresos / (ingresos + gastos)) * 100);
            progresoPorMes.put(mes, porcentaje);
        }

        // Ahora asignamos a las barras
        List<Integer> barrasId = Arrays.asList(R.id.primerBar, R.id.segundoBar, R.id.tercerBar, R.id.cuartoBar);
        List<Integer> textosId = Arrays.asList(R.id.primerMes, R.id.segundoMes, R.id.tercerMes, R.id.cuartoMes);

        int index = 0;
        for (Map.Entry<String, Integer> entry : progresoPorMes.entrySet()) {
            String mes = entry.getKey();
            int progreso = entry.getValue();

            ProgressBar bar = findViewById(barrasId.get(index));
            bar.setProgress(progreso);

            TextView labelMes = findViewById(textosId.get(index));
            labelMes.setText(convertirMes(mes));

            index++;
        }


        // GASTO MAS FRECUENTE DEL MES
        String[] resultadoGastoFrecuente = calcularMayorFuenteIngreso(movimientoDAO, SessionController.cuentaActual.idCuenta);
        TextView gastoFrecuenteTextView = findViewById(R.id.transaction_description);
        TextView porcentajeGasto = findViewById(R.id.transaction_amount);

        gastoFrecuenteTextView.setText(resultadoGastoFrecuente[0]);
        porcentajeGasto.setText(resultadoGastoFrecuente[1]);


        /* ESTADISTICAS GRAFICOS */
        /* ESTADISTICAS GRAFICOS */
        /* ESTADISTICAS GRAFICOS */

        /* Porcentaje de ingresos respecto al total de movimientos */
        TextView porcentajeIngresosLabel = findViewById(R.id.circle1_text);
        ProgressBar barraPorcentajeIngresos = findViewById(R.id.circle1_progress);

        List<Movimiento> ingresosMes = movimientoDAO.getOnlyIngresosByCuentaEnMesActual(SessionController.cuentaActual.idCuenta);
        List<Movimiento> todosMovimientosMes = movimientoDAO.getMovimientosByCuentaEnMesActual(SessionController.cuentaActual.idCuenta);

        /* Obtener Ingresos y Gastos este mes */
        MovimientoDAO mvDao = new MovimientoDAO(SessionController.dbInstance);
        List<Movimiento> movimientosEsteMes = mvDao.getMovimientosByCuentaEnMesActual(SessionController.cuentaActual.idCuenta);

        double ingresos = 0;
        double gastos = 0;

        for (Movimiento movimientoActual : movimientosEsteMes) {
            if(movimientoActual.esUnGasto) {
                gastos += movimientoActual.cantidadMovida;
            } else {
                ingresos += movimientoActual.cantidadMovida;
            }
        }


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

        List<Movimiento> movimientosAnuales = movimientoDAO.getMovimientosByCuentaEnAnoActual(SessionController.cuentaActual.idCuenta);
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

    }
}