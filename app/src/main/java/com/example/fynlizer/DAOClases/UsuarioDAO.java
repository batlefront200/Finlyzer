package com.example.fynlizer.DAOClases;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.fynlizer.Database.LocalDatabaseHelper;
import com.example.fynlizer.Implementaciones.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.security.SecureRandom;

public class UsuarioDAO {

    private final SQLiteDatabase db;

    public UsuarioDAO(SQLiteDatabase currentDbInstance) {
        this.db = currentDbInstance;
    }

    public long insert(Usuario usuario) {
        ContentValues values = new ContentValues();
        values.put("CodigoRecuperacion", usuario.CodigoRecuperacion);
        values.put("esUsuarioPremium", usuario.esUsuarioPremium ? 1 : 0);
        values.put("estaSincronizado", usuario.estaSincronizado ? 1 : 0);
        values.put("fechaUltimaSync", usuario.fechaUltimaSync);
        return db.insert("USUARIO", null, values);
    }

    public Usuario getById(int uuid) {
        Cursor cursor = db.rawQuery("SELECT * FROM USUARIO WHERE UUID = ?", new String[]{String.valueOf(uuid)});
        if (cursor.moveToFirst()) {
            Usuario u = parse(cursor);
            cursor.close();
            return u;
        }
        cursor.close();
        return null;
    }

    public List<Usuario> getAll() {
        List<Usuario> list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM USUARIO", null);
        while (cursor.moveToNext()) {
            list.add(parse(cursor));
        }
        cursor.close();
        return list;
    }

    public int update(Usuario usuario) {
        ContentValues values = new ContentValues();
        values.put("CodigoRecuperacion", usuario.CodigoRecuperacion);
        values.put("esUsuarioPremium", usuario.esUsuarioPremium ? 1 : 0);
        values.put("estaSincronizado", usuario.estaSincronizado ? 1 : 0);
        values.put("fechaUltimaSync", usuario.fechaUltimaSync);
        return db.update("USUARIO", values, "UUID = ?", new String[]{String.valueOf(usuario.UUID)});
    }

    public int delete(int uuid) {
        return db.delete("USUARIO", "UUID = ?", new String[]{String.valueOf(uuid)});
    }

    private Usuario parse(Cursor c) {
        Usuario u = new Usuario();
        u.UUID = c.getInt(c.getColumnIndexOrThrow("UUID"));
        u.CodigoRecuperacion = c.getString(c.getColumnIndexOrThrow("CodigoRecuperacion"));
        u.esUsuarioPremium = c.getInt(c.getColumnIndexOrThrow("esUsuarioPremium")) == 1;
        u.estaSincronizado = c.getInt(c.getColumnIndexOrThrow("estaSincronizado")) == 1;
        u.fechaUltimaSync = c.getString(c.getColumnIndexOrThrow("fechaUltimaSync"));
        return u;
    }

    public String createRandomCode() {
        String characterPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();

        StringBuilder uuid = new StringBuilder(24);

        for (int i = 0; i < 24; i++) {
            int index = random.nextInt(characterPool.length());
            uuid.append(characterPool.charAt(index));
        }

        return uuid.toString();
    }

}

