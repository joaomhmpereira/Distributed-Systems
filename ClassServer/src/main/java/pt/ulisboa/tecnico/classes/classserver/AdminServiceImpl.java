package pt.ulisboa.tecnico.classes.classserver;

import java.util.logging.Logger;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.StudentClass;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipResponse;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipRequest;


public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase{

    // the school
    private School school;

    // debug mode (on/off)
    private boolean debugMode;

    private String serverQualifier;

    final Logger LOGGER = Logger.getLogger(AdminServiceImpl.class.getName());


    /**
     * Admin service impl constructor
     *
     * @param school (School)
     * @param debug (boolean)
     */
    public AdminServiceImpl(School school, boolean debug, String qualifier){
        setSchool(school);
        this.debugMode = debug;
        setServerQualifier(qualifier);
    }



    // getters and setters

    public void setSchool(School school){
        this.school = school;
    }

    public School getSchool(){
        return this.school;
    }

    public void setServerQualifier(String qualifier){
        this.serverQualifier = qualifier;
    }

    public String getServerQualifier(){
        return this.serverQualifier;
    }



    // service methods
    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver){
        ResponseCode code;
        ActivateResponse response;
        if(debugMode){
            LOGGER.info("Admin Service: ActivateRequest Received");
        }
        school.activateServer();

        code = ResponseCode.OK;
        response = ActivateResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver){
        ResponseCode code;
        DeactivateResponse response;
        if(!school.isServerActive()){
            if(debugMode){
                LOGGER.info("-> Server inactive");
            }
            code = ResponseCode.INACTIVE_SERVER;
            response = DeactivateResponse.newBuilder().setCode(code).build();
        }else{
            if(debugMode){
                LOGGER.info("Admin Service: DeactivateRequest Received");
            }
            school.deactivateServer();
            code = ResponseCode.OK;
            response = DeactivateResponse.newBuilder().setCode(code).build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Dump method service, similar to list, it shows the
     * class state:
     *
     * @param request (DumpRequest)
     * @param responseObserver (StreamObserver<DumpResponse>)
     */
    @Override
    public void dump(DumpRequest request, StreamObserver<DumpResponse> responseObserver){
        if(debugMode){
            LOGGER.info("Admin Service: DumpRequest received");
        }
        ResponseCode code;
        DumpResponse response;
        if(!school.isServerActive()){
            if(debugMode){
                LOGGER.info("-> Server inactive");
            }
            code = ResponseCode.INACTIVE_SERVER;
            response = DumpResponse.newBuilder().setCode(code).build();
        } else{
            if(debugMode){
                LOGGER.info("-> Dump OK");
            }
            SchoolClassState schoolClassState = school.getSchoolClass().getClassState();
            ClassState.Builder classState = ClassState.newBuilder();

            schoolClassState.updateClassStateEnrolledStudents(classState);
            schoolClassState.updateClassStateDiscardedStudents(classState);
            classState.setCapacity(schoolClassState.getClassCapacity())
                    .setOpenEnrollments(schoolClassState.isEnrollmentOpen());

            code = ResponseCode.OK;
            response = DumpResponse.newBuilder().setCode(code).setClassState(classState).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void activateGossip(ActivateGossipRequest request, StreamObserver<ActivateGossipResponse> responseObserver){
        LOGGER.info("Activate Gossip Request received");
        ResponseCode code = ResponseCode.OK;
        this.school.activateGossip();
        ActivateGossipResponse response = ActivateGossipResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deactivateGossip(DeactivateGossipRequest request, StreamObserver<DeactivateGossipResponse> responseObserver){
        LOGGER.info("Deactivate Gossip Request received");
        ResponseCode code = ResponseCode.OK;
        this.school.deactivateGossip();
        DeactivateGossipResponse response = DeactivateGossipResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver){
        LOGGER.info("Gossip Request received");
        if(getServerQualifier().equals("P")){
            LOGGER.info("Forcing Propagate State to Server S");
            NamingServerFrontend frontend = new NamingServerFrontend();
            LookupRequest req = LookupRequest.newBuilder().setServiceName("turmas").addServerQualifiers("S").build();
			LookupResponse resp = frontend.lookup(req);
            frontend.close();

			List<ClassesDefinitions.Server> servidoresS = resp.getServerList();
			if(servidoresS.size()>0) {
				ClassesDefinitions.Server s = servidoresS.get(0);
				ClassServerFrontend cs = new ClassServerFrontend(s.getHost(), s.getPort());
				try{
					PropagateStateResponse response = cs.propagateState(school.getSchoolClass().getClassState(), school.getVectorClock());
                    if(response.hasClassState()){
                        LOGGER.info("SchoolClassState updated");
                        SchoolClassState newSchoolClassState = updateClassState(response.getClassState());
                        school.getSchoolClass().setClassState(newSchoolClassState);
                        school.setVectorClockByIndex(0, response.getClockList().get(0));
                        school.setVectorClockByIndex(1, response.getClockList().get(1));
                        LOGGER.info("Vector clock updated: " + Arrays.toString(school.getVectorClock()));
                    }
				} finally {
					cs.close();
				}
			}
        } else {
            LOGGER.info("Forcing Propagate State to Server P");
            NamingServerFrontend frontend = new NamingServerFrontend();
            LookupRequest req = LookupRequest.newBuilder().setServiceName("turmas").addServerQualifiers("P").build();
			LookupResponse resp = frontend.lookup(req);
            frontend.close();

			List<ClassesDefinitions.Server> servidoresS = resp.getServerList();
			if(servidoresS.size()>0) {
				ClassesDefinitions.Server s = servidoresS.get(0);
				ClassServerFrontend cs = new ClassServerFrontend(s.getHost(), s.getPort());
				try{
					PropagateStateResponse response = cs.propagateState(school.getSchoolClass().getClassState(), school.getVectorClock());
                    if(response.hasClassState()){
                        LOGGER.info("SchoolClassState updated");
                        SchoolClassState newSchoolClassState = updateClassState(response.getClassState());
                        school.getSchoolClass().setClassState(newSchoolClassState);
                        school.setVectorClockByIndex(0, response.getClockList().get(0));
                        school.setVectorClockByIndex(1, response.getClockList().get(1));
                        LOGGER.info("Vector clock updated: " + Arrays.toString(school.getVectorClock()));
                    }
				} finally {
					cs.close();
				}
			}
        }

        ResponseCode code = ResponseCode.OK;
        GossipResponse response = GossipResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public SchoolClassState updateClassState(ClassState receivedClassState){
        SchoolClassState newSchoolClassState = new SchoolClassState();
        newSchoolClassState.setClassCapacity(receivedClassState.getCapacity());
        newSchoolClassState.setEnrollmentState(receivedClassState.getOpenEnrollments());
        ConcurrentHashMap<String, StudentClass> enrolled = new ConcurrentHashMap<String, StudentClass>();
        ConcurrentHashMap<String, StudentClass> discarded = new ConcurrentHashMap<String, StudentClass>();

        for(Student student: receivedClassState.getEnrolledList()){
            StudentClass newStudent = new StudentClass(student.getStudentName(), student.getStudentId());
            enrolled.put(student.getStudentId(), newStudent);
        }
        newSchoolClassState.setEnrolledStudents(enrolled);

        for(Student student: receivedClassState.getDiscardedList()){
          StudentClass newStudent = new StudentClass(student.getStudentName(), student.getStudentId());
          discarded.put(student.getStudentId(), newStudent);
        }
        newSchoolClassState.setDiscarded(discarded);
        return newSchoolClassState;
    }


}

