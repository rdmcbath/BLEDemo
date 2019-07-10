package com.mcbath.rebecca.bledemo;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Rebecca McBath
 * on 2019-07-10.
 */
public interface ScanResultsConsumer {

	public void candidateBleDevice(BluetoothDevice device, byte[] scan_record, int rssi); public void scanningStarted();
	public void scanningStopped();
}