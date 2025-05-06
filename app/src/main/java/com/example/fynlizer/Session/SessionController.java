package com.example.fynlizer.Session;

import android.database.sqlite.SQLiteDatabase;

import com.example.fynlizer.Implementaciones.Cuenta;
import com.example.fynlizer.Implementaciones.Usuario;

public class SessionController {
    public static Usuario usuarioActual;
    public static Cuenta cuentaActual;
    public static SQLiteDatabase dbInstance;
}
