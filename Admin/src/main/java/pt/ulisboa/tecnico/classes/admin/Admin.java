package pt.ulisboa.tecnico.classes.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;

import io.grpc.Status;
// imports for grpc
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Server;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipRequest;


public class Admin {
    private static final String DUMP_CMD = "dump";
    private static final String ACT_CMD = "activate";
    private static final String DEACT_CMD = "deactivate";
    private static final String EXIT_CMD = "exit";
    private static final String serviceName = "turmas";
    private static final String GOSS_CMD = "gossip";
    private static final String DEACT_GOSS_CMD = "deactivateGossip";
    private static final String ACT_GOSS_CMD = "activateGossip";

    private static boolean debugMode = false;

	private static final Logger LOGGER = Logger.getLogger(Admin.class.getName());


    public static void main(String[] args) {
        // loop
        if(args.length == 1 && args[0].equals("-debug")){
            debugMode = true;
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.printf("%n> ");

            try {
                String line = scanner.nextLine(); // get the line from command prompt
                String[] tokens = line.split(" "); // get the tokens for lookup

                if (EXIT_CMD.equals(tokens[0]))
                    break;
                else if (DUMP_CMD.equals(tokens[0])) {
                    if(debugMode)
                        LOGGER.info("Dump operation called");
                        
                    dumpMethod();
                }
                else if (ACT_CMD.equals(tokens[0])) {
                    if(tokens.length < 2){
                        if(debugMode)
                            LOGGER.info("Activate operation called for P server");
                        activateMethod("P");
                    } else{
                        if(debugMode)
                            LOGGER.info("Activate operation called for" + tokens[1] + "server");
                        activateMethod(tokens[1]);
                    }
                }
                else if(DEACT_CMD.equals(tokens[0])) {
                    if(tokens.length < 2){
                        if(debugMode)
                            LOGGER.info("Deactivate operation called for P server");
                        deactivateMethod("P");
                    } else{
                        if(debugMode)
                            LOGGER.info("Deactivate operation called for" + tokens[1] + "server");
                        deactivateMethod(tokens[1]);
                    }
                }
                else if(GOSS_CMD.equals(tokens[0])) {
                    if(tokens.length < 2){
                        if(debugMode)
                            LOGGER.info("Gossip operation called for P server");
                        gossipMethod("P");
                    } else{
                        if(debugMode)
                            LOGGER.info("Gossip operation called for" + tokens[1] + "server");
                        gossipMethod(tokens[1]);
                    }
                }
                else if(ACT_GOSS_CMD.equals(tokens[0])) {
                    if(tokens.length < 2){
                        if(debugMode)
                            LOGGER.info("Activate Gossip operation called for P server");
                        activateGossipMethod("P");
                    } else{
                        if(debugMode)
                            LOGGER.info("Activate Gossip operation called for" + tokens[1] + "server");
                        activateGossipMethod(tokens[1]);
                    }
                }
                else if(DEACT_GOSS_CMD.equals(tokens[0])) {
                    if(tokens.length < 2){
                        if(debugMode)
                            LOGGER.info("Deactivate Gossip operation called for P server");
                        deactivateGossipMethod("P");
                    } else{
                        if(debugMode)
                            LOGGER.info("Deactivate Gossip operation called for" + tokens[1] + "server");
                        deactivateGossipMethod(tokens[1]);
                    }
                }
            } catch (StatusRuntimeException e) {
                System.out.println(e);
            }
        }
        scanner.close();
        System.exit(0);
    }

    /**
     * Dump method from grpc
     *
     * @param token (String)
     */
    public static void activateMethod(String token) {
        ArrayList<String> qualif = new ArrayList<String>();
        qualif.add(token);
        LookupResponse response = callLookupMethod(qualif, serviceName);
        Server choosenServer = chooseAvailableServer(response.getServerList());
        // if there is no available server
        if (choosenServer == null) {
            System.out.println("No server available");
            return;
        }
        AdminFrontend frontend = new AdminFrontend(choosenServer.getHost(), choosenServer.getPort());
        ActivateResponse responseAct = frontend.activate(ActivateRequest.getDefaultInstance());
        System.out.println(Stringify.format(responseAct.getCode()));
        frontend.close();
    }

    /**
     * Dump method from grpc
     *
     * @param token (String)
     */
    public static void deactivateMethod(String token) {
        ArrayList<String> qualif = new ArrayList<String>();
        qualif.add(token);
        LookupResponse response = callLookupMethod(qualif, serviceName);
        Server choosenServer = chooseAvailableServer(response.getServerList());
        // if there is no available server
        if (choosenServer == null) {
            System.out.println("No server available");
            return;
        }
        AdminFrontend frontend = new AdminFrontend(choosenServer.getHost(), choosenServer.getPort());
        DeactivateResponse responseDeact = frontend.deactivate(DeactivateRequest.getDefaultInstance());
        System.out.println(Stringify.format(responseDeact.getCode()));
        frontend.close();
    }

    /**
     * chooses a available server out of the list of available servers
     *
     * @param availableServers (List<Server>)
     * @return choosenServer (Server)
     */
    public static Server chooseAvailableServer(List<Server> availableServers){
        Server choosenServer = null;
        if(availableServers.size() > 1){
            choosenServer = availableServers.get(new Random().nextInt(availableServers.size()));
        } else{
            if(availableServers.size() == 0){
                return null;
            }else{
                choosenServer = availableServers.get(0);
            }
        }
        return choosenServer;
    }


    /**
     * Creates a frontend to communicate with the Naming Server
     * also, chooses a available server
     *
     * @param qualifierList (List<String>)
     * @param serviceName (String)
     * @return choosenServer (Server)
     */
    public static LookupResponse callLookupMethod(List<String> qualifierList, String serviceName){
        // creating the frontend
        AdminNamingServerFrontend frontend = new AdminNamingServerFrontend();
        LookupResponse response = null;
        try{
            // lookup command
            LookupRequest request = LookupRequest.newBuilder()
                    .setServiceName(serviceName)
                    .addAllServerQualifiers(qualifierList)
                    .build();
            response = frontend.lookup(request);
        } finally{
            frontend.close();
        }
        return response;
    }

    /**
     * Dump method from grpc
     *
     * @param token (String)
     */
    public static void dumpMethod(){
        // call lookup and choose a server
        LookupResponse response = callLookupMethod(new ArrayList<String>(), serviceName);
        
        if(response.getServerList().isEmpty()){
            System.out.println("No servers available.");
            return;
        }

        for(Server server: response.getServerList()){
            AdminFrontend frontend = new AdminFrontend(server.getHost(), server.getPort());
            try{
                DumpResponse responseList = frontend.dump(DumpRequest.getDefaultInstance());
                if(responseList.getCode().equals(ResponseCode.OK)){
                    System.out.println(Stringify.format(responseList.getClassState()));
                    return;
                } else if(responseList.getCode().equals(ResponseCode.INACTIVE_SERVER)){
                    continue;
                }
            } catch(StatusRuntimeException e){
                if(e.getStatus().equals(Status.UNAVAILABLE)){
                    continue;
                }
            } finally{
                frontend.close();
            }
        }
        System.out.println("No servers available.");
    }

    public static void activateGossipMethod(String token) {
        ArrayList<String> qualif = new ArrayList<String>();
        qualif.add(token);
        LookupResponse response = callLookupMethod(qualif, serviceName);
        Server choosenServer = chooseAvailableServer(response.getServerList());
        // if there is no available server
        if (choosenServer == null) {
            System.out.println("No server available");
            return;
        }
        AdminFrontend frontend = new AdminFrontend(choosenServer.getHost(), choosenServer.getPort());
        ActivateGossipResponse responseAct = frontend.activateGossip(ActivateGossipRequest.getDefaultInstance());
        System.out.println(Stringify.format(responseAct.getCode()));
        frontend.close();
    }

    public static void deactivateGossipMethod(String token) {
        ArrayList<String> qualif = new ArrayList<String>();
        qualif.add(token);
        LookupResponse response = callLookupMethod(qualif, serviceName);
        Server choosenServer = chooseAvailableServer(response.getServerList());
        // if there is no available server
        if (choosenServer == null) {
            System.out.println("No server available");
            return;
        }
        AdminFrontend frontend = new AdminFrontend(choosenServer.getHost(), choosenServer.getPort());
        DeactivateGossipResponse responseAct = frontend.deactivateGossip(DeactivateGossipRequest.getDefaultInstance());
        System.out.println(Stringify.format(responseAct.getCode()));
        frontend.close();
    }

    public static void gossipMethod(String token) {
        ArrayList<String> qualif = new ArrayList<String>();
        qualif.add(token);
        LookupResponse response = callLookupMethod(qualif, serviceName);
        Server choosenServer = chooseAvailableServer(response.getServerList());
        // if there is no available server
        if (choosenServer == null) {
            System.out.println("No server available");
            return;
        }
        AdminFrontend frontend = new AdminFrontend(choosenServer.getHost(), choosenServer.getPort());
        GossipResponse responseAct = frontend.gossip(GossipRequest.getDefaultInstance());
        System.out.println(Stringify.format(responseAct.getCode()));
        frontend.close();
    }
}
