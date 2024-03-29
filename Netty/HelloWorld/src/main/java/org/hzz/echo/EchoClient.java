package org.hzz.echo;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class EchoClient {
    private static final Logger LOG = LoggerFactory.getLogger(EchoClient.class);
    private final int port;
    private final String host;

    public EchoClient(int port, String host) {
        this.port = port;
        this.host = host;
    }

    public static void main(String[] args) throws InterruptedException {
        new EchoClient(10000,"127.0.0.1").start();
    }

    private void start() throws InterruptedException {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        EchoClientHandler handler = new EchoClientHandler();
        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host,port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(handler);
                        }
                    });
            /**--------------- 源码研究的部分---------------------------*/
            ChannelFuture connect = bootstrap.connect();
            connect.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    System.out.println("Hello World");
                }
            });
            ChannelFuture f = connect.sync();
            /**--------------- 源码研究的部分---------------------------*/
            f.channel().closeFuture().sync();
            LOG.info("客户端关闭");
        }finally {
            eventLoopGroup.shutdownGracefully().sync();
        }
    }
}
