package com.huntloc.handheldcomputingequipmentcontrol;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EquipmentActivity extends AppCompatActivity {
    ListView equipmentListView = null;
    private String documentId = null, credential = "";
    private Button buttonFinish;
    private FragmentManager manager = getFragmentManager();
    private EditEquipmentDialogFragment equipmentDialog = new EditEquipmentDialogFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = findViewById(android.R.id.content);
        Intent intent = getIntent();
        String response = intent.getStringExtra(MainActivity.PERSONNEL_MESSAGE);
        String personnelName = "";
        try {
            JSONObject jsonResponse = new JSONObject(response);
            personnelName = jsonResponse.optString("PersonnelName");
            credential = jsonResponse.optString("Credential");
            documentId = jsonResponse.optString("DocumentId");
        } catch (JSONException e) {

        }
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(personnelName);
        actionBar.setSubtitle("Badge Id: " + credential);
        setContentView(R.layout.activity_equipment);
        equipmentListView = (ListView) view.findViewById(R.id.list_Equipment);
        requestEquipment();
        /*new Handler().postDelayed(new Runnable() {
            public void run() {
                listEquipments();
            }
        }, 1000);*/
        buttonFinish = (Button) view.findViewById(R.id.button_finish);
        buttonFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("buttonFinish", "buttonFinish");
                NavUtils.navigateUpFromSameTask(EquipmentActivity.this);

            }
        });

    }

    public void newEquipment(MenuItem item) {
        try {
            JSONObject newEquipment = new JSONObject();
            newEquipment.put("OwnerCredentialId", credential);
            newEquipment.put("OwnerDocumentId", documentId);
            showPopupWindow(newEquipment);
        } catch (JSONException je) {
        }
    }

    public void deleteEquipment(JSONObject equipment) {
        if (!equipment.isNull("ComputingEquipmentId")) {
            final String ComputingEquipmentId = equipment.optString("ComputingEquipmentId");
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            Log.d("ComputingEquipmentId", ComputingEquipmentId);

                            String serverURL = getResources().getString(R.string.service_url)
                                    + "/ComputingEquipmentService/Delete/" + ComputingEquipmentId;
                            new DeleteEquipmentTask().execute(serverURL);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            dialog.dismiss();
                            break;
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage("Delete this equipment?");
            builder.setPositiveButton("Yes", dialogClickListener);
            builder.setNegativeButton("No", dialogClickListener);
            builder.show();
        }
    }

    public void logEquipment(JSONObject equipment, int log) {
        String serverURL = getResources().getString(R.string.service_url)
                + "/ComputingEquipmentLogService/"
                + equipment.optString("ComputingEquipmentId")
                + "/"
                + log;
        new LogOperationTask().execute(serverURL);
    }

    public void showLogPopupWindow(JSONObject equipment) {
        FragmentManager manager = getFragmentManager();
        LogEquipmentDialogFragment equipmentDialog = new LogEquipmentDialogFragment();
        equipmentDialog.setEquipment(equipment);
        equipmentDialog.setActivity(this);
        equipmentDialog.show(manager, "equipmentDialog");
    }

    public void showPopupWindow(JSONObject equipment) {
        Log.d("showPopupWindow", equipment.toString());

        equipmentDialog.setEquipment(equipment);
        equipmentDialog.setActivity(this);
        equipmentDialog.show(manager, "equipmentDialog");
    }

    public void requestEquipment(MenuItem item) {
        requestEquipment();
    }

    private void requestEquipment() {
        String serverURL = getResources().getString(R.string.service_url)
                + "/ComputingEquipmentService/Retrieve/" + documentId + "/_";
        Log.d("URL personnel", serverURL);
        new QueryEquipmentTask().execute(serverURL);
    }

    private void showEquipment(JSONArray jsonArray) {
        equipmentListView.setAdapter(new CustomAdapter(this, jsonArray));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.equipment_main_menu, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d("onActivityResult", "EquipmentActivity");

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private class QueryEquipmentTask extends AsyncTask<String, String, String> {
        HttpURLConnection urlConnection;

        @SuppressWarnings("unchecked")
        protected String doInBackground(String... args) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(args[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            } catch (Exception e) {
                Log.d("Exception", e.getMessage());
            } finally {
                urlConnection.disconnect();
            }
            return result.toString();
        }

        protected void onPostExecute(String result) {
            Log.d("Result", result);
            try {
                if (result != null && !result.equals("")) {
                    JSONArray jsonResponse = new JSONArray(result);
                    if (jsonResponse.length() > 0) {
                        EquipmentActivity.this.showEquipment(jsonResponse);
                        return;
                    } else {
                        Toast.makeText(getBaseContext(), "No equipment registered.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception ex) {

            }
        }
    }

    public static class LogEquipmentDialogFragment extends DialogFragment implements View.OnClickListener {
        Button entranceButton, exitButton;
        JSONObject equipment;
        ImageView photo;
        EquipmentActivity activity;

        public void setEquipment(JSONObject equipment) {
            this.equipment = equipment;
        }

        public void setActivity(EquipmentActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setCancelable(true);
            getDialog().setTitle("Computing Equipment");
            View view = inflater.inflate(R.layout.equipment_popup_log_window, null, false);
            photo = (ImageView) view.findViewById(R.id.imageView_photo_log);
            entranceButton = (Button) view.findViewById(R.id.ib_entrance);
            exitButton = (Button) view.findViewById(R.id.ib_exit);
            entranceButton.setOnClickListener(this);
            exitButton.setOnClickListener(this);
            if (this.equipment != null && !this.equipment.isNull("ComputingEquipmentId")) {
                if (!this.equipment.isNull("Photo") && !this.equipment.optString("Photo").equals("null")) {
                    byte[] byteArray;
                    Bitmap bitmap;
                    byteArray = Base64
                            .decode(this.equipment.optString("Photo"), 0);
                    bitmap = BitmapFactory.decodeByteArray(byteArray, 0,
                            byteArray.length);
                    photo.setImageBitmap(bitmap);
                } else {
                    photo.setImageResource(R.drawable.im_nophotoavailable);
                }
            }
            return view;
        }

        public void onClick(View view) {
            int i = view.getId();
            if (i == R.id.ib_entrance) {
                activity.logEquipment(equipment, 1);
            } else if (i == R.id.ib_exit) {
                activity.logEquipment(equipment, 0);
            }
            dismiss();
        }
    }

    public static class EditEquipmentDialogFragment extends DialogFragment implements View.OnClickListener {
        Button saveButton, cancelButton, pickButton;
        Spinner spinner;
        JSONObject equipment;
        EditText brand, serial, comments;
        ImageView photo;
        EquipmentActivity activity;

        public void setEquipment(JSONObject equipment) {
            this.equipment = equipment;
        }

        public void setActivity(EquipmentActivity activity) {
            this.activity = activity;
        }

        public EditEquipmentDialogFragment() {
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setCancelable(false);
            getDialog().setTitle("Computing Equipment");
            View view = inflater.inflate(R.layout.equipment_popup_window, null, false);

            brand = (EditText) view.findViewById(R.id.editText_Brand);
            serial = (EditText) view.findViewById(R.id.editText_Serial);
            comments = (EditText) view.findViewById(R.id.editText_Observations);
            photo = (ImageView) view.findViewById(R.id.imageView_photo);

            saveButton = (Button) view.findViewById(R.id.ib_save);
            cancelButton = (Button) view.findViewById(R.id.ib_cancel);
            pickButton = (Button) view.findViewById(R.id.ib_picture);
            saveButton.setOnClickListener(this);
            cancelButton.setOnClickListener(this);
            pickButton.setOnClickListener(this);
            photo.setOnClickListener(this);
            spinner = (Spinner) view.findViewById(R.id.types_spinner);
            String serverURL = getResources().getString(R.string.service_url)
                    + "/ComputingEquipmentService/Retrieve/Types";
            Log.d("URL types", serverURL);
            new QueryTypesTask().execute(serverURL);
            if (this.equipment != null) {
                if (!this.equipment.isNull("ComputingEquipmentId")) {

                    brand.setText(this.equipment.optString("Brand"));
                    if (!this.equipment.isNull("SerialNumber") && !this.equipment.optString("SerialNumber").equals("null")) {
                        serial.setText(this.equipment.optString("SerialNumber"));
                    }
                    if (!this.equipment.isNull("Comments") && !this.equipment.optString("Comments").equals("null")) {
                        comments.setText(this.equipment.optString("Comments"));
                    }
                    if (!this.equipment.isNull("Photo") && !this.equipment.optString("Photo").equals("null")) {
                        byte[] byteArray;
                        Bitmap bitmap;
                        byteArray = Base64
                                .decode(this.equipment.optString("Photo"), 0);
                        bitmap = BitmapFactory.decodeByteArray(byteArray, 0,
                                byteArray.length);
                        photo.setImageBitmap(bitmap);
                    } else {
                        photo.setImageResource(R.drawable.im_nophotoavailable);
                    }
                } else {
                    brand.setText("");
                    serial.setText("");
                    comments.setText("");
                }
            }
            return view;
        }

        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.ib_save:
                    save();
                    break;
                case R.id.ib_cancel:
                    dismiss();
                    break;
                case R.id.ib_picture:
                    pickImage();
                    break;
                case R.id.imageView_photo:
                    pickImage();
                    break;
            }
        }

        private void save() {

            if (TextUtils.isEmpty(brand.getText().toString().trim())) {
                brand.setError("Enter equipment brand.");
                return;
            }
            if (TextUtils.isEmpty(serial.getText().toString().trim())) {
                serial.setError("Enter equipment serial number.");
                return;
            }
            try {
                this.equipment.put("Brand", brand.getText().toString().toUpperCase());
                this.equipment.put("SerialNumber", serial.getText().toString().toUpperCase());
                this.equipment.put("Comments", comments.getText().toString().toUpperCase());
                JSONObject type = new JSONObject();
                Type selected = (Type) spinner.getSelectedItem();
                type.accumulate("ComputingEquipmentTypeId", selected.getComputingEquipmentTypeId());
                type.accumulate("Description", selected.getDescription());
                this.equipment.put("ComputingEquipmentType", type);
            } catch (JSONException je) {

            }
            if (!this.equipment.isNull("ComputingEquipmentId")) {
                String serverURL = getResources().getString(R.string.service_url)
                        + "/ComputingEquipmentService/Update/" + this.equipment.optString("ComputingEquipmentId");
                new SaveEquipmentTask().execute(serverURL);
            } else {
                String serverURL = getResources().getString(R.string.service_url)
                        + "/ComputingEquipmentService/Create";
                new SaveEquipmentTask().execute(serverURL);
            }
        }

        static final int REQUEST_IMAGE_CAPTURE = 11;
        private String pictureImagePath = "";

        private void pickImage() {
            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String imageFileName = timeStamp + ".jpg";
            pictureImagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() + "/" + imageFileName;
            //pictureImagePath = Environment.getExternalStorageDirectory().toString()+"/Pictures";
            Log.d("path", pictureImagePath);
            File file = new File(pictureImagePath);
            Uri outputFileUri = Uri.fromFile(file);
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent intent) {
            super.onActivityResult(requestCode, resultCode, intent);
            Log.d("resultCode", resultCode + "");
            if (resultCode == 0) {
                return;
            }
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                try {
                    File imgFile = new File(pictureImagePath);
                    Log.d("exists", imgFile.exists() + "");
                    if (imgFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        Bitmap resized = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * 0.5), (int) (bitmap.getHeight() * 0.5), true);
                        photo.setImageBitmap(resized);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        resized.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                        Log.d("Encoded", encoded);
                        this.equipment.put("Photo", encoded);
                    }

                } catch (Exception e) {
                    Log.d("Exception", e.toString());
                }
            }
        }

        private void showTypes(JSONArray jsonArray) {
            List<Type> list = new ArrayList<>();
            try {
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(new Type(jsonArray.getJSONObject(i).getString("Description"), jsonArray.getJSONObject(i).getInt("ComputingEquipmentTypeId")));
                }
            } catch (Exception e) {
            }
            ArrayAdapter<Type> adapter = new ArrayAdapter<Type>(getActivity().getApplicationContext(), android.R.layout.simple_spinner_item, list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            if (this.equipment != null && !this.equipment.isNull("ComputingEquipmentId")) {
                for (int position = 0; position < adapter.getCount(); position++) {
                    try {
                        if (((Type) adapter.getItem(position)).getComputingEquipmentTypeId() == this.equipment.getJSONObject("ComputingEquipmentType").optInt("ComputingEquipmentTypeId")) {
                            spinner.setSelection(position);
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }

        private class Type {
            private String Description;

            public int getComputingEquipmentTypeId() {
                return ComputingEquipmentTypeId;
            }

            private int ComputingEquipmentTypeId;

            public Type(String description, int id) {
                Description = description;
                ComputingEquipmentTypeId = id;
            }

            public String getDescription() {
                return Description;
            }


            public String toString() {
                return Description;
            }
        }

        private class QueryTypesTask extends AsyncTask<String, String, String> {
            HttpURLConnection urlConnection;

            @SuppressWarnings("unchecked")
            protected String doInBackground(String... args) {
                StringBuilder result = new StringBuilder();
                try {
                    URL url = new URL(args[0]);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                } catch (Exception e) {
                    Log.d("Exception", e.getMessage());
                } finally {
                    urlConnection.disconnect();
                }
                return result.toString();
            }

            protected void onPostExecute(String result) {
                try {
                    if (result != null && !result.equals("")) {
                        JSONArray jsonResponse = new JSONArray(result);
                        if (jsonResponse.length() > 0) {
                            EditEquipmentDialogFragment.this.showTypes(jsonResponse);
                            return;
                        }
                    }
                } catch (Exception ex) {

                }
            }
        }

        private class SaveEquipmentTask extends AsyncTask<String, String, String> {
            HttpURLConnection urlConnection;

            @SuppressWarnings("unchecked")
            protected String doInBackground(String... args) {
                StringBuilder result = new StringBuilder();
                try {
                    URL url = new URL(args[0]);
                    //Log.d("URL",url.toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                    //Log.d("ComputingEquipment", equipment.toString());
                    OutputStream os = urlConnection.getOutputStream();
                    os.write(equipment.toString().getBytes("UTF-8"));
                    os.close();

                    /*OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
                    out.write(equipment.toString());*/

                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                } catch (IOException e) {
                    Log.d("Exception1", e.toString());
                } finally {
                    urlConnection.disconnect();
                }
                return result.toString();
            }

            protected void onPostExecute(String result) {
                try {
                    if (result != null && !result.equals("")) {
                        Log.d("Result", result);
                        Toast.makeText(
                                activity, "Equipment Saved", Toast.LENGTH_LONG)
                                .show();
                        activity.requestEquipment();
                        EditEquipmentDialogFragment.this.dismiss();
                    }
                } catch (Exception ex) {

                }
            }
        }
    }

    private class DeleteEquipmentTask extends AsyncTask<String, String, String> {
        HttpURLConnection urlConnection;

        @SuppressWarnings("unchecked")
        protected String doInBackground(String... args) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(args[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            } catch (Exception e) {
                Log.d("Exception", e.getMessage());
            } finally {
                urlConnection.disconnect();
            }
            return result.toString();
        }

        protected void onPostExecute(String result) {
            Log.d("Result", result);
            try {
                if (result != null && !result.equals("")) {
                    Toast.makeText(
                            EquipmentActivity.this, "Equipment Deleted", Toast.LENGTH_LONG)
                            .show();
                    EquipmentActivity.this.requestEquipment();
                }
            } catch (Exception ex) {

            }
        }
    }

    private class LogOperationTask extends AsyncTask<String, String, String> {
        HttpURLConnection urlConnection;

        @SuppressWarnings("unchecked")
        protected String doInBackground(String... args) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(args[0]);
                Log.d("Log URL", url.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }
            return result.toString();
        }

        protected void onPostExecute(String result) {
            try {
                JSONObject jsonResponse = new JSONObject(result);

                String log = jsonResponse.optString("log")
                        .contains("Entry") ? "Entrada" : "Salida";
                String response = jsonResponse.optString("records") + " " + log
                        + " Registrada";

                Toast.makeText(
                        EquipmentActivity.this, response, Toast.LENGTH_LONG)
                        .show();
            } catch (JSONException e) {
            }

        }
    }
}
