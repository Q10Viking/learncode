package org.hzz.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FileTest {
    public static void main(String[] args) throws IOException {
        // 系统中现在没有这个文件
        File file = new File("D:\\SourceCode\\hello.txt");
        System.out.println(file.getAbsolutePath());
        System.out.println(file.exists());
        // 创建这个文件
        FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        MappedByteBuffer byteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 1024 * 1024);
        // 写入数据
        ByteBuffer wrap = ByteBuffer.wrap("hello".getBytes());
        byteBuffer.put(wrap);
        System.out.println(file.exists()); // 创建了一个1024kb的文件
    }
}
/**
 * D:\SourceCode\hello.txt
 * false
 * true
 */