package com.example.fynlizer.Session;

import android.database.sqlite.SQLiteDatabase;

import com.example.fynlizer.DAOClases.CuentaDAO;
import com.example.fynlizer.Database.ConfigHandler;
import com.example.fynlizer.Implementaciones.Cuenta;
import com.example.fynlizer.Implementaciones.Usuario;

public class SessionController {
    public static Usuario usuarioActual;
    public static Cuenta cuentaActual;
    public static SQLiteDatabase dbInstance;
    public static ConfigHandler configInstance;
    public static boolean guardarCuentaActual() {
        CuentaDAO cuentaDao = new CuentaDAO(SessionController.dbInstance);
        int rows_affected = cuentaDao.update(cuentaActual);
        return rows_affected > 0; // Devuelve true si se ha afectado minimo una row
    }
}
