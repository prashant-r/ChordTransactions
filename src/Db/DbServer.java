package Db;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import Transactions.TwoPhaseCommit;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import Paxos.PaxosInterface;

public class DbServer {
    final static Logger log = Logger.getLogger(DbServer.class);
    final static String PATTERN = "%d [%p|%c|%C{1}] %m%n";

    static void configureLogger() {
        ConsoleAppender console = new ConsoleAppender(); //create appender
        //configure the appender
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(Level.ALL);
        console.activateOptions();
        //add appender to any Logger (here is root)
        log.addAppender(console);

        // This is for the tcp_client log file
        FileAppender fa = new FileAppender();
        fa.setName("FileLogger");
        fa.setFile("log/dbserver.log");
        fa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
        fa.setThreshold(Level.ALL);
        fa.setAppend(true);
        fa.activateOptions();

        //add appender to any Logger (here is root)
        log.addAppender(fa);
        log.setAdditivity(false);
        //repeat with all other desired appenders
    }

    // Takes a port number to initialize the server
    public DbServer(String hostname, Integer portNumber, String transactionType) throws Exception {
        // create the registry

        DbServerInterface dbi = null;
        PaxosInterface pxi = null;

        if (transactionType.startsWith("two")) {
            TwoPhaseCommit rmiImpl;
            dbi = (rmiImpl = new TwoPhaseCommit(hostname, portNumber));
            pxi = rmiImpl.getPaxosHelper();
        } 

        LocateRegistry.createRegistry(portNumber);
        //bind the method to this name so the client can search for it
        String bindMeRMI = "rmi://" + hostname + ":" + portNumber + "/Calls";
        String bindMePaxos = "rmi://" + hostname + ":" + portNumber + "/Paxos";
        Naming.bind(bindMeRMI, dbi);
        Naming.bind(bindMePaxos, pxi);
        log.info("Paxos RMIServer " + hostname + "  started successfully");
    }

    public static void main(String args[]) {
        configureLogger();
        if (args.length != 3) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            args = new String[3];
            try {
                log.info("Enter hostname");
                args[0] = br.readLine();
                log.info("Enter port");
                args[1] = br.readLine();
                log.info("Enter transaction type");
                args[2] = br.readLine();
            } catch (Exception e) {
                log.fatal("Fatal error : Usage - hostname port#");
                System.exit(-1);
            }
        }
        Integer portNumber = 0;
        try {
            portNumber = Integer.parseInt(args[1]);
        } catch (Exception e) {
            log.fatal("Fatal error : Usage - port#, error caused due to " + e.getMessage());
            System.exit(-1);
        }
        try {
            new DbServer(args[0], portNumber, args[2]);
        } catch (Exception e) {
            log.error("RMI server binding failed with " + e.getMessage());
        }
    }

}
