package com.mcbath.rebecca.bledemo;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/**
 * Created by Rebecca McBath
 * on 2019-07-10.
 */
public class MainActivity extends AppCompatActivity implements ScanResultsConsumer {

	private boolean ble_scanning = false;
	private ListAdapter ble_device_list_adapter;
	private BleScanner ble_scanner;
	private static final long SCAN_TIMEOUT = 5000;
	private static final int REQUEST_LOCATION = 0;
	private static String PERMISSIONS_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
	private boolean permissions_granted = false;
	private int device_count = 0;
	private Toast toast;

	private static class ViewHolder {
		private TextView macLabel;
		private TextView deviceName;
		private TextView macAddress;
		private TextView rssiLabel;
		private TextView rssi;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "Bluetooth low energy is not supported", Toast.LENGTH_SHORT).show();
			finish();
		}

		setButtonText();

		ble_device_list_adapter = new ListAdapter();
		ListView listView = this.findViewById(R.id.deviceList);
		listView.setAdapter(ble_device_list_adapter);
		ble_scanner = new BleScanner(this.getApplicationContext());

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
			                        int position, long id) {

				if (ble_scanning) {
					setScanState(false);
					ble_scanner.stopScanning();
				}

				BluetoothDevice device = ble_device_list_adapter.getDevice(position);
				if (toast != null) {
					toast.cancel();
				}
				Intent intent = new Intent(MainActivity.this, PeripheralControlActivity.class);
				intent.putExtra(PeripheralControlActivity.EXTRA_NAME, device.getName());
				intent.putExtra(PeripheralControlActivity.EXTRA_ID, device.getAddress());
				startActivity(intent);

			}
		});
	}

	private void setButtonText() {
		String text = "";
		text = Constants.FIND;
		final String button_text = text;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				((TextView) MainActivity.this.findViewById(R.id.scanButton)).setText(button_text);
			}
		});
	}

	public void onScan(View view) {
		if (!ble_scanner.isScanning()) {
			device_count = 0;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

				if (checkSelfPermission(PERMISSIONS_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					permissions_granted = false;
					requestLocationPermission();
				} else {
					Log.i(Constants.TAG, "Location permission has already been granted. Starting scanning.");
					permissions_granted = true;
					startScanning();
				}
			} else {
				// runtime permission not necessay for devices below M
				permissions_granted = true;
				startScanning();
			}
		} else {
			ble_scanner.stopScanning();
		}
	}

	private void requestLocationPermission() {
		Log.i(Constants.TAG, "Location permission has NOT yet been granted. Requesting permission.");
		if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
			Log.i(Constants.TAG, "Displaying location permission rationale to provide additional context.");

			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Permission Required");
			builder.setMessage("Please grant Location access so this application can perform Bluetooth scanning");
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					Log.d(Constants.TAG, "Requesting permissions after explanation");
					ActivityCompat.requestPermissions(MainActivity.this, new String[]{PERMISSIONS_LOCATION}, REQUEST_LOCATION);
				}
			});
			builder.show();
		} else {
			ActivityCompat.requestPermissions(this, new String[]{PERMISSIONS_LOCATION}, REQUEST_LOCATION);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_LOCATION) {

			Log.i(Constants.TAG, "Received response for location permission request.");
			// Check if the only required permission has been granted
			if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Location permission has been granted
				Log.i(Constants.TAG, "Location permission has now been granted. Scanning.....");
				permissions_granted = true;
				startScanning();
			} else {
				Log.i(Constants.TAG, "Location permission was NOT granted.");
			}
		} else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	private void simpleToast(String message, int duration) {
		toast = Toast.makeText(this, message, duration);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	public void startScanning() {
		if (permissions_granted) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ble_device_list_adapter.clear();
					ble_device_list_adapter.notifyDataSetChanged();
				}
			});
			simpleToast(Constants.SCANNING, 2000);
			ble_scanner.startScanning(this, SCAN_TIMEOUT);
		} else {
			Log.i(Constants.TAG, "Permission to perform Bluetooth scanning was not yet granted");
		}
	}

	public void setScanState(boolean value) {
		ble_scanning = value;
		((Button) this.findViewById(R.id.scanButton)).setText(value ? Constants.STOP_SCANNING : Constants.FIND);
	}

	@Override
	public void scanningStarted() {
		setScanState(true);
	}

	@Override
	public void scanningStopped() {
		if (toast != null) {
			toast.cancel();
		}
		setScanState(false);
	}

	@Override
	public void candidateBleDevice(final BluetoothDevice device, byte[] scan_record, final int rssi) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ble_device_list_adapter.addDevice(device, rssi);
				ble_device_list_adapter.notifyDataSetChanged();
				device_count++;
			}
		});
	}

	// --------------------
	//      Adapter
	// --------------------
	public class ListAdapter extends BaseAdapter {
		private ArrayList<BluetoothDevice> ble_devices;

		private ListAdapter() {
			super();
			ble_devices = new ArrayList<>();
		}

		private void addDevice(BluetoothDevice device, int rssi) {
			if (!ble_devices.contains(device)) {
				ble_devices.add(device);
			}
		}

		public boolean contains(BluetoothDevice device) {
			return ble_devices.contains(device);
		}

		private BluetoothDevice getDevice(int position) {
			return ble_devices.get(position);
		}

		public void clear() {
			ble_devices.clear();
		}

		@Override
		public int getCount() {
			return ble_devices.size();
		}

		@Override
		public Object getItem(int i) {
			return ble_devices.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			ViewHolder viewHolder;
			if (view == null) {
				view = MainActivity.this.getLayoutInflater().inflate(R.layout.list_row, null);
				viewHolder = new ViewHolder();

				viewHolder.deviceName = view.findViewById(R.id.nameTextView);
				viewHolder.macLabel = view.findViewById(R.id.bdaddr_label);
				viewHolder.macAddress = view.findViewById(R.id.bdaddr);
				//				viewHolder.rssiLabel = view.findViewById(R.id.rssi_label);
				//				viewHolder.rssi = view.findViewById(R.id.rssi);
				view.setTag(viewHolder);

			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			BluetoothDevice device = ble_devices.get(i);
			String deviceName = device.getName();

			if (deviceName != null && deviceName.length() > 0) {
				viewHolder.deviceName.setText(deviceName);
			} else {
				viewHolder.deviceName.setText("unknown device");
			}

			// Mac Address
			viewHolder.macLabel.setText("MAC Address:");
			viewHolder.macAddress.setText(device.getAddress());
			//			viewHolder.rssiLabel.setText("RSSI:");
			//			viewHolder.rssi.setText("placeholder rssi");
			return view;

		}
	}
}
