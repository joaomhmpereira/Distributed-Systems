package pt.ulisboa.tecnico.classes.classserver;

import java.util.logging.Logger;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollResponse;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassResponse;
import java.util.Arrays;


public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase{

    private School school;

    // debug mode (on/off)
    private boolean debugMode;

    private String serverQualifier;

    final Logger LOGGER = Logger.getLogger(StudentServiceImpl.class.getName());



    /**
     * StudentServiceImpl constructor
     *
     * @param school (School)
     * @param debug (boolean)
     */
    public StudentServiceImpl(School school, boolean debug, String qualifier){
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

    /**
     * show's the state of the class
     *
     * @param request (ListClassRequest)
     * @param responseObserver (StreamObserver<ListClassResponse>)
     */
    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver){
        if(this.debugMode){
            LOGGER.info("Student Service: ListClassRequest received");
        }

        ResponseCode code = ResponseCode.OK;
        SchoolClassState schoolClassState = school.getSchoolClass().getClassState();
        ClassState.Builder classState = ClassState.newBuilder();
        ListClassResponse response;

        if(!school.isServerActive()){
            if(debugMode){
                LOGGER.info("Server inactive");
            }
            code = ResponseCode.INACTIVE_SERVER;
            response = ListClassResponse.newBuilder().setCode(code).build();

        }else{
            if(debugMode){
                LOGGER.info("ListClass OK");
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
     * Enroll the student
     *
     * @param request (EnrollRequest)
     * @param responseObserver (StreamObserver<EnrollResponse>)
     */
    @Override
    public void enroll(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver){
        if(debugMode){
            LOGGER.info("Student Service: EnrollRequest received");
        }
        Student student = request.getStudent();
        String studentName = student.getStudentName();
        String studentId = student.getStudentId();
        ResponseCode code = ResponseCode.OK;
        SchoolClassState schoolClassState = school.getSchoolClass().getClassState();

        if(!school.isServerActive()){
            if(debugMode){
                LOGGER.info("Server inactive");
            }
            code = ResponseCode.INACTIVE_SERVER;
        }
        // enroll not open
        else if(!schoolClassState.isEnrollmentOpen()){
            if(debugMode){
                LOGGER.info("Enrollments closed");
            }
            code = ResponseCode.ENROLLMENTS_ALREADY_CLOSED;
        }
        // student already enrolled
        else if(schoolClassState.isStudentEnrolled(studentName, studentId)){
            if(debugMode){
                LOGGER.info("Student already enrolled");
            }
            code = ResponseCode.STUDENT_ALREADY_ENROLLED;
        }
        // class full
        else if(schoolClassState.isClassFull()){
            if(debugMode){
                LOGGER.info("Class full");
            }
            code = ResponseCode.FULL_CLASS;
        }
        // no problem
        else{
            if(schoolClassState.isStudentDiscardedList(studentName, studentId)){
                schoolClassState.removeStudentFromDiscardedList(studentId);
            }
            schoolClassState.addStudentToEnrolledList(studentName, studentId);
            int serverId = school.getId();
            school.setVectorClockByIndex(serverId, school.getVectorClockByIndex(serverId)+1);
            
            if(debugMode){
                LOGGER.info("Enroll OK");
                LOGGER.info("VectorClock on server " + serverId + " updated to: " + Arrays.toString(school.getVectorClock()));
            }
        }

        EnrollResponse response = EnrollResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
