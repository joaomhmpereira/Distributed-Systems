package pt.ulisboa.tecnico.classes.student;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;



public class StudentNamingServerFrontend implements AutoCloseable {
    private final ManagedChannel channel;
    private final NamingServerServiceGrpc.NamingServerServiceBlockingStub stub; // for the naming server
    private static String host = "localhost";
    private static int port = 5000;


    /**
     * Student Frontend constructor - for naming server connection
     */
    public StudentNamingServerFrontend(){
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.stub = NamingServerServiceGrpc.newBlockingStub(channel);
    }



    // methods


    /**
     * lookup method
     *
     * @param request (lookupRequest)
     * @return LookupResponse*/
    public LookupResponse lookup(LookupRequest request){
        return stub.lookup(request);
    }


    /**
     * channel shutdown
     */
    @Override
    public final void close() {
        channel.shutdown();
    }

}

