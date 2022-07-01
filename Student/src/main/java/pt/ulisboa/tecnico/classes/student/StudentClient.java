package pt.ulisboa.tecnico.classes.student;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.Stringify;


// imports for grpc
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Server;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollResponse;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;


public class StudentClient {
    private static final String LIST_CMD = "list";
    private static final String ENROLL_CMD = "enroll";
    private static final String EXIT_CMD = "exit";
    private static final String serviceName = "turmas";

    public static void main(String[] args) {
        // check arguments size
        if (args.length < 3) {
            System.out.println("Missing arguments!");
            return;
        }

        // check if name is valid
        String studentName = "";
        for (int i = 1; i < args.length; i++) {
            studentName += args[i];
            if (i < args.length - 1) {
                studentName += " ";
            }
        }
        if (studentName.length() < 3 || studentName.length() > 30) {
            System.out.println("Name invalid, must have between 3 and 30 characters");
            return;
        }

        String studentID = "";

        // checks if the student is valid
        if (isStudentIdValid(args[0])) {
            studentID = args[0];
        } else {
            System.out.println("Invalid student id, must be of format: 'alunoXXXX'");
            return;
        }

        Student student = Student.newBuilder().setStudentId(studentID).setStudentName(studentName).build();

        // loop
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.printf("%n> ");

            String line = scanner.nextLine(); // get the line from command prompt
            String[] tokens = line.split(" "); // get the tokens for lookup

            // EXIT
            if (EXIT_CMD.equals(tokens[0])) {
                break;
            } else if (LIST_CMD.equals(line)) {
                listMethod();
            } else if (ENROLL_CMD.equals(line)) {
                enrollMethod(student);
            }
        }
        scanner.close();
        System.exit(0);
    }

    /**
     *  List method
     */
    public static void listMethod(){
        // call lookup and choose a server
        int flag = 0;
        LookupResponse response = callLookupMethod(new ArrayList<String>(), serviceName);
        Server server = null;
        if(response.getServerList().isEmpty()){
            System.out.println("No servers available.");
            return;
        } else if(response.getServerList().size() > 1){
            server = chooseAvailableServer(response.getServerList());
        } else{
            flag = 1;
            server = response.getServerList().get(0);
        }

        StudentFrontend frontend = new StudentFrontend(server.getHost(), server.getPort());
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
                    System.out.println("Retrying list request");
                    listMethod();
                }
            }
        } finally{
            frontend.close();
        }
    }

    /**
     *  List method
     */
    public static void enrollMethod(Student student){
        // call lookup and choose a server
        int flag = 0;
        ArrayList<String> qualif = new ArrayList<String>();
        LookupResponse response = callLookupMethod(qualif, serviceName);
        Server choosenServer = null;

        // if there is no available server
        if(response.getServerList().isEmpty()){
            System.out.println("No server available");
            return;
        } else if(response.getServerList().size() > 1){
            choosenServer = chooseAvailableServer(response.getServerList());
        } else{
            flag = 1;
            choosenServer = response.getServerList().get(0);
        }
        // communicate with class server
        // end client afterwards

        StudentFrontend frontend = new StudentFrontend(choosenServer.getHost(), choosenServer.getPort());
        try{
            EnrollResponse responseEnr = frontend.enroll(EnrollRequest.newBuilder().setStudent(student).build());
            System.out.println(Stringify.format(responseEnr.getCode()));
        } catch(StatusRuntimeException e){
            if(e.getStatus().equals(Status.UNAVAILABLE)){
                if(flag == 1){
                    System.out.println("Server is currently unavailable.");
                    return;
                } else{
                    System.out.println("Retrying enroll request");
                    enrollMethod(student);
                }
            }
        }finally{
            frontend.close();
        }
    }

    /**
     * chooses a available server out of the list of available servers
     *
     * @param availableServers (List<Server>)
     * @return choosenServer (Server)
     */
    public static Server chooseAvailableServer(List<Server> availableServers){
        int randomInt = new Random().nextInt(availableServers.size());
        System.out.println("Choosen server index: " + randomInt);    
        return availableServers.get(randomInt);
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
        StudentNamingServerFrontend frontend = new StudentNamingServerFrontend();
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
     * checks if the student if is valid
     *
     * @param studentId (String)
     * @return boolean
     */
    public static boolean isStudentIdValid(String studentId){
        try{
            if(studentId.length() == 9)
                Integer.parseInt(studentId.substring(5, 9));
            else
                return false;
        } catch(NumberFormatException e){
            return false;
        }
        return studentId.substring(0, 5).equals("aluno");
    }
}
