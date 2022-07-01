package pt.ulisboa.tecnico.classes.classserver;


import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateRequest;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateResponse;


public class ServerSImpl extends ClassServerServiceGrpc.ClassServerServiceImplBase{

    // the school
    private School school;

    // debug mode (on/off)
    private boolean debugMode;

    private String serverQualifier;

    final Logger LOGGER = Logger.getLogger(ServerSImpl.class.getName());


    /**
     * constructor
     *
     * @param school
     * @param debugMode
     */
    public ServerSImpl(School school, boolean debugMode, String qualifier){
        setSchool(school);
        setDebugMode(debugMode);
        setQualifier(qualifier);
    }


    // getters and setters

    public void setSchool(School school){ 
        this.school = school; 
    }

    public School getSchool(){ 
        return this.school; 
    }

    public void setQualifier(String qualifier){
        this.serverQualifier = qualifier;
    }

    public String getQualifier(){
        return this.serverQualifier;
    }

    public void setDebugMode(boolean debugMode){
        this.debugMode = debugMode;
    }


    /**
     * propagate State from a P server to a S server
     *
     * @param request (PropagateStateRequest)
     * @param responseObserver ()
     */
    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver){
        if(debugMode){
            if(getQualifier().equals("S"))
                LOGGER.info("Server S Service: PropagateStateRequest received");
            else
                LOGGER.info("Server P Service: PropagateStateRequest received");
        }
        
        PropagateStateResponse response = null;
        ResponseCode code = ResponseCode.OK;
        SchoolClassState schoolClassState = school.getSchoolClass().getClassState();


        // server inactive
        if(!school.isServerActive()){
            if(debugMode){
                LOGGER.info("-> Server inactive");
            }
            code = ResponseCode.INACTIVE_SERVER;
            response = PropagateStateResponse.newBuilder().setCode(code).build();

        }
        else{
            List<Integer> receivedClock = request.getClockList();
            int[] myClock = school.getVectorClock();
            if(debugMode)
                LOGGER.info("Received clock: " + receivedClock.toString() + " This server's clock: " + Arrays.toString(myClock));
            
            //Check if incoming request is outdated or not
            if(serverQualifier.equals("S")){
                if(receivedClock.get(0) > myClock[0] && receivedClock.get(1) == myClock[1]){
                    
                    //our ClassState is outdated, update
                    ClassState classState = request.getClassState();

                    synchronized(this){
                        schoolClassState.setClassCapacity(classState.getCapacity());
                        schoolClassState.setEnrollmentState(classState.getOpenEnrollments());
                        schoolClassState.changeEnrolledList(classState.getEnrolledList());
                        schoolClassState.changeDiscardedList(classState.getDiscardedList());
                        //update our clock
                        school.setVectorClockByIndex(0, receivedClock.get(0));
                    }
                    
                    
                    if(debugMode){
                        LOGGER.info("Server S ClassState was outdated: Updating ClassState");
                        LOGGER.info("New clock values for server S: " + Arrays.toString(school.getVectorClock()));
                    }
                    response = PropagateStateResponse.newBuilder().setCode(code).build();

                } else if(receivedClock.get(0) > myClock[0] && receivedClock.get(1) < myClock[1]){
                    //merge both classStates
                    SchoolClassState updatedClassState = this.school.mergeClassStates(request.getClassState());
                    
                    //build classState that is going to be sent to the other server
                    ClassState.Builder classState = ClassState.newBuilder();
                    updatedClassState.updateClassStateEnrolledStudents(classState);
                    updatedClassState.updateClassStateDiscardedStudents(classState);
                    classState.setCapacity(schoolClassState.getClassCapacity())
                              .setOpenEnrollments(schoolClassState.isEnrollmentOpen());

                    //updating out clock
                    this.school.setVectorClockByIndex(0, Math.max(receivedClock.get(0), myClock[0]));
                    this.school.setVectorClockByIndex(1, Math.max(receivedClock.get(1), myClock[1]));
                    if(debugMode){
                        LOGGER.info("Conflict between clocks, merging both ClassStates");
                        LOGGER.info("New clock values for server S: " + Arrays.toString(school.getVectorClock()));
                    }
                    List<Integer> clockList = Arrays.stream(school.getVectorClock()).boxed().collect(Collectors.toList());
                    
                    //send both the new classState and clock to the other server
                    response = PropagateStateResponse.newBuilder().setCode(code)
                                                     .setClassState(classState)
                                                     .addAllClock(clockList)
                                                     .build();
                    
                } else{
                    response = PropagateStateResponse.newBuilder().setCode(code).build();
                    LOGGER.info("Received is outdated, no update");
                }
            } else {
                if(receivedClock.get(1) > myClock[1] && receivedClock.get(0) == myClock[0]){
                    //our server is outdated, update
                    LOGGER.info("Server P Service: ClassState updated");
                    ClassState classState = request.getClassState();
                    schoolClassState.setClassCapacity(classState.getCapacity());
                    schoolClassState.setEnrollmentState(classState.getOpenEnrollments());
                    schoolClassState.changeEnrolledList(classState.getEnrolledList());
                    schoolClassState.changeDiscardedList(classState.getDiscardedList());

                    //update our clock
                    school.setVectorClockByIndex(1, receivedClock.get(1));
                    LOGGER.info("New clock values for server P: " + Arrays.toString(school.getVectorClock()));
                    response = PropagateStateResponse.newBuilder().setCode(code).build();
                    
                } else if(receivedClock.get(1) > myClock[1] && receivedClock.get(0) < myClock[0]){
                    LOGGER.info("Can't decide who's more up to date. I'm in P");
                    SchoolClassState updatedClassState = this.school.mergeClassStates(request.getClassState());
                    ClassState.Builder classState = ClassState.newBuilder();

                    updatedClassState.updateClassStateEnrolledStudents(classState);
                    updatedClassState.updateClassStateDiscardedStudents(classState);
                    classState.setCapacity(schoolClassState.getClassCapacity())
                              .setOpenEnrollments(schoolClassState.isEnrollmentOpen());
                    
                    this.school.setVectorClockByIndex(0, Math.max(receivedClock.get(0), myClock[0]));
                    this.school.setVectorClockByIndex(1, Math.max(receivedClock.get(1), myClock[1]));
                    LOGGER.info("New clock values for server P: " + Arrays.toString(school.getVectorClock()));
                    List<Integer> clockList = Arrays.stream(school.getVectorClock()).boxed().collect(Collectors.toList());
                
                    response = PropagateStateResponse.newBuilder().setCode(code)
                                                     .setClassState(classState)
                                                     .addAllClock(clockList)
                                                     .build();
                                                                            
                } else{
                    response = PropagateStateResponse.newBuilder().setCode(code).build();
                    LOGGER.info("Received is outdated, no update");
                }
            }
        }
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}