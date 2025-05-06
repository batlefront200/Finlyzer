package com.example.fynlizer.DAOClases;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.fynlizer.Database.LocalDatabaseHelper;
import com.example.fynlizer.Implementaciones.Cuenta;
import java.util.*;

public class CuentaDAO {

    private final SQLiteDatabase db;

    public CuentaDAO(SQLiteDatabase currentDbInstance) {
        this.db = currentDbInstance;
    }

    public long insert(Cuenta cuenta) {
        ContentValues values = new ContentValues();
        values.put("nombreCuenta", cuenta.nombreCuenta);
        values.put("balanceTotal", cuenta.balanceTotal);
        values.put("monedaSeleccionada", cuenta.monedaSeleccionada);
        values.put("fechaUltimoMovimiento", cuenta.fechaUltimoMovimiento);
        values.put("UUID", cuenta.UUID);
        return db.insert("CUENTA", null, values);
    }

    public Cuenta getById(int idCuenta) {
        Cursor cursor = db.rawQuery("SELECT * FROM CUENTA WHERE idCuenta = ?", new String[]{String.valueOf(idCuenta)});
        if (cursor.moveToFirst()) {
            Cuenta c = parse(cursor);
            cursor.close();
            return c;
        }
        cursor.close();
        return null;
    }

    public List<Cuenta> getAll() {
        List<Cuenta> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM CUENTA", null);
        while (cursor.moveToNext()) {
            list.add(parse(cursor));
        }
        cursor.close();
        return list;
    }

    public int update(Cuenta cuenta) {
        ContentValues values = new ContentValues();
        values.put("nombreCuenta", cuenta.nombreCuenta);
        values.put("balanceTotal", cuenta.balanceTotal);
        values.put("monedaSeleccionada", cuenta.monedaSeleccionada);
        values.put("fechaUltimoMovimiento", cuenta.fechaUltimoMovimiento);
        values.put("UUID", cuenta.UUID);
        return db.update("CUENTA", values, "idCuenta = ?", new String[]{String.valueOf(cuenta.idCuenta)});
    }

    public int delete(int idCuenta) {
        return db.delete("CUENTA", "idCuenta = ?", new String[]{String.valueOf(idCuenta)});
    }

    private Cuenta parse(Cursor c) {
        Cuenta cuenta = new Cuenta();
        cuenta.idCuenta = c.getInt(c.getColumnIndexOrThrow("idCuenta"));
        cuenta.nombreCuenta = c.getString(c.getColumnIndexOrThrow("nombreCuenta"));
        cuenta.balanceTotal = c.getDouble(c.getColumnIndexOrThrow("balanceTotal"));
        cuenta.monedaSeleccionada = c.getString(c.getColumnIndexOrThrow("monedaSeleccionada"));
        cuenta.fechaUltimoMovimiento = c.getString(c.getColumnIndexOrThrow("fechaUltimoMovimiento"));
        cuenta.UUID = c.getInt(c.getColumnIndexOrThrow("UUID"));
        return cuenta;
    }
}
