package org.hzz.chapter01;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CopyBytes {
    public static void main(String[] args) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        try{
            // 路径问题，当前路径  D:\learncode\filesystem
            System.out.println("Working Directory = " + System.getProperty("user.dir"));
            // 所以相对路径是在D:\learncode\filesystem\xanadu.txt 且必须要有
            in = new FileInputStream("xanadu.txt");
            // outagain.txt 不存在则直接创建
            out = new FileOutputStream("outagain.txt");

            int c;
            while((c = in.read())!= -1){
                out.write(c);
            }
        }finally {
            if(in != null){
                in.close();
            }

            if(out != null){
                out.close();
            }
        }
    }
}
