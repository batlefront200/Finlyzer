package com.example.fynlizer.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LocalDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "LOCAL_DB.db";
    private static final int DB_VERSION = 1;

    private static LocalDatabaseHelper instance;
    private final Context context;

    private LocalDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
        copyDatabaseIfNeeded();
    }

    public static synchronized LocalDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LocalDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private void copyDatabaseIfNeeded() {
        File dbFile = context.getDatabasePath(DB_NAME);
        if (!dbFile.exists()) {
            dbFile.getParentFile().mkdirs();
            SQLiteDatabase db = getReadableDatabase();
            db.close();

            try (InputStream is = context.getAssets().open(DB_NAME);
                 OutputStream os = new FileOutputStream(dbFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }

            } catch (IOException e) {
                SQLiteDatabase writeDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
                executeCreationScript(writeDb);
                writeDb.close();
            }
        }
    }

    private void executeCreationScript(SQLiteDatabase db) {
        try (InputStream is = context.getAssets().open("creationQuery.sql");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append("\n");
            }

            for (String stmt : sql.toString().split(";")) {
                if (!stmt.trim().isEmpty()) {
                    db.execSQL(stmt.trim() + ";");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error ejecutando creationQuery.sql", e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) { /* Lo pongo porque es obligatorio el override */ }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { /* Lo pongo porque es obligatorio el override */ }

    public SQLiteDatabase getDatabase() {
        return getWritableDatabase();
    }
}

