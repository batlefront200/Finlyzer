package com.example.fynlizer.Database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.example.fynlizer.Implementaciones.Usuario;
import com.example.fynlizer.Session.SessionController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RemoteDatabaseHelper { // DB: WMioqAQuNCqER1jS3rL857mw

    private static final String TAG = "RemoteDatabaseHelper";
    private static final String JDBC_URL = "jdbc:postgresql://db.uxtrypgceiwzcyyrprfc.supabase.co:5432/postgres?sslmode=require";
    private static final String JDBC_USER = "postgres";
    private static final String JDBC_PASSWORD = "WMioqAQuNCqER1jS3rL857mw";

    private final Context context;

    public RemoteDatabaseHelper(Context context) {
        this.context = context;
        if (isNetworkAvailableStatic(this.context)) {
            new SyncTask().execute();
        } else {
            Log.d(TAG, "No hay conexión a internet.");
        }
    }

    private static boolean isNetworkAvailableStatic(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private class SyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            SQLiteDatabase localDb = LocalDatabaseHelper.getInstance(context).getDatabase();
            Connection remoteConnection = null;

            try {
                Class.forName("org.postgresql.Driver");
                remoteConnection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);

                Usuario currentUser = SessionController.usuarioActual;
                if (currentUser == null) {
                    Log.e(TAG, "No hay usuario activo en la sesión.");
                    return null;
                }

                if (currentUser.fechaUltimaSync == null) {
                    // Primera sincronización
                    syncNewUser(remoteConnection, currentUser);
                } else {
                    // Usuario ya sincronizado previamente
                    syncExistingUser(remoteConnection, localDb, currentUser);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error durante la sincronización remota", e);
            } finally {
                if (remoteConnection != null) {
                    try {
                        remoteConnection.close();
                    } catch (SQLException e) {
                        Log.e(TAG, "Error cerrando conexión remota", e);
                    }
                }
            }
            return null;
        }

        private void syncNewUser(Connection remoteConnection, Usuario currentUser) throws SQLException {
            String insertSQL = "INSERT INTO USUARIO (CodigoRecuperacion, esUsuarioPremium, estaSincronizado, fechaUltimaSync) " +
                    "VALUES (?, ?, ?, ?) RETURNING UUID";

            try (PreparedStatement stmt = remoteConnection.prepareStatement(insertSQL)) {
                stmt.setString(1, currentUser.CodigoRecuperacion);
                stmt.setBoolean(2, currentUser.esUsuarioPremium);
                stmt.setBoolean(3, true);
                stmt.setDate(4, new java.sql.Date(new Date().getTime()));

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int remoteId = rs.getInt("UUID");
                    SessionController.configInstance.setSyncId(remoteId);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                    currentUser.fechaUltimaSync = dateFormat.format(new Date());

                    Log.d(TAG, "Usuario sincronizado con UUID remoto: " + remoteId);
                }
            }
        }

        private void syncExistingUser(Connection remoteConnection, SQLiteDatabase localDb, Usuario currentUser) throws SQLException {
            int syncId = SessionController.configInstance.getSyncId();
            if (syncId == -1) {
                Log.e(TAG, "No se encontró el ID de sincronización para el usuario.");
                return;
            }

            syncLocalToRemote(remoteConnection, localDb, syncId);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            currentUser.fechaUltimaSync = dateFormat.format(new Date());

            Log.d(TAG, "Sincronización completada para el usuario existente.");
        }

        private void syncLocalToRemote(Connection remoteConnection, SQLiteDatabase localDb, int syncId) throws SQLException {
            Cursor cursor;

            // Sincronización de USUARIO
            cursor = localDb.rawQuery("SELECT * FROM USUARIO WHERE UUID = ?", new String[]{String.valueOf(syncId)});
            if (cursor.moveToFirst()) {
                String updateSQL = "UPDATE USUARIO SET CodigoRecuperacion = ?, esUsuarioPremium = ?, estaSincronizado = ?, fechaUltimaSync = ? WHERE UUID = ?";

                try (PreparedStatement stmt = remoteConnection.prepareStatement(updateSQL)) {
                    stmt.setString(1, cursor.getString(cursor.getColumnIndexOrThrow("CodigoRecuperacion")));
                    stmt.setBoolean(2, cursor.getInt(cursor.getColumnIndexOrThrow("esUsuarioPremium")) > 0);
                    stmt.setBoolean(3, true);
                    stmt.setDate(4, new java.sql.Date(new Date().getTime()));
                    stmt.setInt(5, syncId);
                    stmt.executeUpdate();
                }
            }
            cursor.close();

            // Sincronización de CUENTA
            cursor = localDb.rawQuery("SELECT * FROM CUENTA WHERE UUID = ?", new String[]{String.valueOf(syncId)});
            while (cursor.moveToNext()) {
                String insertSQL = "INSERT INTO CUENTA (idCuenta, nombreCuenta, balanceTotal, monedaSeleccionada, fechaUltimoMovimiento, UUID) " +
                        "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (idCuenta) DO UPDATE SET " +
                        "nombreCuenta = EXCLUDED.nombreCuenta, " +
                        "balanceTotal = EXCLUDED.balanceTotal, " +
                        "monedaSeleccionada = EXCLUDED.monedaSeleccionada, " +
                        "fechaUltimoMovimiento = EXCLUDED.fechaUltimoMovimiento";

                try (PreparedStatement stmt = remoteConnection.prepareStatement(insertSQL)) {
                    stmt.setInt(1, cursor.getInt(cursor.getColumnIndexOrThrow("idCuenta")));
                    stmt.setString(2, cursor.getString(cursor.getColumnIndexOrThrow("nombreCuenta")));
                    stmt.setDouble(3, cursor.getDouble(cursor.getColumnIndexOrThrow("balanceTotal")));
                    stmt.setString(4, cursor.getString(cursor.getColumnIndexOrThrow("monedaSeleccionada")));
                    stmt.setDate(5, java.sql.Date.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("fechaUltimoMovimiento"))));
                    stmt.setInt(6, syncId);
                    stmt.executeUpdate();
                }
            }
            cursor.close();

            // Sincronización de MOVIMIENTOS
            cursor = localDb.rawQuery("SELECT * FROM MOVIMIENTOS WHERE idCuenta IN (SELECT idCuenta FROM CUENTA WHERE UUID = ?)", new String[]{String.valueOf(syncId)});
            while (cursor.moveToNext()) {
                String insertSQL = "INSERT INTO MOVIMIENTOS (idMovimiento, nombre, fechaMovimiento, cantidadMovida, esUnGasto, idCuenta) " +
                        "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (idMovimiento) DO UPDATE SET " +
                        "nombre = EXCLUDED.nombre, " +
                        "fechaMovimiento = EXCLUDED.fechaMovimiento, " +
                        "cantidadMovida = EXCLUDED.cantidadMovida, " +
                        "esUnGasto = EXCLUDED.esUnGasto";

                try (PreparedStatement stmt = remoteConnection.prepareStatement(insertSQL)) {
                    stmt.setInt(1, cursor.getInt(cursor.getColumnIndexOrThrow("idMovimiento")));
                    stmt.setString(2, cursor.getString(cursor.getColumnIndexOrThrow("nombre")));
                    stmt.setDate(3, java.sql.Date.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("fechaMovimiento"))));
                    stmt.setDouble(4, cursor.getDouble(cursor.getColumnIndexOrThrow("cantidadMovida")));
                    stmt.setBoolean(5, cursor.getInt(cursor.getColumnIndexOrThrow("esUnGasto")) > 0);
                    stmt.setInt(6, cursor.getInt(cursor.getColumnIndexOrThrow("idCuenta")));
                    stmt.executeUpdate();
                }
            }
            cursor.close();
        }
    }
}
