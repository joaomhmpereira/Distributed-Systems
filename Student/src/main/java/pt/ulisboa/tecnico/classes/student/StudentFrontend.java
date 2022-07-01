package pt.ulisboa.tecnico.classes.student;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollResponse;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassResponse;


public class StudentFrontend implements AutoCloseable {
    private final ManagedChannel channel;
    private final StudentServiceGrpc.StudentServiceBlockingStub stub; // for the student server


    /**
     * Student Frontend constructor - for student class server
     *
     * @param host (String)
     * @param port (int)
     */
    public StudentFrontend(String host, int port) {
        // Channel is the abstraction to connect to a service endpoint.
        // Let us use plaintext communication because we do not have certificates.
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        // Create a blocking stub.
        this.stub = StudentServiceGrpc.newBlockingStub(channel);
    }


    // methods


    /**
     * List class method
     *
     * @param request (ListClassRequest)
     * @return ListClassResponse
     */
    public ListClassResponse listClass(ListClassRequest request) {
        try{
            ListClassResponse response = this.stub.listClass(request);
            return response;
        } catch(StatusRuntimeException e){
            return ListClassResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }

        //return stub.listClass(request);
    }


    /**
     * Enroll method
     *
     * @param request (EnrllRequest)
     * @return EnrollResponse
     */
    public EnrollResponse enroll(EnrollRequest request) {
        try{
            EnrollResponse response = stub.enroll(request);
            return response;
        } catch(StatusRuntimeException e){
            return EnrollResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }

    /**
     * channel shutdown
     */
    @Override
    public final void close() {
        channel.shutdown();
    }

}

