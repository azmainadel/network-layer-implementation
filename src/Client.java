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
public class Client {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Socket socket;
        ObjectInputStream input = null;
        ObjectOutputStream output = null;
        
        try {
            socket = new Socket("localhost", 1234);
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("Connected to server");

        /**
         * Tasks
         */
        /*
        1. Receive EndDevice configuration from server
        2. Receive active client list from server        
        3. for(int i=0;i<100;i++)
        4. {
        5.      Generate a random message
        6.      Assign a random receiver from active client list
        7.      if(i==20)
        8.      {
        9.            Send the message and recipient IP address to server and a special request "SHOW_ROUTE"
        10.           Display routing path, hop count and routing table of each router [You need to receive 
                            all the required info from the server in response to "SHOW_ROUTE" request]
        11.     }
        12.     else
        13.     {
        14.           Simply send the message and recipient IP address to server.   
        15.     }
        16.     If server can successfully send the message, client will get an acknowledgement along with hop count
                    Otherwise, client will get a failure message [dropped packet]
        17. }
        18. Report average number of hops and drop rate
        */

        EndDevice endDevice = (EndDevice) input.readObject();
        ArrayList<IPAddress> clients = (ArrayList<IPAddress>) input.readObject();

        if(clients.size() == 1){
            System.out.println("No receiver found");
            return;
        }

        double totalHop = 0;
        double dropped = 0;
        double averageHopCount = 0;
        double averageDropRate = 0;
        int successfulMessages = 0;

        for(int i = 0; i < 100; i++){

            String message = "Message" + i;
            IPAddress receiver;
            int random = (int) (Math.random() % clients.size());

            if(clients.get(random) != endDevice.getIp()){
                receiver = clients.get(random);
            }
            else{
                random++;
                receiver = clients.get(random);
            }


            if(i == 20){
                output.writeObject(message);
                output.writeObject("SHOW_ROUTE");
                output.writeObject(receiver);

                double hop = (double) input.readObject();

                if(hop == -1){
                    dropped++;
                    System.out.println("[Error in sending packet]");

                    ArrayList<RoutingTableEntry> routingTableEntries = (ArrayList<RoutingTableEntry>) input.readObject();

                    for(RoutingTableEntry routingTableEntry: routingTableEntries){
                        System.out.println("RouterID: " + routingTableEntry.getRouterId() +
                                " | Distance: "+routingTableEntry.getDistance() +
                                " | Gateway: " + routingTableEntry.getGatewayRouterId());
                    }
                }
                else{
                    totalHop += hop;
                    successfulMessages++;
                    System.out.println("[Packet sent]");

                    String path = (String) input.readObject();
                    System.out.println("Full Path: " + path);

                    ArrayList<RoutingTableEntry> routingTableEntries = (ArrayList<RoutingTableEntry>) input.readObject();

                    for(RoutingTableEntry routingTableEntry: routingTableEntries){
                        System.out.println("RouterID: " + routingTableEntry.getRouterId() +
                                " | Distance: "+routingTableEntry.getDistance() +
                                " | Gateway: " + routingTableEntry.getGatewayRouterId());
                    }


                }

            }
            else {
                output.writeObject(message);
                output.writeObject(receiver);

                double hop = (double) input.readObject();

                if(hop == -1){
                    dropped++;
                    System.out.println("[Error in sending packet]");
                }
                else {
                    totalHop += hop;
                    successfulMessages++;
                    System.out.println("[Packet sent]");
                }
            }
        }
//        output.writeObject("");
        String string = "Acknowledged";

        if(string.equals((String) input.readObject())){
            System.out.println("[Acknowledgement received]");
        }

        averageHopCount = totalHop / successfulMessages;
        averageDropRate = dropped / 100;

        System.out.println("[Average Hop Count:] " + averageHopCount + " | [Average Drop Rate:] " + averageDropRate);

    }
}
