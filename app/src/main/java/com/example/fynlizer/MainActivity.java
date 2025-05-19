package com.example.fynlizer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fynlizer.Activities.GastosActivity;
import com.example.fynlizer.Activities.HomeActivity;
import com.example.fynlizer.Activities.NewAccountActivity;
import com.example.fynlizer.Activities.NewGastoActivity;
import com.example.fynlizer.Database.ConfigHandler;
import com.example.fynlizer.Database.LocalDatabaseHelper;
import com.example.fynlizer.Database.RemoteDatabaseHelper;
import com.example.fynlizer.Database.RemotePostgREST;
import com.example.fynlizer.Implementaciones.*;
import com.example.fynlizer.DAOClases.*;
import com.example.fynlizer.Session.SessionController;

import java.util.List;
import android.util.Log;




public class MainActivity extends AppCompatActivity {

    // Guardo aqui la conexion a la db local para trabajar con ella
    // No creo nuevas instancias en cada DAO por optimización

    private void iniciarSincronizacion() {
        Log.d("MainActivity", "Sincronización iniciada con PostgREST.");
        new RemotePostgREST(this);
    }

    private void configurarUsuarioPrimeraVez() {
        Log.i("TAG", "Configurar Usuario primera vez entrado");
        UsuarioDAO usuarioDAO = new UsuarioDAO(SessionController.dbInstance);
        List<Usuario>userlist = usuarioDAO.getAll();
        if(userlist.isEmpty()) {
            // Crear nuevo usuario
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.UUID = 0;
            nuevoUsuario.esUsuarioPremium = false;
            nuevoUsuario.estaSincronizado = false;
            nuevoUsuario.fechaUltimaSync = null;
            nuevoUsuario.CodigoRecuperacion = usuarioDAO.createRandomCode();

            int responseCode = (int)usuarioDAO.insert(nuevoUsuario);
            if(responseCode == -1) {
                Toast.makeText(this, "No se pudo crear un usuario. Reinicie la aplicación", Toast.LENGTH_LONG).show();
                finishAffinity();
            } else {
                SessionController.usuarioActual = nuevoUsuario;
            }

        } else { // Existe asi que lo establecemos en la sesión
            SessionController.usuarioActual = userlist.get(0);
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Dejar vacío para bloquear el volver atrás con la flechita
            }
        });

        // Bindear Boton Añadir Actividad
        Button addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NewAccountActivity.class);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // INSTANCIA BASE DE DATOS
        if(SessionController.dbInstance == null) {
            LocalDatabaseHelper localDbHelper = LocalDatabaseHelper.getInstance(this);
            SessionController.dbInstance = localDbHelper.getDatabase();
        }
        // INSTANCIA CONFIG
        if(SessionController.configInstance == null) {
            SessionController.configInstance = ConfigHandler.getInstance(getApplicationContext());
        }

        configurarUsuarioPrimeraVez();

        // Sincronización
        iniciarSincronizacion();

        CuentaDAO daoCuenta = new CuentaDAO(SessionController.dbInstance);
        List<Cuenta> cuentas = daoCuenta.getAll();

        LinearLayout linearLayoutContainer = findViewById(R.id.linear_layout_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Cuenta cuenta : cuentas) {
            View itemView = inflater.inflate(R.layout.item_cuenta, linearLayoutContainer, false);

            TextView userBalanceTextView = itemView.findViewById(R.id.user_balance);
            userBalanceTextView.setText(String.format("%s\n(%.2f €)", cuenta.nombreCuenta, cuenta.balanceTotal));
            // userBalanceTextView.setText(cuenta.nombreCuenta + "\n(" + cuenta.balanceTotal + "€)");

            linearLayoutContainer.addView(itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SessionController.cuentaActual = cuenta;
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(intent);
                }
            });

        }

    }
}