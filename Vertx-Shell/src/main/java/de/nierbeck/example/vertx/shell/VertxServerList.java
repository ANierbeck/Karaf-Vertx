package de.nierbeck.example.vertx.shell;

import java.util.Map;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import io.vertx.core.http.impl.HttpServerImpl;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.impl.NetServerImpl;
import io.vertx.core.net.impl.ServerID;

@Command(scope = "vertx", name = "netlist", description = "Lists all running Servers")
@Service
public class VertxServerList extends AbstractVertxCommand {

    @Override
    public Object execute() throws Exception {

        ShellTable table = new ShellTable();

        table.column("HttpServer");
        table.column("NetServer");
        table.column("Host");
        table.column("Port");
        
        VertxInternal vertx = (VertxInternal) getVertxService();
        for (Map.Entry<ServerID, NetServerImpl> server : vertx.sharedNetServers().entrySet()) {
            table.addRow().addContent("", "X", server.getKey().host, server.getKey().port);
        }
        for (Map.Entry<ServerID, HttpServerImpl> server : vertx.sharedHttpServers().entrySet()) {
            table.addRow().addContent("X", "", server.getKey().host, server.getKey().port);
        }
        
        try {
            table.print(System.out);
        } catch (Throwable t)  {
            System.err.println("FAILED to write table");
        }
        
        return null;
    }

}
