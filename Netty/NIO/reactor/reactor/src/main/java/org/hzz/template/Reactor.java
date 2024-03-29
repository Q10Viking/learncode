package org.hzz.template;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class Reactor implements Runnable{
    protected static Logger logger = Logger.getLogger("Reactor");
    private final static int MAXIN = 65535;
    private final static int MAXOUT = 65535;
    final int port;
    final Selector selector;
    final ServerSocketChannel serverSocketChannel;

    public Reactor(int port) throws IOException {
        this.port = port;
        this.selector = Selector.open();
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        SelectionKey key = serverSocketChannel.register(selector, 0);
        key.interestOps(SelectionKey.OP_ACCEPT);
        key.attach(new Acceptor());
        logger.info("Reactor started on port "+port);
    }
    @Override
    public void run() {     // normally in a new Thread
        try {
            while (!Thread.interrupted()) {
                selector.select();
                // ... deal with selected keys ...
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                if(selectionKeys.isEmpty()){
                    logger.info(Thread.currentThread().getName()+": selectionKeys is empty");
                    continue;
                }
                logger.info(Thread.currentThread().getName()+": selectionKeys size is "+selectionKeys.size());
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while(iterator.hasNext()){
                    dispatch(iterator.next());
                    //iterator.remove();
                }
                selectionKeys.clear();

            }
        } catch (IOException ex) { /* ... */ }

    }

    void dispatch(SelectionKey key){
        Runnable r = (Runnable) key.attachment(); // Accept or Handler 都实现了Runnable接口
        if(r != null){
            r.run();
        }
    }

    class Acceptor implements Runnable{
        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverSocketChannel.accept();
                if(socketChannel != null){
                    logger.info("accept a new connection"+socketChannel.getRemoteAddress());
                    new Handler(selector, socketChannel);
                }
            } catch (IOException e) {/*...*/}
        }
    }

    final class Handler implements Runnable{
        static final int READING = 0, SENDING = 1;
        int state = READING;
        final SocketChannel socketChannel;
        final SelectionKey selectionKey;
        ByteBuffer input = ByteBuffer.allocate(MAXIN);
        ByteBuffer output = ByteBuffer.allocate(MAXOUT);

        Handler(Selector selector, SocketChannel socketChannel) throws IOException {
            this.socketChannel = socketChannel;
            socketChannel.configureBlocking(false);
            selectionKey = socketChannel.register(selector, 0);
            selectionKey.attach(this);
            selectionKey.interestOps(SelectionKey.OP_READ);
            selector.wakeup();
        }
        @Override
        public void run() {
            try {
                if (state == READING) read();
                else if (state == SENDING) send();
            } catch (IOException ex) { /* ... */ }
        }

        void read() throws IOException {
            // ... read data ...
            socketChannel.read(input);
            if(isInputComplete()){
                process();
                state = SENDING;
                selectionKey.interestOps(SelectionKey.OP_WRITE);
            }
        }

        void send() throws IOException {
            // ... send data ...
            output.put("hello client".getBytes(StandardCharsets.UTF_8));
            output.flip();
            socketChannel.write(output);
            state = READING;
            selectionKey.interestOps(SelectionKey.OP_READ);
            if(isOutputComplete()){
                logger.info(Thread.currentThread().getName()+"关闭"+socketChannel.getRemoteAddress());
                selectionKey.cancel();
                socketChannel.close();
            }
        }

        boolean isInputComplete(){
            return true;
        }
        boolean isOutputComplete(){
            return true;
        }
        void process() throws IOException {
            // ... process data ...
            if(input.position() > 0){
                input.flip();
                byte[] bytes = new byte[input.limit()];
                input.get(bytes);
                logger.info(Thread.currentThread().getName()+": read data is "+new String(bytes, StandardCharsets.UTF_8));
                input.clear();
            }
        }
    }
}
