package com.lin.jiang.appart.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.lin.jiang.appart.utils.MyUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class TCPServerService extends Service {

    private static final String TAG = "TCPServerService";

    private boolean mIsServiceDestoried = false;

    private String[] mDefinedMessages = {
            "你好啊，哈哈",
            "请问你叫什么名字啊？",
            "今天天气不错啊",
            "你知道吗？我可是可以和多人同时聊天的哦",
            "给你讲个笑话吧：据说爱笑的人运气都不会太差，不知道真假"
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        new Thread(new TcpServer()).start();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void responseClient(Socket client) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())));
        out.println("欢迎来到聊天室！");

        while (!mIsServiceDestoried) {
            String str = in.readLine();
            System.out.println("msg from client: " + str);
            if (str == null) break;

            int i = new Random().nextInt(mDefinedMessages.length);
            String msg = mDefinedMessages[i];
            out.println(msg);
            System.out.println("send: " + msg);
        }

        System.out.println("client quit.");
        MyUtils.close(out);
        MyUtils.close(in);
        client.close();
    }

    private class TcpServer implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "run: TcpServer");
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(8688);
            } catch (IOException e) {
                System.out.println("establish tcp server failed, port: 8688");
                e.printStackTrace();
                return;
            }

            while (!mIsServiceDestoried) {
                try {
                    final Socket client = serverSocket.accept();
                    System.out.println("accept");
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                responseClient(client);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
