package pt.ulisboa.tecnico.classes.classserver;

import java.util.logging.Logger;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.StudentClass;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CancelEnrollmentRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CancelEnrollmentResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CloseEnrollmentsRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.CloseEnrollmentsResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.ListClassResponse;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.OpenEnrollmentsRequest;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.OpenEnrollmentsResponse;
import static io.grpc.Status.INVALID_ARGUMENT;
import java.util.Arrays;


public class ProfessorServiceImpl extends ProfessorServiceGrpc.ProfessorServiceImplBase{

    // the school
    private School school;

    // debug mode (on/off)
    private boolean debugMode;

    private String serverQualifier;

    final Logger LOGGER = Logger.getLogger(ProfessorServiceImpl.class.getName());



    /**
     * Professor Service Impl Constructor
     * @param school (School)
     * @param debug (boolean)
     */
    public ProfessorServiceImpl(School school, boolean debug, String qualifier){
        setSchool(school);
        this.debugMode = debug;
        setQualifier(qualifier);
    }

    // getters and setters
    public void setQualifier(String qualifier){
        this.serverQualifier = qualifier;
    }

    public String getQualifier(){
        return this.serverQualifier;
    }

    public void setSchool(School school){
        this.school = school;
    }

    public School getSchool(){
        return this.school;
    }

    // service methods


    /**
     * enables enrollments in the school class
     *
     * @param request (OpenEnrollmentsRequest)
     * @param responseObserver (StreamObserver<OpenEnrollmentsResponse>)
     */
    @Override
    public void openEnrollments(OpenEnrollmentsRequest request, StreamObserver<OpenEnrollmentsResponse> responseObserver){
        if(debugMode){
            LOGGER.info("Professor Service: OpenEnrollmentsRequest received");
        }
        ResponseCode code = ResponseCode.OK;
        if(getQualifier().equals("S")){
            code = ResponseCode.WRITING_NOT_SUPPORTED;
        } else{
            code = ResponseCode.OK;
            SchoolClassState schoolClassState = school.getSchoolClass().getClassState();

            if(!school.isServerActive()){
                if(debugMode){
                    LOGGER.info("-> Server inactive");
                }
                code = ResponseCode.INACTIVE_SERVER;
            }
            //else if(schoolClassState.isEnrollmentOpen()){
            //    if(debugMode){
            //        LOGGER.info("-> Enrollments already open");
            //    }
            //    code = ResponseCode.ENROLLMENTS_ALREADY_OPENED;
            //}
            else{

                Integer capacity = request.getCapacity();
                if(capacity > 0){
                    schoolClassState.setClassCapacity(capacity);
                    schoolClassState.setEnrollmentState(true);

                    int serverId = school.getId();
                    school.setVectorClockByIndex(serverId, school.getVectorClockByIndex(serverId)+1);

                    if(debugMode){
                        LOGGER.info("OpenEnrollments OK");
                        LOGGER.info("VectorClock on server " + serverId + " updated to: " + Arrays.toString(school.getVectorClock()));
                    }
                } else{
                    if(debugMode){
                        LOGGER.info("-> OpenEnrollments NOK, capacity <= 0");
                    }
                    responseObserver.onError(INVALID_ARGUMENT.withDescription("Capacity must be an int > 0!").asRuntimeException());
                }
            }
        }
        

        OpenEnrollmentsResponse response = OpenEnrollmentsResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }



    /**
     * Close Enrollments, disables enrollments in the school class
     *
     * @param request (CloseEnrollmentsRequest)
     * @param responseObserver (StreamObserver<CloseEnrollmentsResponse>)
     */
    @Override
    public void closeEnrollments(CloseEnrollmentsRequest request, StreamObserver<CloseEnrollmentsResponse> responseObserver){
        if(debugMode){
            LOGGER.info("Professor Service: CloseEnrollmentsRequest received");
        }
        ResponseCode code = ResponseCode.OK;

        if(getQualifier().equals("S")){
            code = ResponseCode.WRITING_NOT_SUPPORTED;
        } else{
            SchoolClassState schoolClassState = school.getSchoolClass().getClassState();

            // enrollments closed
            if(!school.isServerActive()){
                if(debugMode){
                    LOGGER.info("-> Server inactive");
                }
                code = ResponseCode.INACTIVE_SERVER;
            }
            else if(!schoolClassState.isEnrollmentOpen()){
                if(debugMode){
                    LOGGER.info("-> Enrollments already closed");
                }
                code = ResponseCode.ENROLLMENTS_ALREADY_CLOSED;
            }
            // no problem
            else{
                schoolClassState.setEnrollmentState(false);
                
                int serverId = school.getId();
                school.setVectorClockByIndex(serverId, school.getVectorClockByIndex(serverId)+1);

                if(debugMode){
                    LOGGER.info("CloseEnrollments OK");
                    LOGGER.info("VectorClock on server " + serverId + " updated to: " + Arrays.toString(school.getVectorClock()));
                }
            }
        }

        

        CloseEnrollmentsResponse response = CloseEnrollmentsResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }



    /**
     * Shows the state of the school class
     *
     * @param request (ListClassRequest)
     * @param responseObserver (StreamObserver<ListClassResponse>)
     */
    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver){
        if(debugMode){
            LOGGER.info("Professor Service: ListClassRequest received");
        }

        ResponseCode code = ResponseCode.OK;
        SchoolClassState schoolClassState = school.getSchoolClass().getClassState();
        ClassState.Builder classState = ClassState.newBuilder();
        ListClassResponse response;

        if(!school.isServerActive()){
            if(debugMode){
                LOGGER.info("-> Server inactive");
            }
            code = ResponseCode.INACTIVE_SERVER;
            response = ListClassResponse.newBuilder().setCode(code).build();
        } else{
            if(debugMode){
                LOGGER.info("-> ListClass OK");
            }
            schoolClassState.updateClassStateEnrolledStudents(classState);
            schoolClassState.updateClassStateDiscardedStudents(classState);
            classState.setCapacity(schoolClassState.getClassCapacity())
                      .setOpenEnrollments(schoolClassState.isEnrollmentOpen());

            response = ListClassResponse.newBuilder().setCode(code).setClassState(classState).build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }



    /**
     * Cancels the enrollment of an enrolled student
     *
     * @param request (CancelEnrollmentRequest)
     * @param responseObserver (streamObserver<CancelEnrollmentResponse>)
     */
    @Override
    public void cancelEnrollment(CancelEnrollmentRequest request, StreamObserver<CancelEnrollmentResponse> responseObserver){
        if(debugMode){
            LOGGER.info("Professor Service: CancelEnrollmentRequest received");
        }

        SchoolClassState schoolClassState = school.getSchoolClass().getClassState();
        ResponseCode code = ResponseCode.OK;
        String studentId = request.getStudentId();
        CancelEnrollmentResponse response;

        // it doesn't check if there is no such student in the class
        if(!school.isServerActive()){
            if(debugMode){
                LOGGER.info("-> Server inactive");
            }
            code = ResponseCode.INACTIVE_SERVER;
        }
        else if(!schoolClassState.isStudentEnrolled(studentId)){
            if(debugMode){
                LOGGER.info("-> Student doesn't exist");
            }
            code = ResponseCode.NON_EXISTING_STUDENT;
        }
        else{
            
            StudentClass student = schoolClassState.getStudentAlreadyEnrolled(studentId);
            schoolClassState.removeStudentFromEnrolledlist(studentId);
            schoolClassState.addStudentToDiscardedList(student.getName(), studentId);

            int serverId = school.getId();
            school.setVectorClockByIndex(serverId, school.getVectorClockByIndex(serverId)+1);
            if(debugMode){
                LOGGER.info("CancelEnrollment OK");
                LOGGER.info("VectorClock on server " + serverId + " updated to: " + Arrays.toString(school.getVectorClock()));

            }

        }

        response = CancelEnrollmentResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
