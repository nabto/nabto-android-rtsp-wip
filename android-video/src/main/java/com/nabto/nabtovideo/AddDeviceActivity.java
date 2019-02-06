package com.nabto.nabtovideo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.nabto.nabtovideo.util.Storage;
import com.nabto.nabtovideo.util.VideoDevice;
import com.nabto.qr.IntentIntegrator;
import com.nabto.qr.IntentResult;
import com.nabto.nabtovideo.R;

import java.util.ArrayList;

public class AddDeviceActivity extends Activity implements AdapterView.OnItemSelectedListener {

    int deviceType = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_device);

        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
        mAdapter.add("Web");
        mAdapter.add("MPEG");

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setAdapter(mAdapter);
        spinner.setOnItemSelectedListener(this);

        Button qrButton = (Button) findViewById(R.id.qr_scan);
        qrButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openQrScan();
            }
        });

        Intent intent = getIntent();
        if (intent.hasExtra(VideoDevice.DEVICETITLE)) {
            EditText titleEditText = (EditText)findViewById(R.id.titleEditText);
            titleEditText.setText(intent.getStringExtra(VideoDevice.DEVICETITLE));
        }
        if (intent.hasExtra(VideoDevice.DEVICENAME)) {
            EditText nameEditText = (EditText)findViewById(R.id.nameEditText);
            nameEditText.setText(intent.getStringExtra(VideoDevice.DEVICENAME));
        }
        if (intent.hasExtra(VideoDevice.DEVICEURL)) {
            EditText urlEditText = (EditText)findViewById(R.id.urlEditText);
            urlEditText.setText(intent.getStringExtra(VideoDevice.DEVICEURL));
        }
        if (intent.hasExtra(VideoDevice.DEVICEHOST)) {
            EditText hostEditText = (EditText)findViewById(R.id.hostEditText);
            hostEditText.setText(String.valueOf(intent.getStringExtra(VideoDevice.DEVICEHOST)));
        }
        if (intent.hasExtra(VideoDevice.DEVICEPORT)) {
            EditText portEditText = (EditText)findViewById(R.id.portEditText);
            portEditText.setText(String.valueOf(intent.getIntExtra(VideoDevice.DEVICEPORT, 80)));
        }
        if (intent.hasExtra(VideoDevice.DEVICETYPE)) {
            spinner.setSelection(intent.getIntExtra(VideoDevice.DEVICETYPE, 1) - 1);
        }
    }

    private void openQrScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getActionBar().setDisplayHomeAsUpEnabled(true);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_add_device, menu);
        return super.onCreateOptionsMenu(menu);
	}

    private void saveDevice() {
        Log.d(this.getClass().getSimpleName(), "Saving device");

        EditText titleEditText = (EditText)findViewById(R.id.titleEditText);
        EditText nameEditText = (EditText)findViewById(R.id.nameEditText);
        EditText hostEditText = (EditText)findViewById(R.id.hostEditText);
        EditText portEditText = (EditText)findViewById(R.id.portEditText);
        EditText urlEditText = (EditText)findViewById(R.id.urlEditText);
        EditText userEditText = (EditText)findViewById(R.id.basicAuthUserEditText);
        EditText passEditText = (EditText)findViewById(R.id.basicAuthPasswordEditText);

        String title = titleEditText.getText().toString();
        String name = nameEditText.getText().toString().toLowerCase();
        String host = hostEditText.getText().toString().toLowerCase();
        String port = portEditText.getText().toString();
        String url = urlEditText.getText().toString();
        String user = userEditText.getText().toString();
        String pass = passEditText.getText().toString();

        if (name.isEmpty() || port.isEmpty()) {
            finish();
            return;
        }

        int portInt = 0;
        if (!port.equals("")) {
            portInt = Integer.parseInt(port);
        }

        if (host.isEmpty()) {
            host = "127.0.0.1";
        }   

        int typeInt = deviceType;

        VideoDevice device = new VideoDevice(title, name, url, portInt, typeInt, 0, host, user, pass);
        Storage storage = new Storage();
        storage.addFavorite(getApplicationContext(), device);

        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == 0) {
            return;
        }

        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            Storage storage = new Storage();
            storage.addDeviceFromUriString(getApplicationContext(), scanResult.getContents());
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == android.R.id.home) {
    		finish();
    	}
        else if (item.getItemId() == R.id.action_save) {
            saveDevice();
        }
    	return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        // Skip type UNKNOWN
        deviceType = i + 1;

        TextView t = (TextView)findViewById(R.id.typeDescriptionTextView);
        String _string = "";
        switch (i) {
            case 0:
                _string = getString(R.string.web_description);
                break;
            case 1:
                _string = getString(R.string.mpeg_description);
                break;
        }
        t.setText(_string);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
