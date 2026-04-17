package com.example.menu_pos.printer;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BluetoothPrinterManager {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static volatile BluetoothPrinterManager INSTANCE;

    private final Context appContext;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final Object lock = new Object();
    @Nullable private BluetoothSocket socket;
    @Nullable private OutputStream outputStream;
    @Nullable private String connectedMac;

    public interface Listener {
        void onConnectionChanged(boolean connected);
    }

    @Nullable private Listener listener;

    private BluetoothPrinterManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static BluetoothPrinterManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BluetoothPrinterManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BluetoothPrinterManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void autoConnectIfEnabled() {
        if (!PrinterPrefs.isAutoConnectEnabled(appContext)) return;
        String mac = PrinterPrefs.getPrinterMac(appContext);
        if (mac == null) return;
        connect(mac);
    }

    public void connect(String macAddress) {
        if (macAddress == null || macAddress.trim().isEmpty()) return;
        ioExecutor.execute(() -> connectBlocking(macAddress.trim()));
    }

    /** Blocking connect attempt for worker threads (never call on main thread). */
    public boolean connectBlockingWithRetry(String macAddress) {
        if (macAddress == null || macAddress.trim().isEmpty()) return false;
        return connectBlocking(macAddress.trim());
    }

    public void disconnect() {
        ioExecutor.execute(this::disconnectBlocking);
    }

    public boolean isConnected() {
        synchronized (lock) {
            return socket != null && socket.isConnected() && outputStream != null;
        }
    }

    @Nullable
    public OutputStream getOutputStream() {
        synchronized (lock) {
            return outputStream;
        }
    }

    @Nullable
    public String getConnectedMac() {
        synchronized (lock) {
            return connectedMac;
        }
    }

    @SuppressLint("MissingPermission")
    private boolean connectBlocking(String macAddress) {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                notifyConnection(false);
                return false;
            }
            BluetoothDevice device = adapter.getRemoteDevice(macAddress);
            if (device == null) {
                notifyConnection(false);
                return false;
            }

            disconnectBlocking();
            adapter.cancelDiscovery();

            BluetoothSocket s = null;
            OutputStream os = null;

            // Try standard secure RFCOMM first.
            try {
                s = device.createRfcommSocketToServiceRecord(SPP_UUID);
                s.connect();
                os = s.getOutputStream();
            } catch (Exception secureEx) {
                closeQuietly(s);
                s = null;
            }

            // Vozy/portable printers often prefer insecure RFCOMM.
            if (s == null) {
                try {
                    s = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                    s.connect();
                    os = s.getOutputStream();
                } catch (Exception insecureEx) {
                    closeQuietly(s);
                    s = null;
                }
            }

            // Last fallback: channel 1 via reflection.
            if (s == null) {
                try {
                    Method m = device.getClass().getMethod("createRfcommSocket", int.class);
                    s = (BluetoothSocket) m.invoke(device, 1);
                    s.connect();
                    os = s.getOutputStream();
                } catch (Exception reflectionEx) {
                    closeQuietly(s);
                    s = null;
                }
            }

            if (s == null || os == null) {
                disconnectBlocking();
                notifyConnection(false);
                return false;
            }

            synchronized (lock) {
                socket = s;
                outputStream = os;
                connectedMac = macAddress;
            }
            notifyConnection(true);
            return true;
        } catch (Exception e) {
            disconnectBlocking();
            notifyConnection(false);
            return false;
        }
    }

    private void disconnectBlocking() {
        BluetoothSocket s;
        synchronized (lock) {
            s = socket;
            socket = null;
            outputStream = null;
            connectedMac = null;
        }
        if (s != null) {
            closeQuietly(s);
        }
        notifyConnection(false);
    }

    private void closeQuietly(@Nullable BluetoothSocket s) {
        if (s == null) return;
        try { s.close(); } catch (IOException ignored) {}
    }

    private void notifyConnection(boolean connected) {
        Listener l = listener;
        if (l != null) l.onConnectionChanged(connected);
    }
}

