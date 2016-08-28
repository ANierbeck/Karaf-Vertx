/*
   Copyright 2016 Achim Nierbeck

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package de.nierbeck.example.vertx.shell;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

@Command(scope = "vertx", name = "bus-tail", description = "Subscribe to one or several event bus address and logs received messages on the console")
@Service
public class VertxBusTail extends AbstractVertxCommand {
    
    @Reference
    Session session;

    @Argument(multiValued = true, name = "adress", required = true, description = "Adress of EventBus")
    private List<String> addresses;

    @Option(name = "verbose", description = "Verbose output")
    private boolean verbose;

    @Option(name = "local", description = "subscribe to local address")
    private boolean local;
    
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public Object execute() throws Exception {
        
        if (addresses == null || addresses.isEmpty()) {
            return "provide at lease one address";
        }
        
        PrintEventThread printThread = new PrintEventThread();
        ReadKeyBoardThread readKeyboardThread = new ReadKeyBoardThread(Thread.currentThread());
        executorService.execute(printThread);
        executorService.execute(readKeyboardThread);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(200);
            } catch (java.lang.InterruptedException e) {
                break;
            }
        }
        printThread.abort();
        readKeyboardThread.abort();
        executorService.shutdownNow();  
        return null;      
        
    }
    
    class ReadKeyBoardThread implements Runnable {
        private Thread sessionThread;
        boolean readKeyboard = true;
        public ReadKeyBoardThread(Thread thread) {
            this.sessionThread = thread;
        }

        public void abort() {
            readKeyboard = false;            
        }

        public void run() {
            while (readKeyboard) {
                try {
                    int c = session.getKeyboard().read();
                    if (c < 0) {
                        sessionThread.interrupt();
                        break;
                    }
                } catch (IOException e) {
                    break;
                }
                
            }
        }
    } 
    
    class PrintEventThread implements Runnable {

        PrintStream out = System.out;
        boolean doDisplay = true;

        public void run() {
            EventBus eb = getEventBusService();
            addresses.stream().map(address -> {
                Handler<Message<Object>> consumer = msg -> {
                    Object body = msg.body();
                    String bodyString = null;
                    if (body instanceof Buffer) {
                        bodyString = DatatypeConverter.printHexBinary(((Buffer) body).getBytes());
                    } else {
                        bodyString = String.valueOf(body);
                    }
                    if (doDisplay) {
                        if (verbose) {
                            out.println(address + ":");
                            out.println("Reply address: "+msg.replyAddress());
                            MultiMap headers = msg.headers();
                            for (String header : headers.names()) {
                                out.println("Header "+header+ ":"+ headers.getAll(header));
                            }
                            out.println(bodyString);
                        } else {
                            out.println(address+":"+bodyString);
                        }
                    }
                };
                return local ? eb.localConsumer(address, consumer) : eb.consumer(address, consumer);
            }).collect(Collectors.toList());
            out.println();
        }

        public void abort() {
            doDisplay = false;
        }

    }

}
