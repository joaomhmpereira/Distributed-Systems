package pt.ulisboa.tecnico.classes.professor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.*;


/**
 * Encapsulates gRPC channel and stub for remote service. All remote calls from
 * client should use this object.
 */
public class ProfessorFrontend implements AutoCloseable {
    private final ManagedChannel channel;
    private final ProfessorServiceGrpc.ProfessorServiceBlockingStub classServerStub;


    /**
     * ProfessorFrontend constructor
     *
     * @param host (String)
     * @param port (int)
     */
    public ProfessorFrontend(String host, int port) {
        // Channel is the abstraction to connect to a service endpoint.
        // Let us use plaintext communication because we do not have certificates.
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        // Create a blocking stub.
        classServerStub = ProfessorServiceGrpc.newBlockingStub(channel);
    }



    // methods


    /**
     * Open Enrollments method
     *
     * @param request (OpenEnrollmentsRequest)
     * @return OpenEnrollmentsResponse
     */
    public OpenEnrollmentsResponse openEnrollments(OpenEnrollmentsRequest request){
        try{
            OpenEnrollmentsResponse response = classServerStub.openEnrollments(request);
            return response;
        } catch(StatusRuntimeException e){
            return OpenEnrollmentsResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }


    /**
     * Close Enrollment method
     *
     * @param request (CloseEnrollmentsRequest)
     * @return CloseEnrollmentResponse
     */
    public CloseEnrollmentsResponse closeEnrollments(CloseEnrollmentsRequest request){
        try{
            CloseEnrollmentsResponse response = classServerStub.closeEnrollments(request);
            return response;
        } catch(StatusRuntimeException e){
            return CloseEnrollmentsResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }


    /**
     * List class method
     *
     * @param request (ListClassRequest)
     * @return ListClassResponse
     */
    public ListClassResponse listClass(ListClassRequest request){
        try{
            ListClassResponse response = classServerStub.listClass(request);
            return response;
        } catch(StatusRuntimeException e){
            return ListClassResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }


    /**
     * Cancel Enrollment method
     *
     * @param request (CancelEnrollmentRequest)
     * @return CancelEnrollmentResponse
     */
    public CancelEnrollmentResponse cancelEnrollment(CancelEnrollmentRequest request){
        try{
            CancelEnrollmentResponse response = classServerStub.cancelEnrollment(request);
            return response;
        } catch(StatusRuntimeException e){
            return CancelEnrollmentResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }


    /**
     * shutdown the channel
     */
    @Override
    public final void close() {
        channel.shutdown();
    }
}

