package com.kirubas.alumniapp1;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "AlumniDB.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Alumni core profile
        db.execSQL("CREATE TABLE alumni (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "firstName TEXT, lastName TEXT, email TEXT UNIQUE, graduationYear INTEGER," +
                "department TEXT, phone TEXT, profilePicture BLOB)");

        // User-defined fields meta-table
        db.execSQL("CREATE TABLE userDefinedFields (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "fieldName TEXT UNIQUE, fieldType TEXT)");

        // User-defined values
        db.execSQL("CREATE TABLE userDefinedValues (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "alumniId INTEGER, fieldId INTEGER, value TEXT," +
                "FOREIGN KEY(alumniId) REFERENCES alumni(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(fieldId) REFERENCES userDefinedFields(id) ON DELETE CASCADE, " +
                "UNIQUE(alumniId, fieldId))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS userDefinedValues");
        db.execSQL("DROP TABLE IF EXISTS userDefinedFields");
        db.execSQL("DROP TABLE IF EXISTS alumni");
        onCreate(db);
    }
}
