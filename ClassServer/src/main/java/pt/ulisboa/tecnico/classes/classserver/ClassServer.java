package pt.ulisboa.tecnico.classes.classserver;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.RegisterRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.RegisterResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.DeleteRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.StudentClass;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateResponse;

import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.List;


public class ClassServer {

	  // Server host port
	private static int port;

	  // the school
	  private static School school;

	  // server hostname
	  private static String host;

	  private static String qualifier;

	  // debug mode (on/off)
	  private static boolean debugMode = false;

	  final static Logger LOGGER = Logger.getLogger(ClassServer.class.getName());

	  public static synchronized void main(String[] args) throws IOException, InterruptedException {
		// checking arguments
		if(args.length < 3 || args.length > 4){
			  System.out.println("Invalid number of arguments. Must have 4 arguments");
		} else{
			  host = args[0];
			  port = Integer.parseInt(args[1]);
			  qualifier = args[2];
		}

		// Check for debug mode
		for(String arg: args){
			  if(arg.equals("-debug")){
				debugMode = true;
			}
		}

		String hostAndPort = String.format("%s:%s", host, port);

		ManagedChannel channel = ManagedChannelBuilder.forAddress(host, 5000).usePlaintext().build();

		NamingServerServiceGrpc.NamingServerServiceBlockingStub stub = NamingServerServiceGrpc.newBlockingStub(channel);

		// school creation
		school = new School();

		// Creating the services
		final BindableService studentImpl = new StudentServiceImpl(school, debugMode, qualifier);
		final BindableService professorImpl = new ProfessorServiceImpl(school, debugMode, qualifier);
		final BindableService adminImpl = new AdminServiceImpl(school, debugMode, qualifier);
		final BindableService serverSImpl = new ServerSImpl(school, debugMode, qualifier);


		Server server = ServerBuilder.forPort(port)
						.addService(studentImpl)
						.addService(professorImpl)
						.addService(adminImpl)
						.addService(serverSImpl)
						.build();
		// create a new server to listen on port and start server
		if(qualifier.equals("P") || qualifier.equals("S")){
			Runnable triggerPropagate = new Runnable(){
				public void run(){
					if(school.isGossipActive()){
						
						if(debugMode)
							LOGGER.info("Triggered Gossip");
						
							LookupRequest req = null;
						if(qualifier.equals("S")){
							req = LookupRequest.newBuilder().setServiceName("turmas").addServerQualifiers("P").build();
						}
						else if(qualifier.equals("P")){
							req = LookupRequest.newBuilder().setServiceName("turmas").addServerQualifiers("S").build();
						}
						LookupResponse resp = stub.lookup(req);
						List<ClassesDefinitions.Server> serversP = resp.getServerList();
						if(serversP.size()>0) {
							ClassesDefinitions.Server s = serversP.get(0);
							ClassServerFrontend cs = new ClassServerFrontend(s.getHost(), s.getPort());
							try{
								PropagateStateResponse response = cs.propagateState(school.getSchoolClass().getClassState(), school.getVectorClock());
								if(response.hasClassState()){
									
									SchoolClassState newSchoolClassState = updateClassState(response.getClassState());
									school.getSchoolClass().setClassState(newSchoolClassState);
									school.setVectorClockByIndex(0, response.getClockList().get(0));
									school.setVectorClockByIndex(1, response.getClockList().get(1));
									
									if(debugMode){
										LOGGER.info("SchoolClassState updated");
										LOGGER.info("Vector clock updated: " + Arrays.toString(school.getVectorClock()));
									}
								}
							} finally {
								cs.close();
							}
						}
					}
				}
			};
			ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
			if(qualifier.equals("P"))
				executor.scheduleAtFixedRate(triggerPropagate, 10, 15, TimeUnit.SECONDS);
			else
				executor.scheduleAtFixedRate(triggerPropagate, 10, 30, TimeUnit.SECONDS);

		} else { 
			System.out.println("No such server qualifier");
			return; 
		}

		// start the server
		server.start();

		RegisterRequest request = RegisterRequest.newBuilder().setHostAndPort(hostAndPort).addServerQualifiers(qualifier).setServiceName("turmas").build();
		RegisterResponse response = stub.register(request);

		//set server id
		school.setId(response.getServerId());


		if(debugMode){
			LOGGER.info("Server started");
			  LOGGER.info("Server Id set to: " +  school.getId());
		}

		//shutdownHook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if(debugMode){
					LOGGER.info("Deleting server from NamingServer List");
				}
				stub.delete(DeleteRequest.newBuilder().setHostAndPort(hostAndPort).setServiceName("turmas").build());
			}
		});

		// Wait until server is terminated.
		server.awaitTermination();
	}

	/**
	 * Given a ClassesDefinitions.ClassState return a SchoolClassState
	 * with the same information
	 * 
	 * @param receivedClassState
	 * @return newSchoolClassState
	 */
	public static SchoolClassState updateClassState(ClassState receivedClassState){
		SchoolClassState newSchoolClassState = new SchoolClassState();
		//set new classState's capacity and enrollment status
		newSchoolClassState.setClassCapacity(receivedClassState.getCapacity());
		newSchoolClassState.setEnrollmentState(receivedClassState.getOpenEnrollments());
		ConcurrentHashMap<String, StudentClass> enrolled = new ConcurrentHashMap<String, StudentClass>();
		ConcurrentHashMap<String, StudentClass> discarded = new ConcurrentHashMap<String, StudentClass>();
		
		//add every student in enrolled list to new enrolled ConcurrentHashMap
		for(Student student: receivedClassState.getEnrolledList()){
		  	StudentClass newStudent = new StudentClass(student.getStudentName(), student.getStudentId());
		  	enrolled.put(student.getStudentId(), newStudent);
		}

		newSchoolClassState.setEnrolledStudents(enrolled);
		//add every student in discarded list do new discarded ConcurrentHashMap
		for(Student student: receivedClassState.getDiscardedList()){
			StudentClass newStudent = new StudentClass(student.getStudentName(), student.getStudentId());
			discarded.put(student.getStudentId(), newStudent);
		}
		newSchoolClassState.setDiscarded(discarded);
		return newSchoolClassState;
	}
}
