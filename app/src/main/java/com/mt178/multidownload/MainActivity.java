package com.mt178.multidownload;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    //    public static final String path = "http://gdown.baidu.com/data/wisegame/fb3bba73b5437246/QQ_264.apk";
    public static final int threadCount = 3;
    private ProgressBar pb_downlaod;
    private EditText et_path;
    private int runningThread = 3;
    private int currentProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pb_downlaod = (ProgressBar) findViewById(R.id.pb_downlaod);
        et_path = (EditText) findViewById(R.id.et_path);

    }

    public void download(View view) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    String path = et_path.getText().toString().trim();
                    if (TextUtils.isEmpty(path)) {
                        return;
                    }
                    URL url = new URL(path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(5000);//设置超时时间
                    conn.setRequestMethod("GET");//设置访问方式
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        int length = conn.getContentLength();//获取文件长度

                        pb_downlaod.setMax(length - 1);//设置progressbar的最大值

                        //创建一个与服务器大小相等的文件
                        RandomAccessFile raf = new RandomAccessFile("/sdcard/qq.apk", "rw");
                        raf.setLength(length);
                        raf.close();

                        int blockSize = length / 3;
                        for (int threadId = 1; threadId <= threadCount; threadId++) {
                            int startIndex = (threadId - 1) * blockSize;
                            int endIndex = threadId * blockSize - 1;
                            if (threadId == threadCount) {
                                endIndex = length;
                            }
                            new DownLoadThread(path, threadId, startIndex, endIndex).start();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class DownLoadThread extends Thread {
        private String path;
        private int threadId;
        private int startIndex, endIndex;

        public DownLoadThread(String path, int threadId, int startIndex, int endIndex) {
            this.path = path;
            this.threadId = threadId;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public void run() {
            super.run();
            try {
                File tempfile = new File("/sdcard/" + threadId + ".txt");
                if (tempfile.exists() && tempfile.length() > 0) {
                    FileInputStream fis = new FileInputStream(tempfile);
                    byte[] temp = new byte[1024];
                    int len = fis.read(temp);
                    String downloadLength = new String(temp, 0, len);
                    int downloadInt = Integer.parseInt(downloadLength);
                    //修改下载的真实位置
                    startIndex = downloadInt;

                    fis.close();
                }
                URL url = new URL(path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);
                int code = conn.getResponseCode();
                if (code == 206) {
                    InputStream is = conn.getInputStream();
                    RandomAccessFile raf = new RandomAccessFile("/sdcard/qq.apk", "rw");
                    // 定位文件
                    raf.seek(startIndex);

                    int total = 0;
                    int len = 0;
                    byte[] buffer = new byte[1024];
                    while ((len = is.read(buffer)) != -1) {
                        RandomAccessFile file = new RandomAccessFile("/sdcard/" + threadId + ".txt", "rw");
                        total += len;
                        System.out.println("线程" + threadId + "下载到了" + total + startIndex);
                        //记录当前线程下载的数据位置
                        file.write((total + startIndex - 1 + "").getBytes());
                        file.close();
                        raf.write(buffer, 0, len);

                        synchronized (MainActivity.this) {
                            // 获取所有线程下载的总进度
                            currentProgress += len;
                            // 更改界面上progressbar的进度(progressbar和progressdialog可以在子线程里修改UI)
                            pb_downlaod.setProgress(currentProgress);
                        }
                    }

                    raf.close();
                    is.close();
                }

                System.out.println("线程：" + threadId + "下载完毕了");
                runningThread--;

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (runningThread == 0) {
                    for (int i = 1; i <= threadCount; i++) {
                        File deleteFile = new File("/mnt/sdcard/" + i + ".txt");
                        deleteFile.delete();
                    }

                }
            }
        }
    }

}
