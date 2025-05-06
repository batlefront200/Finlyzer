package com.example.fynlizer.DAOClases;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.fynlizer.Database.LocalDatabaseHelper;
import com.example.fynlizer.Implementaciones.CuentaMovimiento;
import java.util.*;

public class CuentaMovimientoDAO {

    private final SQLiteDatabase db;

    public CuentaMovimientoDAO(SQLiteDatabase currentDbInstance) {
        this.db = currentDbInstance;
    }

    public long insert(CuentaMovimiento cm) {
        ContentValues values = new ContentValues();
        values.put("idCuenta", cm.idCuenta);
        values.put("idMovimiento", cm.idMovimiento);
        return db.insert("CUENTA_MOVIMIENTO", null, values);
    }

    public List<CuentaMovimiento> getAll() {
        List<CuentaMovimiento> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM CUENTA_MOVIMIENTO", null);
        while (cursor.moveToNext()) {
            CuentaMovimiento cm = new CuentaMovimiento();
            cm.idCuenta = cursor.getInt(cursor.getColumnIndexOrThrow("idCuenta"));
            cm.idMovimiento = cursor.getInt(cursor.getColumnIndexOrThrow("idMovimiento"));
            list.add(cm);
        }
        cursor.close();
        return list;
    }

    public int delete(int idCuenta, int idMovimiento) {
        return db.delete("CUENTA_MOVIMIENTO", "idCuenta = ? AND idMovimiento = ?", new String[]{
                String.valueOf(idCuenta), String.valueOf(idMovimiento)
        });
    }
}

