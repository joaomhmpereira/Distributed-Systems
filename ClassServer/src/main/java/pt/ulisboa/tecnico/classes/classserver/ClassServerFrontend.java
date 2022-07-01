package pt.ulisboa.tecnico.classes.classserver;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateRequest;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateResponse;


public class ClassServerFrontend implements AutoCloseable {
    private final ManagedChannel channel;
    private final ClassServerServiceGrpc.ClassServerServiceBlockingStub stub; // for the student server

    /**
     * constructor
     *
     * @param host (String)
     * @param port (int)
     */
    public ClassServerFrontend(String host, int port) {
        // Channel is the abstraction to connect to a service endpoint.
        // Let us use plaintext communication because we do not have certificates.
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        // Create a blocking stub.
        this.stub = ClassServerServiceGrpc.newBlockingStub(channel);
    }


    /**
     * propagate state method
     *
     * @param schoolClassState (SchoolClassState)
     * @return response (PropagateStateResponse)
     */
    public PropagateStateResponse propagateState(SchoolClassState schoolClassState, int[] clock){
        try{
            ClassState classState = createClassStateFromSchoolClass(schoolClassState);
            List<Integer> clockList = Arrays.stream(clock).boxed().collect(Collectors.toList());
            PropagateStateRequest request = PropagateStateRequest.newBuilder()
                                            .setClassState(classState)
                                            .addAllClock(clockList)
                                            .build();
            PropagateStateResponse response = this.stub.propagateState(request);
            return response;
        } catch(StatusRuntimeException e){
            return PropagateStateResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }

    /**
     * creates a class State (classes definition) from the school Class state
     *
     * @param schoolClassState (SchoolClassState)
     * @return classState (ClassState)
     * */
    public ClassState createClassStateFromSchoolClass(SchoolClassState schoolClassState){
        ClassState.Builder classStateBuilder = ClassesDefinitions.ClassState.newBuilder();

        schoolClassState.updateClassStateEnrolledStudents(classStateBuilder);
        schoolClassState.updateClassStateDiscardedStudents(classStateBuilder);
        classStateBuilder.setCapacity(schoolClassState.getClassCapacity())
                .setOpenEnrollments(schoolClassState.isEnrollmentOpen());
        ClassesDefinitions.ClassState classState = classStateBuilder.build();
        return classState;
    }

    /**
     * channel shutdown
     */
    @Override
    public final void close() {
        channel.shutdown();
    }
}
