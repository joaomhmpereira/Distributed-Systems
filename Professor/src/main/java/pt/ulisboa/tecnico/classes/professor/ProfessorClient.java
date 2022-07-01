package pt.ulisboa.tecnico.classes.professor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;

import pt.ulisboa.tecnico.classes.Stringify;
import io.grpc.Status;
// imports for grpc
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CancelEnrollmentRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CancelEnrollmentResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CloseEnrollmentsRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CloseEnrollmentsResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.OpenEnrollmentsRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.OpenEnrollmentsResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.ListClassResponse;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Server;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;


public class ProfessorClient {
    private static final String OPEN_CMD = "openEnrollments";
    private static final String CLOSE_CMD = "closeEnrollments";
    private static final String CANCEL_CMD = "cancelEnrollment";
    private static final String LIST_CMD = "list";
    private static final String EXIT_CMD = "exit";
    private static final String serviceName = "turmas";

    private static boolean debugMode = false;

	private static final Logger LOGGER = Logger.getLogger(ProfessorClient.class.getName());


    public static void main(String[] args) {
        
        if(args.length == 1 && args[0].equals("-debug")){
            debugMode = true;
        }

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.printf("%n> ");
            try {
                String line = scanner.nextLine(); // get the line from command prompt
                String[] tokens = line.split(" "); // get the tokens for lookup

                // EXIT
                if (EXIT_CMD.equals(tokens[0]))
                    break;
                else if (OPEN_CMD.equals(tokens[0])) {
                    if (tokens.length > 1) {

                        if(debugMode)
                            LOGGER.info("Open Enrollments operation called");

                        Integer capacity = Integer.parseInt(tokens[1]);
                        openEnrollmentsMethod(capacity);
                    
                    } else {
                        System.out.println("Need to specify class capacity.");
                    }
                } else if (CANCEL_CMD.equals(tokens[0])) {
                    if (tokens.length > 1) {

                        if(debugMode)
                            LOGGER.info("Cancel Enrollment operation called");

                        String studentId = tokens[1];
                        cancelEnrollmentMethod(studentId);
                    } else {
                        System.out.println("Need to specify studentId.");
                    }
                } else if (CLOSE_CMD.equals(tokens[0])) {
                    
                    if(debugMode)
                        LOGGER.info("Close Enrollments operation called");
                    closeEnrollmentsMethod();

                } else if (LIST_CMD.equals(tokens[0])){
                    if(debugMode)
                        LOGGER.info("List operation called");

                    listMethod();
                }
            }catch(StatusRuntimeException e) {
                System.out.println(e);
            }
        }
        scanner.close();
        System.exit(0);
    }

    /**
     * List method from grpc
     */
    public static void listMethod() {
        int flag = 0;
        LookupResponse response = callLookupMethod(new ArrayList<String>(), serviceName);
        Server server = null;
        if(response.getServerList().isEmpty()){
            System.out.println("No servers available.");
            return;
        } else if(response.getServerList().size() > 1){
            //randomly choose one server from the available list
            server = chooseAvailableServer(response.getServerList());
        } else{
            //only one server available
            flag = 1;
            server = response.getServerList().get(0);
        }
        
        ProfessorFrontend frontend = new ProfessorFrontend(server.getHost(), server.getPort());
        try{
            ListClassResponse responseList = frontend.listClass(ListClassRequest.getDefaultInstance());
            if(responseList.getCode().equals(ResponseCode.OK)){
                System.out.println(Stringify.format(responseList.getClassState()));
                return;
            } else if(responseList.getCode().equals(ResponseCode.INACTIVE_SERVER)){
                if(flag == 1){
                    System.out.println("No servers available.");
                    return;
                } else{
                    //if there are other servers available retry list operation
                    System.out.println("Retrying list request");
                    listMethod();
                }
            }
        } catch(StatusRuntimeException e){
            if(e.getStatus().equals(Status.UNAVAILABLE)){
                if(flag == 1){
                    System.out.println("No servers available.");
                    return;
                } else{
                    //if there are other servers available retry list operation
                    System.out.println("Retrying list request");
                    listMethod();
                }
            }
        } finally{
            frontend.close();
        }
    }

    /**
     * OpenEnrollments method from grpc
     *
     * @param capacity (Integer)
     */
    public static void openEnrollmentsMethod(Integer capacity){
        try{
            ArrayList<String> qualif = new ArrayList<String>();
            qualif.add("P");
            LookupResponse response = callLookupMethod(qualif, serviceName);
            Server choosenServer = chooseAvailableServer(response.getServerList());
            // if there is no available server
            if(choosenServer == null){
                System.out.println("No server available");
                return;
            }
            // communicate with class server
            // end client afterwards
            ProfessorFrontend frontend = new ProfessorFrontend(choosenServer.getHost(), choosenServer.getPort());
            OpenEnrollmentsResponse openResponse = frontend.openEnrollments(OpenEnrollmentsRequest.newBuilder().setCapacity(capacity).build());
            System.out.println(Stringify.format(openResponse.getCode()));
            frontend.close();
        } catch(StatusRuntimeException e){
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());
        } finally{
            
        }
    }


    /**
     * CancelEnrollment method from grpc
     *
     * @param frontend (AdminFrontend)
     */
    public static void cancelEnrollmentMethod(String studentId){
        ArrayList<String> qualif = new ArrayList<String>();
        qualif.add("P");
        LookupResponse response = callLookupMethod(qualif, serviceName);
        Server choosenServer = chooseAvailableServer(response.getServerList());
        // if there is no available server
        if(choosenServer == null){
            System.out.println("No server available");
            return;
        }
        ProfessorFrontend frontend = new ProfessorFrontend(choosenServer.getHost(), choosenServer.getPort());
        CancelEnrollmentResponse cancelResponse = frontend.cancelEnrollment(CancelEnrollmentRequest.newBuilder().setStudentId(studentId).build());
        frontend.close();
        System.out.println(Stringify.format(cancelResponse.getCode()));
    }

    /**
     * CloseEnrollments method from grpc
     *
     * @param frontend (AdminFrontend)
     */
    public static void closeEnrollmentsMethod(){
        ArrayList<String> qualif = new ArrayList<String>();
        qualif.add("P");
        LookupResponse response = callLookupMethod(qualif, serviceName);
        Server choosenServer = chooseAvailableServer(response.getServerList());
        // if there is no available server
        if(choosenServer == null){
            System.out.println("No server available");
            return;
        }
        ProfessorFrontend frontend = new ProfessorFrontend(choosenServer.getHost(), choosenServer.getPort());
        CloseEnrollmentsResponse closeResponse = frontend.closeEnrollments(CloseEnrollmentsRequest.getDefaultInstance());
        frontend.close();
        System.out.println(Stringify.format(closeResponse.getCode()));
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
        ProfessorNamingServerFrontend frontend = new ProfessorNamingServerFrontend();
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

}
