package pt.ulisboa.tecnico.classes.admin;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipRequest;


/**
 * Encapsulates gRPC channel and stub for remote service. All remote calls from
 * client should use this object.
 */
public class AdminFrontend implements AutoCloseable {
    private final ManagedChannel channel;
    private final AdminServiceGrpc.AdminServiceBlockingStub stub; // check if we want blocking or not

    public AdminFrontend(String host, int port) {
        // Channel is the abstraction to connect to a service endpoint.
        // Let us use plaintext communication because we do not have certificates.
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        // Create a blocking stub.
        stub = AdminServiceGrpc.newBlockingStub(channel);
    }

    public DumpResponse dump(DumpRequest request){
        try{
            DumpResponse response = stub.dump(request);
            return response;
        } catch(StatusRuntimeException e){
            return DumpResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }

    public ActivateResponse activate(ActivateRequest request) {
        try{
            ActivateResponse response = stub.activate(request);
            return response;
        } catch(StatusRuntimeException e){
            return ActivateResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }

    public DeactivateResponse deactivate(DeactivateRequest request) {
        try{
            DeactivateResponse response = stub.deactivate(request);
            return response;
        } catch(StatusRuntimeException e){
            return DeactivateResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }


    @Override
    public final void close() {
        channel.shutdown();
    }

    public ActivateGossipResponse activateGossip(ActivateGossipRequest  request) {
        try{
            ActivateGossipResponse response = stub.activateGossip(request);
            return response;
        } catch(StatusRuntimeException e){
            return ActivateGossipResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }

    public DeactivateGossipResponse deactivateGossip(DeactivateGossipRequest  request) {
        try{
            DeactivateGossipResponse response = stub.deactivateGossip(request);
            return response;
        } catch(StatusRuntimeException e){
            return DeactivateGossipResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }

    public GossipResponse gossip(GossipRequest  request) {
        try{
            GossipResponse response = stub.gossip(request);
            return response;
        } catch(StatusRuntimeException e){
            return GossipResponse.newBuilder().setCode(ResponseCode.INACTIVE_SERVER).build();
        }
    }
}

