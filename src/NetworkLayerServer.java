/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author CSE_BUET
 */
public class NetworkLayerServer {
    static int clientCount = 1;
    static ArrayList<Router> routers = new ArrayList<>();
    static RouterStateChanger stateChanger = null;
    /**
     * Each map entry represents number of client end devices connected to the interface
     */
    static Map<IPAddress,Integer> clientInterfaces = new HashMap<>();
    static ArrayList<IPAddress> activeClients = new ArrayList<>();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        /**
         * Task: Maintain an active client list
         */
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(1234);
        } catch (IOException ex) {
            Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Server Ready: "+serverSocket.getInetAddress().getHostAddress());
        
        System.out.println("Creating router topology");
        
        readTopology();
        printRouters();
        
        /**
         * Initialize routing tables for all routers
         */
        initRoutingTables();
        
        /**
         * Update routing table using distance vector routing until convergence
         */
        DVR(1);
        
        /**
         * Starts a new thread which turns on/off routers randomly depending on parameter Constants.LAMBDA
         */
        stateChanger = new RouterStateChanger();
        
        while(true){
            try {
                Socket clientSock = serverSocket.accept();
                System.out.println("Client attempted to connect");

                ServerThread serverThread = new ServerThread(clientSock);
                serverThread.setEndDevice(getClientDeviceSetup());

            } catch (IOException ex) {
                Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
    }
    
    public static void initRoutingTables()
    {
        for(int i=0;i<routers.size();i++)
        {
            if(routers.get(i).getState()) routers.get(i).initiateRoutingTable();
        }
    }


    static int getRouterIndex(int routerId){
        for (int i = 0; i < routers.size(); i++){
            if (routers.get(i).getRouterId() == routerId) {
                return i;
            }
        }
        return 0;
    }

    static int getStartingPosition(int startingRouterId){
        for (int i = 0; i < routers.size(); i++) {
            Router router = routers.get(i);
            if (router.getRouterId() == startingRouterId) {
                return i;
            }
        }
        return 0;
    }
    
    /**
     * Task: Implement Distance Vector Routing with Split Horizon and Forced Update
     */
    public static void DVR(int startingRouterId) throws InterruptedException {
        /**
         * pseudocode
         */
        /*
        while(convergence)
        {
            //convergence means no change in any routingTable before and after executing the following for loop
            for each router r <starting from the router with routerId = startingRouterId, in any order>
            {
                1. T <- getRoutingTable of the router r
                2. N <- find routers which are the active neighbors of the current router r
                3. Update routingTable of each router t in N using the 
                   routing table of r [Hint: Use t.updateRoutingTable(r)]
            }
        }
        */

        boolean convergence = false;
        int startingPosition = getStartingPosition(startingRouterId);

        while(!convergence) {
            convergence = true;

            for (int i = 0; i < startingPosition; i++) {
                ArrayList<Integer> neighbors = routers.get(i).getNeighborRouterIds();

                for (Integer neighbor : neighbors) {

                    Router destinationRouter = routers.get(getRouterIndex(neighbor));

                    if (destinationRouter.getState()) destinationRouter.updateRoutingTable(routers.get(i));
                    if (destinationRouter.getChangeOccured()) convergence = false;

                }

            }

            for (int i = startingPosition; i < routers.size(); i++) {
                ArrayList<Integer> neighbors = routers.get(i).getNeighborRouterIds();

                for (Integer neighbor : neighbors) {

                    Router destinationRouter = routers.get(getRouterIndex(neighbor));

                    if (destinationRouter.getState()) destinationRouter.updateRoutingTable(routers.get(i));
                    if (destinationRouter.getChangeOccured()) convergence = false;

                }
            }
        }
    }


    /**
     * Task: Implement Distance Vector Routing without Split Horizon and Forced Update
     */
    public static void simpleDVR(int startingRouterId) {

        boolean convergence = false;
        int startingPosition = getStartingPosition(startingRouterId);

        while(!convergence) {
            convergence = true;

            for (int i = 0; i < startingPosition; i++) {
                ArrayList<Integer> neighbors = routers.get(i).getNeighborRouterIds();

                for (Integer neighbor : neighbors) {

                    Router destinationRouter = routers.get(getRouterIndex(neighbor));

                    if (destinationRouter.getState()) destinationRouter.simpleUpdateRoutingTable(routers.get(i));
                    if (destinationRouter.getChangeOccured()) convergence = false;

                }

            }

            for (int i = startingPosition; i < routers.size(); i++) {
                ArrayList<Integer> neighbors = routers.get(i).getNeighborRouterIds();

                for (Integer neighbor : neighbors) {

                    Router destinationRouter = routers.get(getRouterIndex(neighbor));

                    if (destinationRouter.getState()) destinationRouter.simpleUpdateRoutingTable(routers.get(i));
                    if (destinationRouter.getChangeOccured()) convergence = false;

                }
            }
        }
        
    }
    
    
    public static EndDevice getClientDeviceSetup()
    {
        Random random = new Random();
        int r =Math.abs(random.nextInt(clientInterfaces.size()));
        
        System.out.println("Size: "+clientInterfaces.size()+"\n"+r);
        
        IPAddress ip=null;
        IPAddress gateway=null;
        
        int i=0;
        for (Map.Entry<IPAddress, Integer> entry : clientInterfaces.entrySet()) {
            IPAddress key = entry.getKey();
            Integer value = entry.getValue();
            if(i==r)
            {
                gateway = key;
                ip = new IPAddress(gateway.getBytes()[0]+"."+gateway.getBytes()[1]+"."+gateway.getBytes()[2]+"."+(value+2));

                activeClients.add(ip);

                value++;
                clientInterfaces.put(key, value);
                break;
            }
            i++;
        }
        
        EndDevice device = new EndDevice(ip, gateway);
        System.out.println("Device : "+ip+"::::"+gateway);
        return device;
    }
    
    public static void printRouters()
    {
        for(int i=0;i<routers.size();i++)
        {
            System.out.println("------------------\n"+routers.get(i));
        }
    }
    
    public static void readTopology()
    {
        Scanner inputFile = null;
        try {
            inputFile = new Scanner(new File("topology.txt"));
            //skip first 27 lines
            int skipLines = 27;
            for(int i=0;i<skipLines;i++)
            {
                inputFile.nextLine();
            }
            
            //start reading contents
            while(inputFile.hasNext())
            {
                inputFile.nextLine();
                int routerId;
                ArrayList<Integer> neighborRouters = new ArrayList<>();
                ArrayList<IPAddress> interfaceAddrs = new ArrayList<>();
                
                routerId = inputFile.nextInt();
                
                int count = inputFile.nextInt();
                for(int i=0;i<count;i++)
                {
                    neighborRouters.add(inputFile.nextInt());
                }
                count = inputFile.nextInt();
                inputFile.nextLine();
                
                for(int i=0;i<count;i++)
                {
                    String s = inputFile.nextLine();
                    //System.out.println(s);
                    IPAddress ip = new IPAddress(s);
                    interfaceAddrs.add(ip);
                    
                    /**
                     * First interface is always client interface
                     */
                    if(i==0)
                    {
                        //client interface is not connected to any end device yet
                        clientInterfaces.put(ip, 0);
                    }
                }
                Router router = new Router(routerId, neighborRouters, interfaceAddrs);
                routers.add(router);
            }
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NetworkLayerServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
