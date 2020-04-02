/*
 * Copyright (C) 2016 Andrew Comminos <andrew@comminos.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.lublin.mumla.preference;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import se.lublin.mumla.Constants;
import se.lublin.mumla.R;
import se.lublin.mumla.db.DatabaseCertificate;
import se.lublin.mumla.db.MumlaDatabase;
import se.lublin.mumla.db.MumlaSQLiteDatabase;

/**
 * Created by andrew on 12/01/16.
 */
public class CertificateExportActivity extends AppCompatActivity implements DialogInterface.OnClickListener {
    /**
     * The name of the directory to export to on external storage.
     */
    private static final String EXTERNAL_STORAGE_DIR = "Mumla";

    private MumlaDatabase mDatabase;
    private List<DatabaseCertificate> mCertificates;

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private DatabaseCertificate mCertificatePendingPerm = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = new MumlaSQLiteDatabase(this);
        mCertificates = mDatabase.getCertificates();

        CharSequence[] labels = new CharSequence[mCertificates.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = mCertificates.get(i).getName();
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.pref_export_certificate_title);
        adb.setItems(labels, this);
        adb.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        adb.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        DatabaseCertificate certificate = mCertificates.get(which);
        saveCertificate(certificate);
    }

    private void saveCertificate(DatabaseCertificate certificate) {
        if (ContextCompat.checkSelfPermission(CertificateExportActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CertificateExportActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            mCertificatePendingPerm = certificate;
            return;
        }

        byte[] data = mDatabase.getCertificateData(certificate.getId());
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            showErrorDialog(R.string.externalStorageUnavailable);
            return;
        }

        File storageDirectory = Environment.getExternalStorageDirectory();
        File mumlaDirectory = new File(storageDirectory, EXTERNAL_STORAGE_DIR);
        if (!mumlaDirectory.exists() && !mumlaDirectory.mkdir()) {
            showErrorDialog(R.string.externalStorageUnavailable);
            return;
        }
        File outputFile = new File(mumlaDirectory, certificate.getName());
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(data);
            bos.close();

            Toast.makeText(this, getString(R.string.export_success, outputFile.getAbsolutePath()), Toast.LENGTH_LONG).show();
            finish();
        } catch (FileNotFoundException e) {
            showErrorDialog(R.string.externalStorageUnavailable);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog(R.string.error_writing_to_storage);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mCertificatePendingPerm != null) {
                    saveCertificate(mCertificatePendingPerm);
                } else {
                    Log.w(Constants.TAG, "No pending certificate after permission was granted");
                }
            } else {
                Toast.makeText(CertificateExportActivity.this, getString(R.string.grant_perm_storage),
                        Toast.LENGTH_LONG).show();
            }
            mCertificatePendingPerm = null;
        }
    }

    private void showErrorDialog(int resourceId) {
        AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
        errorDialog.setMessage(resourceId);
        errorDialog.setPositiveButton(android.R.string.ok, null);
        errorDialog.show();
    }
}
