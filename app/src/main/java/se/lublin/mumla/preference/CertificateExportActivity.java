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

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import se.lublin.mumla.R;
import se.lublin.mumla.db.DatabaseCertificate;
import se.lublin.mumla.db.MumlaDatabase;
import se.lublin.mumla.db.MumlaSQLiteDatabase;

/**
 * Created by andrew on 12/01/16.
 */
public class CertificateExportActivity extends AppCompatActivity implements DialogInterface.OnClickListener {
    private static final String TAG = CertificateExportActivity.class.getName();

    /**
     * The name of the directory to export to on external storage.
     */
    private static final String EXTERNAL_STORAGE_DIR = "Mumla";

    private MumlaDatabase mDatabase;
    private List<DatabaseCertificate> mCertificates;

    private final ActivityResultLauncher<String> documentCreator =
            registerForActivityResult(new CreateDocument(), this::onDocumentCreated);
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private DatabaseCertificate mCertificatePending = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = new MumlaSQLiteDatabase(this);
        mCertificates = mDatabase.getCertificates();

        CharSequence[] labels = new CharSequence[mCertificates.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = mCertificates.get(i).getName();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pref_export_certificate_title)
                .setItems(labels, this)
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        DatabaseCertificate certificate = mCertificates.get(which);
        if (SDK_INT >= Build.VERSION_CODES.R) {
            // TODO Should always use this method?
            mCertificatePending = certificate;
            documentCreator.launch(certificate.getName());
        } else {
            saveCertificateClassic(certificate);
        }
    }

    private void onDocumentCreated(Uri uri) {
        if (uri != null && mCertificatePending != null) {
            try {
                OutputStream os = getContentResolver().openOutputStream(uri);
                DocumentFile df = DocumentFile.fromSingleUri(this, uri);
                writeCertificate(os, mCertificatePending, df != null ? df.getName() : "<unknown>");
            } catch (FileNotFoundException e) {
                showErrorDialog(R.string.externalStorageUnavailable);
                Log.w(TAG, "FileNotFound on output file picked by user?!");
            }
        } else if (mCertificatePending == null) {
            Log.w(TAG, "No pending certificate after user picked output file");
        }
        finish();
    }

    private void saveCertificateClassic(DatabaseCertificate certificate) {
        if (ContextCompat.checkSelfPermission(CertificateExportActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CertificateExportActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            mCertificatePending = certificate;
            return;
        }

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
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            showErrorDialog(R.string.externalStorageUnavailable);
            return;
        }
        writeCertificate(fos, certificate, outputFile.getAbsolutePath());
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mCertificatePending != null) {
                    saveCertificateClassic(mCertificatePending);
                } else {
                    Log.w(TAG, "No pending certificate after permission was granted");
                }
            } else {
                Toast.makeText(CertificateExportActivity.this, getString(R.string.grant_perm_storage),
                        Toast.LENGTH_LONG).show();
            }
            mCertificatePending = null;
        }
    }

    private void writeCertificate(OutputStream fos, DatabaseCertificate cert, String path) {
        byte[] data = mDatabase.getCertificateData(cert.getId());
        try {
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(data);
            bos.close();
            Toast.makeText(this, getString(R.string.export_success, path), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog(R.string.error_writing_to_storage);
        }
    }

    private void showErrorDialog(int resourceId) {
        new MaterialAlertDialogBuilder(this)
                .setMessage(resourceId)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
