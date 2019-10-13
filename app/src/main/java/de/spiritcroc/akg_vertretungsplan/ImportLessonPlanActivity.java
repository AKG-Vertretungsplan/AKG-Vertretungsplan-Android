package de.spiritcroc.akg_vertretungsplan;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ImportLessonPlanActivity extends Activity {

    private static final int READ_STORAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        boolean isStoragePermissionRequired = "file".equals(getIntent().getScheme());

        if (isStoragePermissionRequired && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_STORAGE_REQUEST);
        } else {
            promptImport();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case READ_STORAGE_REQUEST:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    promptImport();
                } else {
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    private void promptImport() {
        new AlertDialog.Builder(this)
                .setTitle(getTitle())
                .setMessage(R.string.import_lesson_plan_summary)
                .setPositiveButton(R.string.import_lesson_plan_action_import,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (tryImportPlan(true)) {
                                    startActivity(new Intent(
                                            ImportLessonPlanActivity.this, LessonPlanActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                }
                                finish();
                            }
                })
                .setNeutralButton(R.string.import_lesson_plan_action_view,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (tryImportPlan(false)) {
                                    startActivity(new Intent(
                                            ImportLessonPlanActivity.this, LessonPlanActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                }
                                finish();
                            }
                        })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .show();
    }

    private boolean tryImportPlan(boolean persist) {
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        try {
            inputStream = getContentResolver().openInputStream(getIntent().getData());
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line).append('\n');
            }
            if (LessonPlan.getInstance(PreferenceManager.getDefaultSharedPreferences(this))
                    .importContent(this, result.toString(), persist)) {
                Log.d("ImportLessonPlan", "importing plan successful");
                Toast.makeText(getApplicationContext(),
                        R.string.import_lesson_plan_success, Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Log.w("ImportLessonPlan", "importing plan failed, broken file?");
                Toast.makeText(getApplicationContext(),
                        R.string.import_lesson_plan_invalid, Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    R.string.import_lesson_plan_failure, Toast.LENGTH_LONG).show();
            return false;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
