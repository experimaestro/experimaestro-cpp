/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

package sf.net.experimaestro.server;

import org.mortbay.thread.ThreadPool;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.type.ArrayValue;
import org.msgpack.type.MapValue;
import org.msgpack.type.Value;
import org.msgpack.type.ValueType;
import org.msgpack.unpacker.Unpacker;
import org.msgpack.unpacker.UnpackerIterator;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * A TCP-IP service based on JSON-like data exchanges (through MessagePack)
 */
public class StreamServer extends Thread {
    static final private Logger LOGGER = Logger.getLogger();

    protected ServerSocket server = null;
    protected boolean isStopped = false;
    protected Thread runningThread = null;
    private final ThreadPool threadPool;
    private final File socketFile;
    private final StreamServerService service;

    public StreamServer(ThreadPool threadPool, File socketFile, Repository repository, Scheduler scheduler) {
        this.threadPool = threadPool;
        this.socketFile = socketFile;
        this.service = new StreamServerService(repository, scheduler);
    }

    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        try {
            openServerSocket();
        } catch (IOException e) {
            LOGGER.error(e, "Cannot open the stream server socket");
            return;
        }
        while (!isStopped()) {
            Socket clientSocket = null;
            try {
                clientSocket = this.server.accept();
            } catch (IOException e) {
                if (isStopped()) {
                    LOGGER.info("Server Stopped.");
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
            threadPool.dispatch(new Client(clientSocket));
        }
        System.out.println("Server Stopped.");
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    /**
     * Close the web server
     */
    public synchronized void close() {
        this.isStopped = true;
        try {
            this.server.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() throws IOException {
        this.server = new ServerSocket();
        server = AFUNIXServerSocket.newInstance();
        server.bind(new AFUNIXSocketAddress(socketFile));
    }

    /**
     * A Bson client
     */
    private class Client implements Runnable {
        private final Socket socket;

        public Client(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            LOGGER.info("Starting client");
            try (InputStream inputStream = socket.getInputStream();
                 OutputStream outputStream = socket.getOutputStream()) {

                MessagePack msgpack = new MessagePack();
                Unpacker unpacker = msgpack.createUnpacker(inputStream);
                Packer packer = msgpack.createPacker(outputStream);

                UnpackerIterator iterator = unpacker.iterator();
                while (iterator.hasNext()) {
                    try {
                        Value value = iterator.next();
                        ValueType type = value.getType();
                        switch (type) {
                            case MAP:
                                MapValue map = value.asMapValue();
                                String command = msgpack.convert(map.get(msgpack.unconvert("command")), String.class);
                                MapValue args = map.get(msgpack.unconvert("args")).asMapValue();
                                runCommand(msgpack, packer, unpacker, command, args);
                                break;
                            default:
                                sendReturnCode(packer, 1, "Expected a map");
                        }

                    } catch(Throwable t) {
                        t.printStackTrace(System.err);
                        sendReturnCode(packer, 1, t.toString());
                    }

                    packer.write(false);
                    packer.flush();
                }

                LOGGER.info("Ending client");
            } catch (IOException e) {
                LOGGER.error(e);
            }

        }

        private void runCommand(MessagePack msgpack, Packer packer, Unpacker unpacker, String command, MapValue args) throws IOException {
            switch (command) {
                case "run-javascript":
                    ArrayValue msgfiles = args.get(msgpack.unconvert("files")).asArrayValue();
                    ArrayList<StreamServerService.FilePointer> files = new ArrayList<>();
                    for(int i = 0; i < msgfiles.size(); i++) {
                        ArrayValue array = msgfiles.get(i).asArrayValue();
                        String path = msgpack.convert(array.get(0), String.class);
                        String content = array.size() > 1 ? msgpack.convert(array.get(1), String.class) : null;
                        files.add(new StreamServerService.FilePointer(path, content));
                    }
                    TreeMap<String, String> env = new TreeMap<>();
                    service.runJSScript(packer, unpacker, files, env);
                    break;
                default:
                    throw new ExperimaestroRuntimeException("Unknown command [%s]", command);
            }

        }
    }

    static void sendReturnCode(Packer packer, int code, String message) throws IOException {
        TreeMap<String, Object> answer = new TreeMap<>();
        answer.put("code", code);
        answer.put("message", message);
        packer.write(answer);
        packer.flush();
    }
}