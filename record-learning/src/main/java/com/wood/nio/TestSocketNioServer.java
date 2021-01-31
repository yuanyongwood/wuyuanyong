package com.wood.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestSocketNioServer {
    private static final Logger log = LoggerFactory.getLogger(TestSocketNioServer.class);
    private Selector selector;
    private final static ExecutorService threadPool = Executors.newFixedThreadPool(1);

    public TestSocketNioServer(int serverPort) {
        this.init(serverPort);
    }

    private void init(int port) {
        ServerSocketChannel serverSocketChannel;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            //绑定端口号，此处产生系统调用
            serverSocketChannel.bind(new InetSocketAddress(port));
            //设置为非阻塞
            serverSocketChannel.configureBlocking(false);
            this.selector = Selector.open();
            //为通道注册选择器,监听客户端连接事件或者客户端发生错误
            serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void execute() throws Exception {
        //select()方法会一直阻塞直到有监听的事件发生
        while (this.selector.select() > 0) {
            Iterator<SelectionKey> selectionKeys = this.selector.selectedKeys().iterator();
            while (selectionKeys.hasNext()) {
                SelectionKey selectionKey = selectionKeys.next();
                //重点！必须移除此次监听到的事件，否则会出现重复处理
                selectionKeys.remove();
                if (selectionKey.isAcceptable()) {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
                    SocketChannel socketChannel = serverSocketChannel.accept();
                        log.debug("监听到客户端连接，地址：{}", socketChannel.getRemoteAddress());
                        //选择器监听客户端发送数据事件
                        socketChannel.configureBlocking(false);
                        socketChannel.register(this.selector, SelectionKey.OP_READ);
                } else {
                    //分配出其他线程来完成客户端发送过来的数据处理
                    threadPool.execute(new Handler(selectionKey));
                }
            }
        }
    }

    public static class Handler implements Runnable {
        private final SelectionKey selectionKey;

        public Handler(SelectionKey selectionKey) {
            this.selectionKey = selectionKey;
        }

        @Override
        public void run() {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int len;
            try {
                while ((len=socketChannel.read(buffer))!=0) {
                    buffer.flip();
                    log.debug("监听到{}发来的消息：{}",socketChannel.getRemoteAddress(),new String(buffer.array(),0,len));
                    ByteBuffer response=ByteBuffer.allocate(1024);
                    response.put("成功接受到消息！\n".getBytes());
                    response.flip();
                    socketChannel.write(response);
                    buffer.clear();
                }
            } catch (IOException e) {
                try {
                    log.info("发生IO异常，关闭连接");
                    socketChannel.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

    }

    public static void main(String[] args) {
        try {
            new TestSocketNioServer(9090).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
