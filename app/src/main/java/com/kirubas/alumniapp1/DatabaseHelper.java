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

        // Institution Master Table
        db.execSQL("CREATE TABLE institution (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "address TEXT, " +
                "logo BLOB, " +
                "authToken TEXT UNIQUE" +
                ")");


        // Institute Admin Table
        db.execSQL("CREATE TABLE adminUser (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL" +
                ")");

        // Institute & admin mapping - multiple admin for institutes
        db.execSQL("CREATE TABLE adminInstitution (" +
                "adminId INTEGER NOT NULL, " +
                "institutionId INTEGER NOT NULL, " +
                "PRIMARY KEY(adminId, institutionId), " +
                "FOREIGN KEY(adminId) REFERENCES adminUser(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(institutionId) REFERENCES institution(id) ON DELETE CASCADE" +
                ")");

        // Alumni core profile
        db.execSQL("CREATE TABLE alumni (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "firstName TEXT, " +
                "lastName TEXT, " +
                "email TEXT UNIQUE, " +
                "graduationYear INTEGER, " +
                "department TEXT, " +
                "phone TEXT, " +
                "profilePicture BLOB, " +
                "institutionId INTEGER, " +
                "FOREIGN KEY(institutionId) REFERENCES institution(id) ON DELETE SET NULL" +
                ")");

        // User-defined fields meta-table
        db.execSQL("CREATE TABLE userDefinedFields (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "fieldName TEXT UNIQUE, " +
                "fieldType TEXT, " +
                "institutionId INTEGER, " +
                "FOREIGN KEY(institutionId) REFERENCES institution(id) ON DELETE CASCADE" +
                ")");

// User-defined values
        db.execSQL("CREATE TABLE userDefinedValues (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "alumniId INTEGER, " +
                "fieldId INTEGER, " +
                "institutionId INTEGER, " +
                "value TEXT, " +
                "FOREIGN KEY(alumniId) REFERENCES alumni(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(fieldId) REFERENCES userDefinedFields(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(institutionId) REFERENCES institution(id) ON DELETE CASCADE, " +
                "UNIQUE(alumniId, fieldId, institutionId))");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS userDefinedValues");
        db.execSQL("DROP TABLE IF EXISTS userDefinedFields");
        db.execSQL("DROP TABLE IF EXISTS alumni");
        db.execSQL("DROP TABLE IF EXISTS adminUser");
        db.execSQL("DROP TABLE IF EXISTS institution");
        db.execSQL("DROP TABLE IF EXISTS adminInstitution");
        onCreate(db);
    }


}
