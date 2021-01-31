package com.wood.nio;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class TestNio {
    /**
     * 一、通过allocate获取缓冲区
     * 二、缓冲区存取数据的两个核心方法：
     * put():存入数据到缓冲区中
     * get():从缓冲区读取数据
     * 三、缓冲区的四个核心数据:
     * capacity:表示缓冲区容量，一旦声明不可改变
     * limit:界限，表示缓冲区中可以操作的数据大小(limit后数据不可读写)
     * position:表示缓冲区中正在读取数据的位置
     * 四、mark:标记，表示记录position当前位置，可以通过reset()恢复到mark的位置
     * 五、rewind():position位置归零，表示可以再一次读取数据
     * 0<=mark<=position<=limit<=capacity
     */
    @Test
    public void TestByteBuffer() {
        String email = "yuanyongwood@163.com";
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        byte[] emailByte = new byte[email.length()];
        //初始化buffer，当前为写模式
        this.printfBufferInfo(buffer.position(), buffer.limit(), buffer.capacity(), "buffer初始化1024个空间");
        //向buffer中添加email字节数组，并设置UFT-8编码,position向前移动
        buffer.put(email.getBytes(StandardCharsets.UTF_8));
        this.printfBufferInfo(buffer.position(), buffer.limit(), buffer.capacity(), "写模式：写入email信息");
        //切换为读数据模式,limit变为position的位置，position归零
        buffer.flip();
        this.printfBufferInfo(buffer.position(), buffer.limit(), buffer.capacity(), "切换至读模式");
        //读取数据至字节数组,position向前移动
        buffer.get(emailByte);
        this.printfBufferInfo(buffer.position(), buffer.limit(), buffer.capacity(), "读取所有字节");
        System.out.println(new String(emailByte));
        //清空缓冲区(只是重置了position,limit,capacity的位置，数据还存在)
        buffer.clear();
        byte[] temp = new byte[10];
        buffer.put(email.getBytes());
        buffer.flip();
        buffer.get(temp, 0, 10);
        for (byte b : temp) {
            System.out.print((char) b);
        }
        System.out.println();
        this.printfBufferInfo(buffer.position(), buffer.limit(), buffer.capacity(), "读取10个字节email");
        //记录position当前位置（10）
        buffer.mark();
        byte[] testRemarkByte = new byte[2];
        //从缓冲区读取2字节数据向testRemarkByte字节数组0的索引位置写入
        buffer.get(testRemarkByte, 0, 2);
        for (byte b : testRemarkByte) {
            System.out.print((char) b);
        }
        System.out.println();
        this.printfBufferInfo(buffer.position(), buffer.limit(), buffer.capacity(), "打印当前位置");
        buffer.reset();
        this.printfBufferInfo(buffer.position(), buffer.limit(), buffer.capacity(), "用reset恢复position至mark位置");
        //重读，position归零
        buffer.rewind();
        this.printfBufferInfo(buffer.position(), buffer.limit(), buffer.capacity(), "重读，position归零");
    }

    void printfBufferInfo(int position, int limit, int capacity, String tips) {
        System.out.printf("--------%s-------%nposition:%d%nlimit:%d%ncapacity:%d%n", tips, position, limit, capacity);
    }

    /**
     * 通过操作fileChannel进行读写文件
     */
    @Test
    public void TestFileChannel() throws Exception {
        //读取原文件路径
        Path sourcePath = Paths.get("/Users/yuanyongwood/Documents/dog.jpeg");
        //新增目标文件路径
        Path targetPath = Paths.get("/Users/yuanyongwood/Documents/target.jpeg");
        //开启一个读通道
        FileChannel readChannel = FileChannel.open(sourcePath, StandardOpenOption.READ);
        //开启一个写通道,CREATE:文件若存在则覆盖，CREATE_NEW:文件若存在则报错
        FileChannel writeChannel = FileChannel.open(targetPath, StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        //创建直接缓冲区
        ByteBuffer buffer=ByteBuffer.allocateDirect(1024);
        while(readChannel.read(buffer)!=-1){
            buffer.flip();
            writeChannel.write(buffer);
            buffer.clear();
        }
        readChannel.close();
        writeChannel.close();
    }

    /**
     * 通过内存映射文件的方式读写文件
     */
    @Test
    public void TestMappedByteBuffer() throws Exception{
        //读取原文件路径
        Path sourcePath = Paths.get("/Users/yuanyongwood/Documents/dog.jpeg");
        //新增目标文件路径
        Path targetPath = Paths.get("/Users/yuanyongwood/Documents/target.jpeg");
        FileChannel readChannel = FileChannel.open(sourcePath,StandardOpenOption.READ);
        FileChannel writeChannel = FileChannel.open(targetPath,StandardOpenOption.WRITE,StandardOpenOption.READ,StandardOpenOption.CREATE);
        //将readChannel中的数据映射到内存中
        MappedByteBuffer readMapped = readChannel.map(FileChannel.MapMode.READ_ONLY, 0, readChannel.size());
        MappedByteBuffer writeMapped = writeChannel.map(FileChannel.MapMode.READ_WRITE, 0, readChannel.size());
        byte[] temp=new byte[readMapped.limit()];
        //读写直接操作内存，将readMapped中的数据读取到temp数组中
        readMapped.get(temp);
        //读写直接操作内存，将readMapped中的数据写入到writeMapped中
        writeMapped.put(temp);
        readChannel.close();
        writeChannel.close();
    }

    /**
     * 通道之间直接传输数据
     */
    @Test
    public void Transform() throws Exception{
        //读取原文件路径
        Path sourcePath = Paths.get("/Users/yuanyongwood/Documents/dog.jpeg");
        //新增目标文件路径
        Path targetPath = Paths.get("/Users/yuanyongwood/Documents/target.jpeg");
        FileChannel readChannel = FileChannel.open(sourcePath,StandardOpenOption.READ);
        FileChannel writeChannel = FileChannel.open(targetPath,StandardOpenOption.WRITE,StandardOpenOption.READ,StandardOpenOption.CREATE);
        readChannel.transferTo(0,readChannel.size(),writeChannel);
        writeChannel.transferFrom(readChannel,0,readChannel.size());
        readChannel.close();
        writeChannel.close();
    }
}
