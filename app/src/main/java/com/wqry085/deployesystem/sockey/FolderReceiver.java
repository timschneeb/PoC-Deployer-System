package com.wqry085.deployesystem.sockey;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.wqry085.deployesystem.R;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FolderReceiver {
    private static final String TAG = "FolderReceiver";
    private static final int PORT = 56423;
    
    private Context context;
    private ProgressDialog progressDialog;
    private AlertDialog resultDialog;
    private volatile boolean isReceiving = false;
    private Handler mainHandler;
    private ServerSocket serverSocket;
    private ExecutorService networkExecutor;
    
    // 协议常量
    private static final byte TYPE_FILE = 0x01;
    private static final byte TYPE_DIRECTORY = 0x02;
    
    public FolderReceiver(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.networkExecutor = Executors.newSingleThreadExecutor();
    }
    
    public void startReceiving() {
        if (isReceiving) {
            showToast(context.getString(R.string.received_service_running));
            return;
        }
        
        networkExecutor.execute(this::startReceivingInternal);
    }
    
    private void startReceivingInternal() {
        isReceiving = true;
        showWaitingDialog(context.getString(R.string.waiting_client_connection, PORT));
        Log.i(TAG, context.getString(R.string.start_listen_port, PORT));

        try {
            serverSocket = new ServerSocket(PORT);
            serverSocket.setSoTimeout(1000); // 1秒超时，用于检查取消状态
            
            while (isReceiving) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    Log.i(TAG, context.getString(R.string.client_connected, clientSocket.getInetAddress()));

                    // 处理文件传输
                    handleFileTransfer(clientSocket);
                    
                } catch (java.net.SocketTimeoutException e) {
                    // 超时，继续循环检查取消状态
                    continue;
                } catch (IOException e) {
                    if (isReceiving) {
                        Log.e(TAG, context.getString(R.string.accept_connection_error, e.getMessage()));
                    }
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, context.getString(R.string.server_start_failed, e.getMessage()));
            showToast(context.getString(R.string.server_start_failed, e.getMessage()));
        } finally {
            closeServerSocket();
            isReceiving = false;
            dismissWaitingDialog();
            Log.i(TAG, context.getString(R.string.received_service_stopped));
        }
    }
    
    private void handleFileTransfer(Socket clientSocket) {
        String baseDir = context.getExternalFilesDir(null).getAbsolutePath();
        int totalFiles = 0;
        int receivedFiles = 0;
        
        Log.i(TAG, context.getString(R.string.start_receive_file, baseDir));
        updateProgressDialog(context.getString(R.string.start_receive_file_progress), 0, 100);

        try {
            InputStream inputStream = clientSocket.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            
            while (isReceiving) {
                // 读取类型字节
                int typeByte = bis.read();
                if (typeByte == -1) {
                    Log.i(TAG, context.getString(R.string.stream_end_transfer_complete));
                    break; // 正常结束
                }
                
                byte type = (byte) typeByte;
                Log.d(TAG, context.getString(R.string.read_type, type));

                // 读取路径长度 (4字节，大端序)
                byte[] pathLenBytes = readExactly(bis, 4);
                if (pathLenBytes == null) break;
                
                int pathLength = ByteBuffer.wrap(pathLenBytes)
                                         .order(ByteOrder.BIG_ENDIAN)
                                         .getInt();
                Log.d(TAG, context.getString(R.string.path_length, pathLength));

                if (pathLength <= 0 || pathLength > 8192) {
                    Log.e(TAG, context.getString(R.string.invalid_path_length, pathLength));
                    break;
                }
                
                // 读取路径
                byte[] pathBytes = readExactly(bis, pathLength);
                if (pathBytes == null) break;
                
                String relativePath = new String(pathBytes, "UTF-8");
                Log.d(TAG, context.getString(R.string.relative_path, relativePath));

                // 读取数据长度 (8字节，大端序)
                byte[] dataLenBytes = readExactly(bis, 8);
                if (dataLenBytes == null) break;
                
                long dataLength = ByteBuffer.wrap(dataLenBytes)
                                          .order(ByteOrder.BIG_ENDIAN)
                                          .getLong();
                Log.d(TAG, context.getString(R.string.data_length, dataLength));

                String fullPath = baseDir + File.separator + relativePath;
                
                if (type == TYPE_FILE) {
                    totalFiles++;
                    receivedFiles++;
                    
                    // 更新UI
                    final String status = String.format(context.getString(R.string.receive_file_progress),
                            relativePath, formatFileSize(dataLength), receivedFiles, totalFiles);
                    
                    updateProgressDialog(status, (receivedFiles * 100) / Math.max(totalFiles, 1), 100);
                    
                    Log.i(TAG, context.getString(R.string.receive_file, relativePath, dataLength));

                    // 创建父目录
                    File file = new File(fullPath);
                    File parentDir = file.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        if (!parentDir.mkdirs()) {
                            Log.e(TAG, context.getString(R.string.create_dir_failed, parentDir.getAbsolutePath()));
                            break;
                        }
                    }
                    
                    // 接收文件内容
                    if (!receiveFileContent(bis, file, dataLength)) {
                        Log.e(TAG, context.getString(R.string.file_content_receive_failed, relativePath));
                        break;
                    }
                    
                    Log.i(TAG, context.getString(R.string.file_receive_complete, relativePath));

                } else if (type == TYPE_DIRECTORY) {
                    Log.i(TAG, context.getString(R.string.create_dir, relativePath));

                    File dir = new File(fullPath);
                    if (!dir.exists() && !dir.mkdirs()) {
                        Log.e(TAG, context.getString(R.string.create_dir_failed, fullPath));
                        break;
                    }
                    
                    // 更新UI显示目录创建
                    updateProgressDialog(String.format(context.getString(R.string.create_dir_progress), relativePath),
                                       (receivedFiles * 100) / Math.max(totalFiles, 1), 100);
                } else {
                    Log.e(TAG, context.getString(R.string.unknown_type, type));
                    break;
                }
            }
            
            // 传输完成
            if (isReceiving) {
                final int finalReceived = receivedFiles;
                mainHandler.post(() -> {
                    showToast(context.getString(R.string.file_receive_complete_count, finalReceived));
                    showWaitingDialog(context.getString(R.string.prepare_next_transfer));
                });
                Log.i(TAG, context.getString(R.string.file_receive_complete_total, receivedFiles));
            }
            
        } catch (IOException e) {
            Log.e(TAG, context.getString(R.string.file_transfer_error, e.getMessage()));
            showToast(context.getString(R.string.file_receive_error, e.getMessage()));
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Log.e(TAG, context.getString(R.string.close_client_socket_error, e.getMessage()));
            }
        }
    }
    
    private byte[] readExactly(InputStream is, int length) throws IOException {
        byte[] buffer = new byte[length];
        int totalRead = 0;
        
        while (totalRead < length && isReceiving) {
            int read = is.read(buffer, totalRead, length - totalRead);
            if (read == -1) {
                Log.e(TAG, context.getString(R.string.stream_early_end, length, totalRead));
                return null;
            }
            totalRead += read;
        }
        
        return totalRead == length ? buffer : null;
    }
    
    private boolean receiveFileContent(InputStream is, File file, long dataLength) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            
            while (totalRead < dataLength && isReceiving) {
                int toRead = (int) Math.min(buffer.length, dataLength - totalRead);
                int read = is.read(buffer, 0, toRead);
                
                if (read == -1) {
                    Log.e(TAG, context.getString(R.string.file_content_read_early_end));
                    return false;
                }
                
                fos.write(buffer, 0, read);
                totalRead += read;
                
                // 大文件进度更新（每1MB更新一次）
                if (dataLength > 1024 * 1024 && totalRead % (1024 * 1024) == 0) {
                    int progress = (int) ((totalRead * 100) / dataLength);
                    Log.d(TAG, context.getString(R.string.file_transfer_progress, progress));
                }
            }
            
            return totalRead == dataLength;
        } catch (IOException e) {
            Log.e(TAG, context.getString(R.string.write_file_error, e.getMessage()));
            return false;
        }
    }
    
    public void stopReceiving() {
        isReceiving = false;
        closeServerSocket();
        dismissWaitingDialog();
    }
    
    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭服务器socket错误: " + e.getMessage());
            }
            serverSocket = null;
        }
    }
    
    private void showWaitingDialog(String message) {
        mainHandler.post(() -> {
            dismissWaitingDialog();
        });
    }
    
    private void updateProgressDialog(String message, int progress, int max) {
        mainHandler.post(() -> {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(context);
                progressDialog.setTitle(context.getString(R.string.app_data_receiving));
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setOnCancelListener(dialog -> stopReceiving());
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMax(max);
            }
            
            progressDialog.setMessage(message);
            progressDialog.setProgress(progress);
            
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        });
    }
    
    private void dismissWaitingDialog() {
        mainHandler.post(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        });
    }
    
    private void showToast(String message) {
        mainHandler.post(() -> {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show();
        });
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        else if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        else return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }
    
    public boolean isReceiving() {
        return isReceiving;
    }
    
    public void release() {
        stopReceiving();
        if (networkExecutor != null) {
            networkExecutor.shutdownNow();
        }
    }
}