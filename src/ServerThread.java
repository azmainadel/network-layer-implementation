/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author samsung
 */
public class ServerThread implements Runnable {
    private Thread t;
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private EndDevice endDevice;
    private String path = "";
    
    public ServerThread(Socket socket){
        
        this.socket = socket;
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Server Ready for client "+NetworkLayerServer.clientCount);
        NetworkLayerServer.clientCount++;
        
        t=new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        /**
         * Synchronize actions with client.
         */
        /*
        Tasks:
        1. Upon receiving a packet and recipient, call deliverPacket(packet)
        2. If the packet contains "SHOW_ROUTE" request, then fetch the required information
                and send back to client
        3. Either send acknowledgement with number of hops or send failure message back to client
        */

        try {
            output.writeObject(endDevice);
            output.writeObject(NetworkLayerServer.activeClients);

            if (NetworkLayerServer.activeClients.size() == 1) {
                System.out.println("[Only one client active]");
                return;
            }

            while(true){
                String message = (String) input.readObject();
                if(message.isEmpty()){
                    System.out.println("[No messages received]");
                    return;
                }

                Object object = input.readObject();

                if(object.getClass() == IPAddress.class){
                    System.out.println("[No special message. IP received]");

                    IPAddress receiverIPAddress = (IPAddress) object;
                    Packet packet = new Packet(message, "", getEndDevice().getIp(), receiverIPAddress );

                    double hop = deliverPacket(packet);
                    output.writeObject(hop);
                }

                else if(object.getClass() == String.class){
                    System.out.println("[Special message received]");

                    String specialMessage = (String) object;
                    IPAddress receiverIPAddress = (IPAddress) input.readObject();

                    Packet packet = new Packet(message, specialMessage, getEndDevice().getIp(), receiverIPAddress);

                    double hop = deliverPacket(packet);
//                    System.out.println("CHECK");

                    if(hop == -1){
                        output.writeObject(hop);

                        for(Router router:NetworkLayerServer.routers){
                            output.writeObject(router.getRoutingTable());
                        }
                        output.writeObject("Acknowledged");
                    }
                    else{
                        output.writeObject(hop);
                        output.writeObject(path);

                        for(Router router:NetworkLayerServer.routers){
                            output.writeObject(router.getRoutingTable());
                        }

                        output.writeObject("Acknowledged");
                    }

                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Returns true if successfully delivered
     * Returns false if packet is dropped
     * @param p
     * @return 
     */
    public double deliverPacket(Packet p)
    {
        /*
        1. Find the router s which has an interface
                such that the interface and source end device have same network address.
        2. Find the router d which has an interface
                such that the interface and destination end device have same network address.
        3. Implement forwarding, i.e., s forwards to its gateway router x considering d as the destination.
                similarly, x forwards to the next gateway router y considering d as the destination, 
                and eventually the packet reaches to destination router d.
                
            3(a) If, while forwarding, any gateway x, found from routingTable of router r is in down state[x.state==FALSE]
                    (i) Drop packet
                    (ii) Update the entry with distance Constants.INFTY
                    (iii) Block NetworkLayerServer.stateChanger.t
                    (iv) Apply DVR starting from router r.
                    (v) Resume NetworkLayerServer.stateChanger.t
                            
            3(b) If, while forwarding, a router x receives the packet from router y, 
                    but routingTableEntry shows Constants.INFTY distance from x to y,
                    (i) Update the entry with distance 1
                    (ii) Block NetworkLayerServer.stateChanger.t
                    (iii) Apply DVR starting from router x.
                    (iv) Resume NetworkLayerServer.stateChanger.t
                            
        4. If 3(a) occurs at any stage, packet will be dropped, 
            otherwise successfully sent to the destination router
        */

        Router source = null;
        Router destination = null;
        IPAddress sourceIP = p.getSourceIP();
        IPAddress destinationIP = p.getDestinationIP();
        ArrayList<Router> routers = NetworkLayerServer.routers;
        double hop = 0;


        for(int i = 0; i < routers.size(); i++){
            IPAddress ipAddress = routers.get(i).getInterfaceAddrs().get(0);
            Short[] bytes = ipAddress.getBytes();

            Short[] sourceBytes = sourceIP.getBytes();

            if(bytes[0].equals(sourceBytes[0]) && bytes[1].equals(sourceBytes[1]) && bytes[2].equals(sourceBytes[2])){
                source = routers.get(i);
            }

            Short[] destinationBytes = destinationIP.getBytes();

            if(bytes[0].equals(destinationBytes[0]) && bytes[1].equals(destinationBytes[1]) && bytes[2].equals(destinationBytes[2])){
                destination = routers.get(i);
            }

        }

        System.out.println("Source: " + source.getRouterId() + " | Destination: " + destination.getRouterId());

        path = String.valueOf(source.getRouterId()) + " -> ";


//        if(!source.getState()){
//
//            System.out.println("[Source DOWN]");
//            hop = -1;
//        }
//
//        if(source.getRouterId() == destination.getRouterId()){
//            System.out.println("[Destination reached]");
//            hop = 0;
//        }
//
//        ArrayList<RoutingTableEntry> sourceRoutingTable = source.getRoutingTable();
//
//        for(RoutingTableEntry routingTableEntry: sourceRoutingTable){
//            if(routingTableEntry.getRouterId() == destination.getRouterId()){
//
//                source = routingTableEntry.getGatewayRouterId();
//            }
//
//        }

        int sId = source.getRouterId();
        int dId = destination.getRouterId();

        while(sId != dId){

            ArrayList<RoutingTableEntry> sourceRoutingTable = source.getRoutingTable();

            int gatewayRouterId = sourceRoutingTable.get(dId - 1).getGatewayRouterId();

            if(!routers.get(gatewayRouterId).getState()){
                System.out.println("[Router DOWN]");
                hop = -1;
                break;
            }
            else hop++;

            sId = gatewayRouterId;
            path += String.valueOf(sId) + " -> ";
        }

        path += String.valueOf(dId);

        System.out.println("[Destination reached]");
        return hop;
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
    }

    public Thread getT() {
        return t;
    }

    public void setT(Thread t) {
        this.t = t;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public ObjectInputStream getInput() {
        return input;
    }

    public void setInput(ObjectInputStream input) {
        this.input = input;
    }

    public ObjectOutputStream getOutput() {
        return output;
    }

    public void setOutput(ObjectOutputStream output) {
        this.output = output;
    }

    public EndDevice getEndDevice() {
        return endDevice;
    }

    public void setEndDevice(EndDevice endDevice) {
        this.endDevice = endDevice;
    }
}
