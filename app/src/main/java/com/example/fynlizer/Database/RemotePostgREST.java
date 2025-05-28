package com.example.fynlizer.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.example.fynlizer.DAOClases.CuentaDAO;
import com.example.fynlizer.DAOClases.MovimientoDAO;
import com.example.fynlizer.DAOClases.UsuarioDAO;
import com.example.fynlizer.Implementaciones.Cuenta;
import com.example.fynlizer.Implementaciones.Movimiento;
import com.example.fynlizer.Implementaciones.Usuario;
import com.example.fynlizer.Session.SessionController;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

        private void syncMovimientos(List<Movimiento> movimientosLocales, JSONArray movimientosRemotos, int idCuenta) {
            try {
                List<JSONObject> movimientosRemotosList = new ArrayList<>();
                for (int i = 0; i < movimientosRemotos.length(); i++) {
                    movimientosRemotosList.add(movimientosRemotos.getJSONObject(i));
                }

                for (Movimiento movLocal : movimientosLocales) {
                    boolean encontrado = false;

                    for (JSONObject movRemoto : movimientosRemotosList) {
                        if (
                                movLocal.nombre.equals(movRemoto.optString("nombre")) &&
                                        safeEqualsDate(movLocal.fechaMovimiento, movRemoto.optString("fechamovimiento")) &&
                                        movLocal.cantidadMovida == movRemoto.optDouble("cantidadmovida") &&
                                        movLocal.esUnGasto == movRemoto.optBoolean("esungasto")
                        ) {
                            encontrado = true;
                            break;
                        }
                    }

                    if (!encontrado) {
                        JSONObject nuevoMovimiento = new JSONObject();
                        nuevoMovimiento.put("nombre", movLocal.nombre);
                        nuevoMovimiento.put("fechamovimiento", movLocal.fechaMovimiento);
                        nuevoMovimiento.put("cantidadmovida", movLocal.cantidadMovida);
                        nuevoMovimiento.put("esungasto", movLocal.esUnGasto);
                        nuevoMovimiento.put("idcuenta", SessionController.configInstance.getIdRemotoCuenta(movLocal.idCuenta));

                        JSONArray arr = new JSONArray();
                        arr.put(nuevoMovimiento);
                        JSONObject insertado = postData("movimientos", arr);
                        int idMovimiento = insertado.getInt("idmovimiento");
                        SessionController.configInstance.saveMovimientoRemoto(movLocal.idMovimiento, idMovimiento);
                        Log.d(TAG, "Insertado movimiento en remoto: " + movLocal.nombre);
                    }
                }

                for (JSONObject movRemoto : movimientosRemotosList) {
                    boolean encontrado = false;

                    for (Movimiento movLocal : movimientosLocales) {
                        if (
                                movLocal.nombre.equals(movRemoto.optString("nombre")) &&
                                        safeEqualsDate(movLocal.fechaMovimiento, movRemoto.optString("fechamovimiento")) &&
                                        movLocal.cantidadMovida == movRemoto.optDouble("cantidadmovida") &&
                                        movLocal.esUnGasto == movRemoto.optBoolean("esungasto")
                        ) {
                            encontrado = true;
                            break;
                        }
                    }

                    if (!encontrado) {
                        int idMovimientoRemoto = movRemoto.getInt("idmovimiento");
                        deleteData("movimientos", "idmovimiento=eq." + idMovimientoRemoto);
                        Log.d(TAG, "Eliminado movimiento remoto: ID " + idMovimientoRemoto);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error en syncMovimientos", e);
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
            userJson.put("codigorecuperacion", currentUser.CodigoRecuperacion);
            userJson.put("esusuariopremium", currentUser.esUsuarioPremium);
            userJson.put("estasincronizado", true);
            userJson.put("fechaultimasync", getCurrentDate());

            JSONArray userArray = new JSONArray();
            userArray.put(userJson);
            patchData("usuario", "uuid=eq." + syncId, userArray);

            CuentaDAO cuentaDao = new CuentaDAO(localDb);
            MovimientoDAO movimientoDao = new MovimientoDAO(localDb);

            List<Cuenta> cuentasLocales = cuentaDao.getAll();
            JSONArray cuentasRemotas = getData("cuenta", "uuid=eq." + syncId);  // <- Aquí va tu línea

            syncCuentas(cuentasLocales, cuentasRemotas, syncId);

            for (Cuenta cuentaLocal : cuentasLocales) {
                JSONArray movimientosRemotos = getData("movimientos", "idcuenta=eq." + SessionController.configInstance.getIdRemotoCuenta(cuentaLocal.idCuenta));
                List<Movimiento> movimientosLocales = movimientoDao.getAllOrderedByDateDescForAccount(cuentaLocal.idCuenta);
                syncMovimientos(movimientosLocales, movimientosRemotos, cuentaLocal.idCuenta);
            }

            SessionController.usuarioActual.fechaUltimaSync = getCurrentDate();
            UsuarioDAO usuarioDao = new UsuarioDAO(localDb);
            usuarioDao.update(currentUser);
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

        private boolean safeEqualsDate(String fecha1, String fecha2) { // Intento parsear con o sin hora
            if (fecha1 == null || fecha2 == null) return false;

            String[] patterns = {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd"
            };

            for (String pattern : patterns) {
                try {
                    SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                    Date date1 = format.parse(fecha1);
                    Date date2 = format.parse(fecha2);
                    if (date1 != null && date2 != null && date1.equals(date2)) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }

            Log.e("safeEqualsDate", "No se pudo parsear una o ambas fechas: " + fecha1 + " vs " + fecha2);
            return false;
        }



        private JSONArray getData(String table, String filter) throws Exception { // Esta obtiene datos de las tablas para ver lo que está sincronizado y lo que no
            URL url = new URL(SUPABASE_URL + table + "?" + filter);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SERVICE_ROLE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SERVICE_ROLE_KEY);
            conn.setRequestProperty("Accept", "application/json");

            InputStream inputStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            inputStream.close();

            return new JSONArray(response.toString());
        }

        private void deleteData(String table, String filter) throws Exception {
            URL url = new URL(SUPABASE_URL + table + "?" + filter);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("apikey", SERVICE_ROLE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SERVICE_ROLE_KEY);
            conn.setRequestProperty("Prefer", "return=minimal");

            int responseCode = conn.getResponseCode();
            if (responseCode != 204) {
                Log.e(TAG, "Error al eliminar de " + table + ": " + conn.getResponseMessage());
            }
        }

        private void syncCuentas(List<Cuenta> localCuentas, JSONArray remoteCuentas, int syncId) throws Exception {
            for (Cuenta local : localCuentas) {
                boolean found = false;
                for (int i = 0; i < remoteCuentas.length(); i++) {
                    JSONObject remote = remoteCuentas.getJSONObject(i);
                    if (SessionController.configInstance.getIdRemotoCuenta(local.idCuenta) != null) {
                        found = true;

                        boolean needsUpdate =
                                remote.optDouble("balancetotal", -1) != local.balanceTotal ||
                                        !remote.optString("monedaseleccionada", "").equals(local.monedaSeleccionada) ||
                                        !remote.optString("fechaultimomovimiento", "").equals(local.fechaUltimoMovimiento);

                        if (needsUpdate) {
                            JSONObject updated = new JSONObject();
                            updated.put("balancetotal", local.balanceTotal);
                            updated.put("monedaseleccionada", local.monedaSeleccionada);
                            updated.put("fechaultimomovimiento", local.fechaUltimoMovimiento);

                            patchData("cuenta", "idcuenta=eq." + remote.getInt("idcuenta"), new JSONArray().put(updated));
                            Log.d(TAG, "Cuenta actualizada: " + local.nombreCuenta);
                        }
                        break;
                    }
                }

                if (!found) {
                    JSONObject nuevaCuenta = new JSONObject();
                    nuevaCuenta.put("nombrecuenta", local.nombreCuenta);
                    nuevaCuenta.put("balancetotal", local.balanceTotal);
                    nuevaCuenta.put("monedaseleccionada", local.monedaSeleccionada);
                    nuevaCuenta.put("fechaultimomovimiento", local.fechaUltimoMovimiento);
                    nuevaCuenta.put("uuid", syncId);

                    JSONObject insert = postData("cuenta", new JSONArray().put(nuevaCuenta));
                    int idRemotoCuenta = insert.getInt("idcuenta");
                    SessionController.configInstance.saveCuentaRemota(local.idCuenta, idRemotoCuenta);

                    Log.d(TAG, "Cuenta agregada: " + local.nombreCuenta);
                }
            }

            // ELIMINACIÓN DE: cuentas que están en remoto pero no en local
            for (int i = 0; i < remoteCuentas.length(); i++) {
                JSONObject remote = remoteCuentas.getJSONObject(i);
                int remoteIdCuenta = remote.getInt("idcuenta");

                boolean existsLocally = false;
                for (Cuenta local : localCuentas) {
                    Integer localMappedRemoteId = SessionController.configInstance.getIdRemotoCuenta(local.idCuenta);
                    if (localMappedRemoteId != null && localMappedRemoteId == remoteIdCuenta) {
                        existsLocally = true;
                        break;
                    }
                }

                if (!existsLocally) {
                    deleteData("cuenta", "idcuenta=eq." + remoteIdCuenta);
                    Log.d(TAG, "Cuenta eliminada: " + remote.getString("nombrecuenta"));
                }
            }

        }


    }
}
