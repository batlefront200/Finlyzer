package com.example.fynlizer.Database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RemoteDatabaseHelper {

    private static final String TAG = "RemoteDatabaseHelper";
    private static final String JDBC_URL = "jdbc:postgresql://ep-morning-hall-a2xkpwkn-pooler.eu-central-1.aws.neon.tech/fynlizerdb";
    private static final String JDBC_USER = "fynlizerdb_owner";
    private static final String JDBC_PASSWORD = "npg_98tHObxoXcpA";
    private final Context context;

    public RemoteDatabaseHelper(Context context) {
        this.context = context;
        if (isNetworkAvailable()) {
            new SyncTask().execute();
        } else {
            Log.d(TAG, "No hay conexión a internet.");
        }
    }

    private boolean isNetworkAvailable() {
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

                Cursor cursor = localDb.rawQuery("SELECT * FROM USUARIO", null);

                /* VOY A USAR getColumnIndexOrThrow EN VEZ DE getColumnIndex porque sino da error */
                while (cursor.moveToNext()) {
                    int uuid = cursor.getInt(cursor.getColumnIndexOrThrow("UUID"));
                    String codigoRecuperacion = cursor.getString(cursor.getColumnIndexOrThrow("CodigoRecuperacion"));
                    boolean esPremium = cursor.getInt(cursor.getColumnIndexOrThrow("esUsuarioPremium")) > 0;
                    boolean estaSync = cursor.getInt(cursor.getColumnIndexOrThrow("estaSincronizado")) > 0;
                    String fechaSync = cursor.getString(cursor.getColumnIndexOrThrow("fechaUltimaSync"));

                    String insertSQL = "INSERT INTO USUARIO (UUID, CodigoRecuperacion, esUsuarioPremium, estaSincronizado, fechaUltimaSync) " +
                            "VALUES (?, ?, ?, ?, ?) ON CONFLICT (UUID) DO UPDATE SET " +
                            "CodigoRecuperacion = EXCLUDED.CodigoRecuperacion, " +
                            "esUsuarioPremium = EXCLUDED.esUsuarioPremium, " +
                            "estaSincronizado = EXCLUDED.estaSincronizado, " +
                            "fechaUltimaSync = EXCLUDED.fechaUltimaSync";

                    try (PreparedStatement stmt = remoteConnection.prepareStatement(insertSQL)) {
                        stmt.setInt(1, uuid);
                        stmt.setString(2, codigoRecuperacion);
                        stmt.setBoolean(3, esPremium);
                        stmt.setBoolean(4, estaSync);
                        stmt.setDate(5, java.sql.Date.valueOf(fechaSync));
                        stmt.executeUpdate();
                    }
                }
                cursor.close();
                Log.d(TAG, "Sincronización de USUARIO completa.");

                cursor = localDb.rawQuery("SELECT * FROM CUENTA", null);
                while (cursor.moveToNext()) {
                    int idCuenta = cursor.getInt(cursor.getColumnIndexOrThrow("idCuenta"));
                    String nombreCuenta = cursor.getString(cursor.getColumnIndexOrThrow("nombreCuenta"));
                    double balanceTotal = cursor.getDouble(cursor.getColumnIndexOrThrow("balanceTotal"));
                    String monedaSeleccionada = cursor.getString(cursor.getColumnIndexOrThrow("monedaSeleccionada"));
                    String fechaUltimoMovimiento = cursor.getString(cursor.getColumnIndexOrThrow("fechaUltimoMovimiento"));
                    int uuid = cursor.getInt(cursor.getColumnIndexOrThrow("UUID"));

                    String insertSQL = "INSERT INTO CUENTA (idCuenta, nombreCuenta, balanceTotal, monedaSeleccionada, fechaUltimoMovimiento, UUID) " +
                            "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (idCuenta) DO UPDATE SET " +
                            "nombreCuenta = EXCLUDED.nombreCuenta, " +
                            "balanceTotal = EXCLUDED.balanceTotal, " +
                            "monedaSeleccionada = EXCLUDED.monedaSeleccionada, " +
                            "fechaUltimoMovimiento = EXCLUDED.fechaUltimoMovimiento, " +
                            "UUID = EXCLUDED.UUID";

                    try (PreparedStatement stmt = remoteConnection.prepareStatement(insertSQL)) {
                        stmt.setInt(1, idCuenta);
                        stmt.setString(2, nombreCuenta);
                        stmt.setDouble(3, balanceTotal);
                        stmt.setString(4, monedaSeleccionada);
                        stmt.setDate(5, java.sql.Date.valueOf(fechaUltimoMovimiento));
                        stmt.setInt(6, uuid);
                        stmt.executeUpdate();
                    }
                }
                cursor.close();
                Log.d(TAG, "Sincronización de CUENTA completa.");

                cursor = localDb.rawQuery("SELECT * FROM MOVIMIENTOS", null);
                while (cursor.moveToNext()) {
                    int idMovimiento = cursor.getInt(cursor.getColumnIndexOrThrow("idMovimiento"));
                    String nombreFecha = cursor.getString(cursor.getColumnIndexOrThrow("nombreFecha"));
                    double cantidadMovida = cursor.getDouble(cursor.getColumnIndexOrThrow("cantidadMovida"));
                    boolean esUnGasto = cursor.getInt(cursor.getColumnIndexOrThrow("esUnGasto")) > 0;

                    String insertSQL = "INSERT INTO MOVIMIENTOS (idMovimiento, nombreFecha, cantidadMovida, esUnGasto) " +
                            "VALUES (?, ?, ?, ?) ON CONFLICT (idMovimiento) DO UPDATE SET " +
                            "nombreFecha = EXCLUDED.nombreFecha, " +
                            "cantidadMovida = EXCLUDED.cantidadMovida, " +
                            "esUnGasto = EXCLUDED.esUnGasto";

                    try (PreparedStatement stmt = remoteConnection.prepareStatement(insertSQL)) {
                        stmt.setInt(1, idMovimiento);
                        stmt.setString(2, nombreFecha);
                        stmt.setDouble(3, cantidadMovida);
                        stmt.setBoolean(4, esUnGasto);
                        stmt.executeUpdate();
                    }
                }
                cursor.close();
                Log.d(TAG, "Sincronización de MOVIMIENTOS completa.");

                cursor = localDb.rawQuery("SELECT * FROM CUENTA_MOVIMIENTO", null);
                while (cursor.moveToNext()) {
                    int idCuenta = cursor.getInt(cursor.getColumnIndexOrThrow("idCuenta"));
                    int idMovimiento = cursor.getInt(cursor.getColumnIndexOrThrow("idMovimiento"));

                    String insertSQL = "INSERT INTO CUENTA_MOVIMIENTO (idCuenta, idMovimiento) " +
                            "VALUES (?, ?) ON CONFLICT (idCuenta, idMovimiento) DO NOTHING";

                    try (PreparedStatement stmt = remoteConnection.prepareStatement(insertSQL)) {
                        stmt.setInt(1, idCuenta);
                        stmt.setInt(2, idMovimiento);
                        stmt.executeUpdate();
                    }
                }
                cursor.close();
                Log.d(TAG, "Sincronización de CUENTA_MOVIMIENTO completa.");


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
    }
}
