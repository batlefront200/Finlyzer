package com.example.fynlizer.DAOClases;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.fynlizer.Database.LocalDatabaseHelper;
import com.example.fynlizer.Implementaciones.Movimiento;
import java.util.*;

public class MovimientoDAO {

    private final SQLiteDatabase db;

    public MovimientoDAO(SQLiteDatabase currentDbInstance) {
        this.db = currentDbInstance;
    }

    public long insert(Movimiento mov) {
        ContentValues values = new ContentValues();
        values.put("nombreFecha", mov.nombreFecha);
        values.put("cantidadMovida", mov.cantidadMovida);
        values.put("esUnGasto", mov.esUnGasto ? 1 : 0);
        return db.insert("MOVIMIENTOS", null, values);
    }

    public Movimiento getById(int id) {
        Cursor cursor = db.rawQuery("SELECT * FROM MOVIMIENTOS WHERE idMovimiento = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            Movimiento m = parse(cursor);
            cursor.close();
            return m;
        }
        cursor.close();
        return null;
    }

    public List<Movimiento> getAll() {
        List<Movimiento> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM MOVIMIENTOS", null);
        while (cursor.moveToNext()) {
            list.add(parse(cursor));
        }
        cursor.close();
        return list;
    }

    public int update(Movimiento mov) {
        ContentValues values = new ContentValues();
        values.put("nombreFecha", mov.nombreFecha);
        values.put("cantidadMovida", mov.cantidadMovida);
        values.put("esUnGasto", mov.esUnGasto ? 1 : 0);
        return db.update("MOVIMIENTOS", values, "idMovimiento = ?", new String[]{String.valueOf(mov.idMovimiento)});
    }

    public int delete(int id) {
        return db.delete("MOVIMIENTOS", "idMovimiento = ?", new String[]{String.valueOf(id)});
    }

    private Movimiento parse(Cursor c) {
        Movimiento m = new Movimiento();
        m.idMovimiento = c.getInt(c.getColumnIndexOrThrow("idMovimiento"));
        m.nombreFecha = c.getString(c.getColumnIndexOrThrow("nombreFecha"));
        m.cantidadMovida = c.getDouble(c.getColumnIndexOrThrow("cantidadMovida"));
        m.esUnGasto = c.getInt(c.getColumnIndexOrThrow("esUnGasto")) == 1;
        return m;
    }
}
