package com.kirubas.alumniapp1;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
                "email TEXT, " +
                "logo BLOB, " +
                "accountManagerUsername TEXT UNIQUE, " +
                "accountManagerPassword TEXT" +
                ")");

        // Institute Admin Table
        db.execSQL("CREATE TABLE adminUser (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "name TEXT, " +                  // admin's display or full name
                "profilePicture BLOB" +          // profile image as BLOB
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
                "username TEXT UNIQUE, " +                   // login username
                "password TEXT, " +                          // password (hash in production!)
                "firstName TEXT, " +
                "lastName TEXT, " +
                "gender TEXT, " +
                "dateOfBirth TEXT, " +                       // ISO string format recommended
                "email TEXT UNIQUE, " +
                "phone TEXT, " +
                "profilePicture BLOB" +
                ")");




        //alumni institution mapping
        db.execSQL("CREATE TABLE alumniInstitution (" +
                "alumniId INTEGER NOT NULL, " +
                "institutionId INTEGER NOT NULL, " +
                "department TEXT, " +
                "fromYear INTEGER, " +        // Start year of association with institution
                "toYear INTEGER, " +          // End year of association (or NULL if ongoing)
                "graduationYear INTEGER, " +  // Actual graduation year
                "approved INTEGER DEFAULT 0, " +  // 0 = pending, 1 = approved, etc.
                "joinDate TEXT, " +           // Optional date string when joined/requested
                "PRIMARY KEY(alumniId, institutionId), " +
                "FOREIGN KEY(alumniId) REFERENCES alumni(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(institutionId) REFERENCES institution(id) ON DELETE CASCADE" +
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


        db.execSQL("CREATE TABLE institutionEvents (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "institutionId INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "eventDate TEXT, " +            // Store date as ISO string (e.g. YYYY-MM-DD) or datetime
                "venue TEXT, " +                // Optional venue/location of event
                "createdAt TEXT DEFAULT (datetime('now')), " +  // Timestamp when event was created
                "FOREIGN KEY(institutionId) REFERENCES institution(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE eventParticipation (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "eventId INTEGER NOT NULL, " +
                "alumniId INTEGER NOT NULL, " +
                "registrationDate TEXT DEFAULT (datetime('now')), " +
                "status TEXT DEFAULT 'may_be', " +   // 'may_be', 'attending', 'not_attending', 'cancelled', 'attended'
                "FOREIGN KEY(eventId) REFERENCES institutionEvents(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(alumniId) REFERENCES alumni(id) ON DELETE CASCADE, " +
                "UNIQUE(eventId, alumniId) " +
                ")");



        //institute polling
        db.execSQL("CREATE TABLE institutionPolls (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "institutionId INTEGER NOT NULL, " +
                "creatorType TEXT NOT NULL, " +            //'adminUser' or 'IAAM'
                "creatorId INTEGER NOT NULL, " +            // refers to id in adminUser or institution
                "question TEXT NOT NULL, " +
                "status TEXT DEFAULT 'new', " +
                "createdAt TEXT DEFAULT (datetime('now')), " +
                "FOREIGN KEY(institutionId) REFERENCES institution(id) ON DELETE CASCADE" +
                ")");

        // Poll options table
        db.execSQL("CREATE TABLE pollOptions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pollId INTEGER NOT NULL, " +
                "optionText TEXT NOT NULL, " +
                "votes INTEGER DEFAULT 0, " +
                "FOREIGN KEY(pollId) REFERENCES institutionPolls(id) ON DELETE CASCADE" +
                ")");

        // poll answers
        db.execSQL("CREATE TABLE pollAnswers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pollId INTEGER NOT NULL, " +          // References institutionPolls.id
                "pollOptionId INTEGER NOT NULL, " +    // References pollOptions.id
                "alumniId INTEGER NOT NULL, " +        // References alumni.id
                "FOREIGN KEY(pollId) REFERENCES institutionPolls(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(pollOptionId) REFERENCES pollOptions(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(alumniId) REFERENCES alumni(id) ON DELETE CASCADE, " +
                "UNIQUE(pollId, alumniId)" +           // Prevent multiple answers per poll per user
                ")");


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("DROP TABLE IF EXISTS userDefinedValues");
        //db.execSQL("DROP TABLE IF EXISTS userDefinedFields");
        //db.execSQL("DROP TABLE IF EXISTS alumni");
        //db.execSQL("DROP TABLE IF EXISTS alumniInstitution");
        //db.execSQL("DROP TABLE IF EXISTS adminUser");
        //db.execSQL("DROP TABLE IF EXISTS institution");
        //db.execSQL("DROP TABLE IF EXISTS adminInstitution");
        onCreate(db);
    }
    // Call this with the URI of the backup file that the user selected (e.g. from file picker)

    //how to call the restore method
    // Suppose you get backupUri from the file picker
    //DatabaseHelper dbHelper = new DatabaseHelper(this); // 'this' is an Activity
    //dbHelper.restoreDatabaseFromUri(this, backupUri);

    public void restoreDatabaseFromUri(Context context, Uri backupUri) {
        try {
            File dbFile = context.getDatabasePath("AlumniDB.db");
            InputStream in = context.getContentResolver().openInputStream(backupUri);
            OutputStream out = new FileOutputStream(dbFile);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
            in.close();
            out.close();

            Toast.makeText(context, "Restore completed! Please restart the app.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "Failed to restore: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }



}
