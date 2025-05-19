package com.example.fynlizer.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.example.fynlizer.DAOClases.UsuarioDAO;
import com.example.fynlizer.Implementaciones.Usuario;
import com.example.fynlizer.Session.SessionController;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.*;

public class RemotePostgREST {

    private static final String TAG = "RemotePostgREST";
    private static final String SUPABASE_URL = "https://uxtrypgceiwzcyyrprfc.supabase.co/rest/v1/";
    private static final String SERVICE_ROLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV4dHJ5cGdjZWl3emN5eXJwcmZjIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NzY4ODQ1MiwiZXhwIjoyMDYzMjY0NDUyfQ.HgkKKX4YGWBkPHp-bhjl8xQKoVqSfKA3h4hhX8RtE9U";
    private final Context context;

    private static UsuarioDAO usuarioDAO;

    public RemotePostgREST(Context context) {
        this.context = context;

        if (isNetworkAvailableStatic(this.context)) {
            usuarioDAO = new UsuarioDAO(SessionController.dbInstance);
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
            Usuario currentUser = SessionController.usuarioActual;
            if (currentUser == null) {
                Log.e(TAG, "No hay usuario activo en la sesión.");
                return null;
            }

            Log.d(TAG, " [!] ID de configuracion: " + SessionController.configInstance.getSyncId());
            Log.d(TAG, " [!] Fecha Ultima Sync: " + SessionController.usuarioActual.fechaUltimaSync);

            try {
                if (currentUser.fechaUltimaSync == null) {
                    syncNewUser(localDb, currentUser);
                } else {
                    // Usuario ya sincronizado previamente
                    syncExistingUser(localDb, currentUser);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error durante la sincronización remota", e);
            }
            return null;
        }

        private void syncNewUser(SQLiteDatabase localDb, Usuario currentUser) throws Exception {
            JSONObject userJson = new JSONObject();
            userJson.put("codigorecuperacion", currentUser.CodigoRecuperacion);
            userJson.put("esusuariopremium", currentUser.esUsuarioPremium);
            userJson.put("estasincronizado", true);
            userJson.put("fechaultimasync", getCurrentDate());

            JSONArray jsonArray = new JSONArray();
            jsonArray.put(userJson);

            postData("usuario", jsonArray);

            int remoteId = fetchUserIdByCodigoRecuperacion(currentUser.CodigoRecuperacion);
            if (remoteId != -1) {
                SessionController.configInstance.setSyncId(remoteId);
                SessionController.usuarioActual.fechaUltimaSync = getCurrentDate();
                usuarioDAO.update(SessionController.usuarioActual);

                Log.d(TAG, "Usuario sincronizado con UUID remoto obtenido: " + remoteId);

                syncExistingUser(localDb, currentUser);
            } else {
                Log.e(TAG, "No se pudo obtener el UUID después de insertar el usuario.");
            }
        }



        private void syncExistingUser(SQLiteDatabase localDb, Usuario currentUser) throws Exception {
            int syncId = SessionController.configInstance.getSyncId();
            if (syncId == -1) {
                Log.e(TAG, "No se encontró el ID de sincronización para el usuario.");
                return;
            }

            JSONObject userJson = new JSONObject();
            userJson.put("uuid", syncId);
            userJson.put("codigorecuperacion", SessionController.usuarioActual.CodigoRecuperacion);
            userJson.put("esusuariopremium", SessionController.usuarioActual.esUsuarioPremium);
            userJson.put("estasincronizado", true);
            userJson.put("fechaultimasync", getCurrentDate());

            JSONArray userArray = new JSONArray();
            userArray.put(userJson);
            patchData("usuario", "uuid=eq." + syncId, userArray);

            Cursor cursor = localDb.rawQuery("SELECT * FROM CUENTA WHERE UUID = ?", new String[]{String.valueOf(SessionController.usuarioActual.UUID)});
            JSONArray cuentaArray = new JSONArray();
            while (cursor.moveToNext()) {
                JSONObject cuentaJson = new JSONObject();
                cuentaJson.put("nombreCuenta", cursor.getString(cursor.getColumnIndexOrThrow("nombreCuenta")));
                cuentaJson.put("balanceTotal", cursor.getDouble(cursor.getColumnIndexOrThrow("balanceTotal")));
                cuentaJson.put("monedaSeleccionada", cursor.getString(cursor.getColumnIndexOrThrow("monedaSeleccionada")));
                cuentaJson.put("fechaUltimoMovimiento", cursor.getString(cursor.getColumnIndexOrThrow("fechaUltimoMovimiento")));
                cuentaJson.put("UUID", syncId);  // Asignar el UUID fijo
                cuentaArray.put(cuentaJson);
            }
            cursor.close();
            postData("cuenta", cuentaArray);

            cursor = localDb.rawQuery(
                    "SELECT M.* FROM MOVIMIENTOS M INNER JOIN CUENTA C ON M.idCuenta = C.idCuenta WHERE C.UUID = ?",
                    new String[]{String.valueOf(SessionController.usuarioActual.UUID)}
            );
            JSONArray movimientosArray = new JSONArray();
            while (cursor.moveToNext()) {
                JSONObject movimientoJson = new JSONObject();
                movimientoJson.put("nombre", cursor.getString(cursor.getColumnIndexOrThrow("nombre")));
                movimientoJson.put("fechaMovimiento", cursor.getString(cursor.getColumnIndexOrThrow("fechaMovimiento")));
                movimientoJson.put("cantidadMovida", cursor.getDouble(cursor.getColumnIndexOrThrow("cantidadMovida")));
                movimientoJson.put("esUnGasto", cursor.getInt(cursor.getColumnIndexOrThrow("esUnGasto")) > 0);
                movimientoJson.put("idCuenta", cursor.getInt(cursor.getColumnIndexOrThrow("idCuenta"))); // Esto requiere mapeo futuro
                movimientosArray.put(movimientoJson);
            }
            cursor.close();
            postData("movimientos", movimientosArray);

            SessionController.usuarioActual.fechaUltimaSync = getCurrentDate();
            usuarioDAO.update(currentUser);
            Log.d(TAG, "Sincronización completada para el usuario existente.");
        }


        private JSONObject postData(String table, JSONArray dataArray) throws Exception {
            URL url = new URL(SUPABASE_URL + table);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", SERVICE_ROLE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SERVICE_ROLE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=representation");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(dataArray.toString().getBytes());
            os.flush();
            os.close();

            if (conn.getResponseCode() == 201) {
                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                inputStream.close();

                JSONArray responseArray = new JSONArray(response.toString());
                if (responseArray.length() > 0) {
                    return responseArray.getJSONObject(0);
                } else {
                    return null;
                }
            } else {
                Log.e(TAG, "Error al insertar datos en " + table + ": " + conn.getResponseMessage());
                return null;
            }
        }

        private int fetchUserIdByCodigoRecuperacion(String codigoRecuperacion) throws Exception {
            URL url = new URL(SUPABASE_URL + "usuario?codigorecuperacion=eq." + codigoRecuperacion);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SERVICE_ROLE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SERVICE_ROLE_KEY);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                inputStream.close();

                JSONArray responseArray = new JSONArray(response.toString());
                if (responseArray.length() > 0) {
                    JSONObject userObject = responseArray.getJSONObject(0);
                    return userObject.getInt("uuid");
                }
            } else {
                Log.e(TAG, "Error al hacer GET del usuario: " + conn.getResponseMessage());
            }
            return -1;
        }


        private void patchData(String table, String filter, JSONArray dataArray) throws Exception {
            URL url = new URL(SUPABASE_URL + table + "?" + filter);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PATCH");
            conn.setRequestProperty("apikey", SERVICE_ROLE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SERVICE_ROLE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=representation");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(dataArray.toString().getBytes());
            os.flush();
            os.close();

            if (conn.getResponseCode() != 200) {
                Log.e(TAG, "Error al actualizar datos en " + table + ": " + conn.getResponseMessage());
            }
        }

        private String getCurrentDate() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            return dateFormat.format(new Date());
        }
    }
}
