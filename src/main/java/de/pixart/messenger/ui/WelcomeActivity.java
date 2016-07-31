package de.pixart.messenger.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.persistance.DatabaseBackend;

public class WelcomeActivity extends Activity {

    private static final int REQUEST_READ_EXTERNAL_STORAGE = 0XD737;
    boolean dbExist = false;
    boolean backup_existing = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        //check if there is a backed up database --
        if (hasStoragePermission(REQUEST_READ_EXTERNAL_STORAGE)) {
            dbExist = checkDatabase();
        }

        if (dbExist) {
            backup_existing = true;
        }


        final Button ImportDatabase = (Button) findViewById(R.id.import_database);
        final TextView ImportText = (TextView) findViewById(R.id.import_text);

        if (backup_existing) {
            ImportDatabase.setVisibility(View.VISIBLE);
            ImportText.setVisibility(View.VISIBLE);
        }

        ImportDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ImportDatabase();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        final Button createAccount = (Button) findViewById(R.id.create_account);
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, MagicCreateActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });
        final Button useOwnProvider = (Button) findViewById(R.id.use_existing_account);
        useOwnProvider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, EditAccountActivity.class));
            }
        });

    }

    private boolean checkDatabase() {

        SQLiteDatabase checkDB = null;
        String DB_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pix-Art Messenger/.Database/";
        String DB_NAME = "Database.bak";
        int DB_Version = DatabaseBackend.DATABASE_VERSION;
        int Backup_DB_Version = 0;

        try {
            String dbPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
            Backup_DB_Version = checkDB.getVersion();
            Log.d(Config.LOGTAG, "Backup found: " + checkDB + " Version: " + checkDB.getVersion());

        } catch (SQLiteException e) {
            //database does't exist yet.
            Log.d(Config.LOGTAG, "No backup found: " + checkDB);
        }

        if (checkDB != null) {
            checkDB.close();
        }
        if (checkDB != null && Backup_DB_Version <= DB_Version) {
            return true;
        } else {
            return false;
        }
    }

    private void ImportDatabase() throws IOException {

        // Set location for the db:
        OutputStream myOutput = new FileOutputStream(this.getDatabasePath(DatabaseBackend.DATABASE_NAME));

        // Set the folder on the SDcard
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pix-Art Messenger/.Database/");

        // Set the input file stream up:
        InputStream myInput = new FileInputStream(directory.getPath() + "/Database.bak");

        // Transfer bytes from the input file to the output file
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }
        Log.d(Config.LOGTAG,"Starting import of backup");

        // Close and clear the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

        Log.d(Config.LOGTAG, "New Features - Uninstall old version of Pix-Art Messenger");
        if (isPackageInstalled("eu.siacs.conversations")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.uninstall_app_text)
                    .setPositiveButton(R.string.uninstall, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //start the deinstallation of old version
                            if (isPackageInstalled("eu.siacs.conversations")) {
                                Uri packageURI_VR = Uri.parse("package:eu.siacs.conversations");
                                Intent uninstallIntent_VR = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI_VR);
                                if (uninstallIntent_VR.resolveActivity(getPackageManager()) != null) {
                                    startActivity(uninstallIntent_VR);
                                }
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.d(Config.LOGTAG, "New Features - Uninstall cancled");
                            restart();
                        }
                    });
            builder.create().show();
        } else {
            restart();
        }

    }

    private void restart() {
        //restart app
        Log.d(Config.LOGTAG, "Restarting " + getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName()));
        Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        System.exit(0);
    }

    private boolean isPackageInstalled(String targetPackage) {
        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(targetPackage)) return true;
        }
        return false;
    }

    public boolean hasStoragePermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

}