package com.example.fynlizer.DAOClases;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.fynlizer.Implementaciones.Movimiento;
import java.util.*;

public class MovimientoDAO {

    private final SQLiteDatabase db;

    public MovimientoDAO(SQLiteDatabase currentDbInstance) {
        this.db = currentDbInstance;
    }

    public long insert(Movimiento mov) {
        ContentValues values = new ContentValues();
        values.put("nombre", mov.nombre);
        values.put("fechaMovimiento", mov.fechaMovimiento);
        values.put("cantidadMovida", mov.cantidadMovida);
        values.put("esUnGasto", mov.esUnGasto ? 1 : 0);
        values.put("idCuenta", mov.idCuenta);
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
        values.put("nombre", mov.nombre);
        values.put("fechaMovimiento", mov.fechaMovimiento);
        values.put("cantidadMovida", mov.cantidadMovida);
        values.put("esUnGasto", mov.esUnGasto ? 1 : 0);
        values.put("idCuenta", mov.idCuenta);
        return db.update("MOVIMIENTOS", values, "idMovimiento = ?", new String[]{String.valueOf(mov.idMovimiento)});
    }

    public int delete(int id) {
        return db.delete("MOVIMIENTOS", "idMovimiento = ?", new String[]{String.valueOf(id)});
    }

    public List<Movimiento> getGastosByCuenta(int idCuenta) {
        List<Movimiento> listaGastos = new ArrayList<>();
        String query = "SELECT * FROM MOVIMIENTOS WHERE idCuenta = ? AND esUnGasto = 1";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCuenta)});
        while (cursor.moveToNext()) {
            listaGastos.add(parse(cursor));
        }
        cursor.close();
        return listaGastos;
    }

    public List<Movimiento> getMovimientosByCuentaEnMesActual(int idCuenta) {
        List<Movimiento> listaGastos = new ArrayList<>();

        // PRIMER DÍA DEL MES ACTUAL
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        String primerDiaMes = String.format(Locale.US, "%04d-%02d-01 00:00:00",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));

        Calendar hoy = Calendar.getInstance();
        String fechaHoy = String.format(Locale.US, "%04d-%02d-%02d 23:59:59",
                hoy.get(Calendar.YEAR),
                hoy.get(Calendar.MONTH) + 1,
                hoy.get(Calendar.DAY_OF_MONTH));

        String query = "SELECT * FROM MOVIMIENTOS WHERE idCuenta = ? AND fechaMovimiento BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCuenta), primerDiaMes, fechaHoy});

        while (cursor.moveToNext()) {
            listaGastos.add(parse(cursor));
        }
        cursor.close();

        return listaGastos;
    }

    public List<Movimiento> getOnlyIngresosByCuentaEnMesActual(int idCuenta) {
        List<Movimiento> listaGastos = new ArrayList<>();

        // PRIMER DÍA DEL MES ACTUAL
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        String primerDiaMes = String.format(Locale.US, "%04d-%02d-01 00:00:00",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));

        Calendar hoy = Calendar.getInstance();
        String fechaHoy = String.format(Locale.US, "%04d-%02d-%02d 23:59:59",
                hoy.get(Calendar.YEAR),
                hoy.get(Calendar.MONTH) + 1,
                hoy.get(Calendar.DAY_OF_MONTH));

        String query = "SELECT * FROM MOVIMIENTOS WHERE idCuenta = ? AND esUnGasto = 0 AND fechaMovimiento BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCuenta), primerDiaMes, fechaHoy});

        while (cursor.moveToNext()) {
            listaGastos.add(parse(cursor));
        }
        cursor.close();

        return listaGastos;
    }

    public Movimiento getUltimoMovimiento() {
        String query = "SELECT * FROM MOVIMIENTOS ORDER BY fechaMovimiento DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            Movimiento ultimoMovimiento = parse(cursor);
            cursor.close();
            return ultimoMovimiento;
        }
        cursor.close();
        return null;
    }

    public Movimiento getUltimoMovimientoById(int idCuenta) {
        String query = "SELECT * FROM MOVIMIENTOS WHERE idCuenta = ? ORDER BY fechaMovimiento DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCuenta)});
        if (cursor.moveToFirst()) {
            Movimiento ultimoMovimiento = parse(cursor);
            cursor.close();
            return ultimoMovimiento;
        }
        cursor.close();
        return null;
    }


    public List<Movimiento> getAllOrderedByDateDescForAccount(int idCuenta) {
        List<Movimiento> list = new ArrayList<>();
        String query = "SELECT * FROM MOVIMIENTOS WHERE idCuenta = ? ORDER BY fechaMovimiento DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCuenta)});
        while (cursor.moveToNext()) {
            list.add(parse(cursor));
        }
        cursor.close();
        return list;
    }

    public List<String> getAllMovementNames(int idCuenta) {
        List<String> nombres = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT DISTINCT nombre FROM MOVIMIENTOS WHERE idCuenta = ?", new String[]{String.valueOf(idCuenta)});
        if (cursor.moveToFirst()) {
            do {
                nombres.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return nombres;
    }

    public List<Movimiento> getMovimientosByCuentaEnAnoActual(int idCuenta) {
        List<Movimiento> listaMovimientos = new ArrayList<>();

        // PRIMER DÍA DEL AÑO ACTUAL
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        String primerDiaAno = String.format(Locale.US, "%04d-01-01 00:00:00", year);
        String ultimoDiaAno = String.format(Locale.US, "%04d-12-31 23:59:59", year);

        String query = "SELECT * FROM MOVIMIENTOS WHERE idCuenta = ? AND fechaMovimiento BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(idCuenta), primerDiaAno, ultimoDiaAno});

        while (cursor.moveToNext()) {
            listaMovimientos.add(parse(cursor));
        }
        cursor.close();

        return listaMovimientos;
    }


    private Movimiento parse(Cursor c) {
        Movimiento m = new Movimiento();
        m.idMovimiento = c.getInt(c.getColumnIndexOrThrow("idMovimiento"));
        m.nombre = c.getString(c.getColumnIndexOrThrow("nombre"));
        m.fechaMovimiento = c.getString(c.getColumnIndexOrThrow("fechaMovimiento"));
        m.cantidadMovida = c.getDouble(c.getColumnIndexOrThrow("cantidadMovida"));
        m.esUnGasto = c.getInt(c.getColumnIndexOrThrow("esUnGasto")) == 1;
        m.idCuenta = c.getInt(c.getColumnIndexOrThrow("idCuenta"));
        return m;
    }
}
