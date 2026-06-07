package com.dpichanger;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.*;
import dev.rikka.shizuku.Shizuku;
import java.io.*;

public class MainActivity extends Activity {
    
    private EditText dpiInput;
    private TextView statusText;
    private boolean shizukuReady = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        dpiInput = findViewById(R.id.dpiInput);
        statusText = findViewById(R.id.statusText);
        
        Button applyBtn = findViewById(R.id.applyBtn);
        Button resetBtn = findViewById(R.id.resetBtn);
        
        // Check Shizuku
        checkShizuku();
        
        applyBtn.setOnClickListener(v -> {
            String dpi = dpiInput.getText().toString();
            if(!dpi.isEmpty()) {
                runShizukuCommand("wm density " + dpi);
            } else {
                statusText.setText("Please enter DPI!");
            }
        });
        
        resetBtn.setOnClickListener(v -> {
            runShizukuCommand("wm density reset");
        });
    }
    
    private void checkShizuku() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    shizukuReady = true;
                    statusText.setText("✅ Shizuku ready! Enter DPI and apply.");
                } else {
                    statusText.setText("⚠️ Requesting Shizuku permission...");
                    Shizuku.requestPermission(1001);
                    
                    Shizuku.addRequestPermissionResultListener((code, result) -> {
                        if (result == PackageManager.PERMISSION_GRANTED) {
                            shizukuReady = true;
                            runOnUiThread(() -> statusText.setText("✅ Permission granted! Ready."));
                        } else {
                            runOnUiThread(() -> statusText.setText("❌ Permission denied! Allow in Shizuku app."));
                        }
                    });
                }
            } else {
                statusText.setText("❌ Shizuku not running!\n\nSolution:\n1. Install Shizuku from Play Store\n2. Open Shizuku & start service\n3. Come back to this app");
                shizukuReady = false;
            }
        } catch (Exception e) {
            statusText.setText("❌ Error: " + e.getMessage());
            shizukuReady = false;
        }
    }
    
    private void runShizukuCommand(String cmd) {
        if (!shizukuReady) {
            statusText.setText("❌ Shizuku not ready! Check status.");
            return;
        }
        
        statusText.setText("⏳ Running: " + cmd);
        
        new Thread(() -> {
            try {
                // Shizuku ke through command execute
                Process process = Shizuku.newProcess(new String[]{"sh", "-c", cmd}, null, null);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
                process.waitFor();
                
                String result = output.toString();
                runOnUiThread(() -> {
                    if (process.exitValue() == 0) {
                        statusText.setText("✅ Success! DPI changed.\n" + cmd);
                        Toast.makeText(MainActivity.this, "DPI Applied!", Toast.LENGTH_SHORT).show();
                    } else {
                        statusText.setText("❌ Failed!\n" + result);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("❌ Error: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    }
