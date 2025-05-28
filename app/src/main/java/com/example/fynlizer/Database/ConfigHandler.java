package com.example.fynlizer.Database;

import android.content.Context;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.*;

public class ConfigHandler {

    private static final String CONFIG_FILENAME = "config.json";
    private static ConfigHandler instance;
    private final Context context;
    private JSONObject config;

    private ConfigHandler(Context context) {
        this.context = context.getApplicationContext();
        loadConfig();
    }

    public static synchronized ConfigHandler getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigHandler(context);
        }
        return instance;
    }

    private void loadConfig() {
        File file = new File(context.getFilesDir(), CONFIG_FILENAME);

        if (!file.exists()) {
            createDefaultConfig();
            saveConfig();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder jsonStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStr.append(line);
            }
            config = new JSONObject(jsonStr.toString());
        } catch (IOException | JSONException e) {
            createDefaultConfig();
            saveConfig();
        }
    }

    private void createDefaultConfig() {
        config = new JSONObject();
        try {
            config.put("sync_enabled", true);
            config.put("autocomplete_enabled", true);
            config.put("currency", "EUR");
            config.put("syncId", "0");
            config.put("objetivomensual", "0");

            config.put("cuentas_remotas", new JSONObject()); // Mapa IDs cuentas remotas
            config.put("movimientos_remotos", new JSONObject()); // Mapa IDs movimientos remotos
        } catch (JSONException e) {
            throw new RuntimeException("Error creando configuración por defecto", e);
        }
    }

    public void saveConfig() {
        File file = new File(context.getFilesDir(), CONFIG_FILENAME);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(config.toString(4));
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Error guardando configuración", e);
        }
    }

    // Métodos Getters y Setters útiles
    public boolean isSyncEnabled() {
        return config.optBoolean("sync_enabled", false);
    }

    public void setSyncEnabled(boolean value) {
        try {
            config.put("sync_enabled", value);
            saveConfig();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setSyncId(int value) {
        try {
            config.put("syncId", value);
            saveConfig();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setObjetivoMensual(int value) {
        try {
            config.put("objetivomensual", value);
            saveConfig();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getObjetivoMensual() {
        return config.optInt("objetivomensual", 0);
    }

    public int getSyncId() {
        return config.optInt("syncId", 0);
    }


    public boolean isAutocompleteEnabled() {
        return config.optBoolean("autocomplete_enabled", false);
    }

    public void setAutocompleteEnabled(boolean value) {
        try {
            config.put("autocomplete_enabled", value);
            saveConfig();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getCurrency() {
        return config.optString("currency", "EUR");
    }

    public void setCurrency(String value) {
        try {
            config.put("currency", value);
            saveConfig();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /* MAPEO DE IDS */
    public void saveCuentaRemota(int idLocal, int idRemoto) {
        try {
            JSONObject cuentas = config.optJSONObject("cuentas_remotas");
            if (cuentas == null) {
                cuentas = new JSONObject();
                config.put("cuentas_remotas", cuentas);
            }
            cuentas.put(String.valueOf(idLocal), idRemoto);
            saveConfig();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Integer getIdRemotoCuenta(int idLocal) {
        JSONObject cuentas = config.optJSONObject("cuentas_remotas");
        if (cuentas != null) {
            return cuentas.optInt(String.valueOf(idLocal), -1) != -1 ? cuentas.optInt(String.valueOf(idLocal)) : null;
        }
        return null;
    }

    public void saveMovimientoRemoto(int idLocal, int idRemoto) {
        try {
            JSONObject movimientos = config.optJSONObject("movimientos_remotos");
            if (movimientos == null) {
                movimientos = new JSONObject();
                config.put("movimientos_remotos", movimientos);
            }
            movimientos.put(String.valueOf(idLocal), idRemoto);
            saveConfig();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Integer getIdRemotoMovimiento(int idLocal) {
        JSONObject movimientos = config.optJSONObject("movimientos_remotos");
        if (movimientos != null) {
            return movimientos.optInt(String.valueOf(idLocal), -1) != -1 ? movimientos.optInt(String.valueOf(idLocal)) : null;
        }
        return null;
    }


}
