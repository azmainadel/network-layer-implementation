/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.ArrayList;
import java.util.Random;

import static java.lang.Thread.sleep;

/**
 *
 * @author samsung
 */
public class Router {
    private int routerId;
    private int numberOfInterfaces;
    private ArrayList<IPAddress> interfaceAddrs;//list of IP address of all interfaces of the router
    private ArrayList<RoutingTableEntry> routingTable;//used to implement DVR
    private ArrayList<Integer> neighborRouterIds;//Contains both "UP" and "DOWN" state routers
    private Boolean state;//true represents "UP" state and false is for "DOWN" state
    private Boolean changeOccured;//true if changes happen in routing table, false if unchanged

    public Router() {
        interfaceAddrs = new ArrayList<>();
        routingTable = new ArrayList<>();
        neighborRouterIds = new ArrayList<>();
        
        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p<=0.80) state = true;
        else state = false;
        
        numberOfInterfaces = 0;
        changeOccured = false;
    }
    
    public Router(int routerId, ArrayList<Integer> neighborRouters, ArrayList<IPAddress> interfaceAddrs)
    {
        this.routerId = routerId;
        this.interfaceAddrs = interfaceAddrs;
        this.neighborRouterIds = neighborRouters;
        routingTable = new ArrayList<>();
        
        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        if(p<=0.80) state = true;
        else state = false;
        
        numberOfInterfaces = this.interfaceAddrs.size();
        changeOccured = false;
    }

    @Override
    public String toString() {
        String temp = "";
        temp+="Router ID: "+routerId+"\n";
        temp+="Intefaces: \n";
        for(int i=0;i<numberOfInterfaces;i++)
        {
            temp+=interfaceAddrs.get(i).getString()+"\t";
        }
        temp+="\n";
        temp+="Neighbors: \n";
        for(int i=0;i<neighborRouterIds.size();i++)
        {
            temp+=neighborRouterIds.get(i)+"\t";
        }
        return temp;
    }


    public int getRouterId() {
        return routerId;
    }

    public void setRouterId(int routerId) {
        this.routerId = routerId;
    }

    public int getNumberOfInterfaces() {
        return numberOfInterfaces;
    }

    public void setNumberOfInterfaces(int numberOfInterfaces) {
        this.numberOfInterfaces = numberOfInterfaces;
    }

    public ArrayList<IPAddress> getInterfaceAddrs() {
        return interfaceAddrs;
    }

    public void setInterfaceAddrs(ArrayList<IPAddress> interfaceAddrs) {
        this.interfaceAddrs = interfaceAddrs;
    }

    public ArrayList<RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }

    public void setRoutingTable(ArrayList<RoutingTableEntry> routingTable) {
        this.routingTable = routingTable;
    }

    public ArrayList<Integer> getNeighborRouterIds() {
        return neighborRouterIds;
    }

    public void setNeighborRouterIds(ArrayList<Integer> neighborRouterIds) {
        this.neighborRouterIds = neighborRouterIds;
    }

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }

    public Boolean getChangeOccured() {
        return changeOccured;
    }

    public void setChangeOccured(Boolean changeOccured) {
        this.changeOccured = changeOccured;
    }

    /**
     * Initialize the distance(hop count) for each router.
     * for itself, distance=0; for any connected router with state=true, distance=1; otherwise distance=Constants.INFTY;
     */
    public void initiateRoutingTable() {

        this.clearRoutingTable();

        ArrayList<Router> routers = NetworkLayerServer.routers;

        for(Router router:routers){

            RoutingTableEntry routingTableEntry = null;

            if(router.getRouterId() == this.getRouterId()){
                routingTableEntry = new RoutingTableEntry(this.getRouterId(), 0, routerId);
            }

            else{
                for(Integer neighbor:neighborRouterIds){

                    if(neighbor.intValue() == router.getRouterId()){
                        routingTableEntry = new RoutingTableEntry(router.getRouterId(), 1, router.getRouterId());
                    }

                    else{
                        routingTableEntry = new RoutingTableEntry(router.getRouterId(), Constants.INFTY, -1);
                    }

                }
            }

            routingTable.add(routingTableEntry);
        }

    }

    /**
     * Delete all the routingTableEntry
     */
    public void clearRoutingTable() {

        routingTable = new ArrayList<>();

//        routingTable.clear();
    }

    /**
     * Update the routing table for this router using the entries of Router neighbor
     * @param neighbor
     */
    public void updateRoutingTable(Router neighbor) throws InterruptedException {

        if(neighbor.getRoutingTable().isEmpty()){

            for(RoutingTableEntry routingTableEntry:getRoutingTable()){

                if(routingTableEntry.getGatewayRouterId() == neighbor.getRouterId() &&
                        routingTableEntry.getDistance() != Constants.INFTY){

                    routingTableEntry.setDistance(Constants.INFTY);
                    changeOccured = true; //isUpdated
                }
            }
        }

        for(int i = 0; i < this.getRoutingTable().size(); i++){

            RoutingTableEntry routingTableEntry = this.getRoutingTable().get(i);
            RoutingTableEntry neighborRoutingTableEntry;

            if(neighbor.getRoutingTable().size() != 0){
                neighborRoutingTableEntry = neighbor.getRoutingTable().get(i);
            }
            else continue;

            double distance = neighborRoutingTableEntry.getDistance() + 1;
            
            if(distance > Constants.INFTY) distance = Constants.INFTY;

            if(routingTableEntry.getGatewayRouterId() == neighbor.routerId
                    || (distance < routingTableEntry.getDistance()
                    && this.routerId != neighborRoutingTableEntry.getGatewayRouterId())) {

                if(distance != routingTableEntry.getDistance()) {

                    changeOccured = true;

                    routingTableEntry.setDistance(distance);
                    routingTableEntry.setGatewayRouterId(neighbor.getRouterId());

                }
            }
        }

    }


    public void simpleUpdateRoutingTable(Router neighbor){

        if(neighbor.getRoutingTable().isEmpty()){

            for(RoutingTableEntry routingTableEntry:getRoutingTable()){

                if(routingTableEntry.getGatewayRouterId() == neighbor.getRouterId()){

                    routingTableEntry.setDistance(Constants.INFTY);
                    changeOccured = true;
                }
            }
        }

        for(int i = 0; i < this.getRoutingTable().size(); i++) {

            RoutingTableEntry routingTableEntry = this.getRoutingTable().get(i);
            RoutingTableEntry neighborRoutingTableEntry = neighbor.getRoutingTable().get(i);

            double distance = Math.min(Constants.INFTY, neighborRoutingTableEntry.getDistance() + 1);

            if (distance > Constants.INFTY) distance = Constants.INFTY;

            if (distance < routingTableEntry.getDistance()) {
                changeOccured = true;

                routingTableEntry.setDistance(distance);
                routingTableEntry.setGatewayRouterId(neighbor.getRouterId());

            }
        }
    }

//    void printRoutingTable(ArrayList<RoutingTableEntry> routingTableEntries){
//
//        for(RoutingTableEntry routingTableEntry: routingTableEntries){
//            System.out.println("RouterID: " + routingTableEntry.getRouterId() +
//                    " | Distance: "+routingTableEntry.getDistance() +
//                    " | Gateway: " + routingTableEntry.getGatewayRouterId());
//        }
//
//    }

//    Router matchIP(ArrayList<Router> routers, IPAddress matchIPAddress){
//
//        Router router = null;
//
//        for(int i = 0; i < routers.size(); i++){
//            IPAddress ipAddress = routers.get(i).getInterfaceAddrs().get(0);
//
//            Short[] bytes = ipAddress.getBytes();
//            Short[] sourceBytes = matchIPAddress.getBytes();
//
//            if(bytes[0].equals(sourceBytes[0]) && bytes[1].equals(sourceBytes[1]) && bytes[2].equals(sourceBytes[2])){
//                router = NetworkLayerServer.routers.get(i);
//                break;
//            }
//
//        }
//        return router;
//    }


    /**
     * If the state was up, down it; if state was down, up it
     */
    public void revertState()
    {
        state=!state;

        if(state==true) this.initiateRoutingTable();
        else this.clearRoutingTable();
    }

    
}
