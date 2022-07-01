package pt.ulisboa.tecnico.classes.namingserver;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Server;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.DeleteRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.DeleteResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.RegisterRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.RegisterResponse;

public class NamingServerServiceImpl extends NamingServerServiceGrpc.NamingServerServiceImplBase {

    private NamingServices services;
    
    final Logger LOGGER = Logger.getLogger(NamingServerServiceImpl.class.getName());

    private int serverIdCounter = 0;

    private boolean debugMode = false;

    public NamingServerServiceImpl(NamingServices services, boolean debugMode){
        this.services = services;
        this.debugMode = debugMode;
    }


    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver){
        String serviceName = request.getServiceName();
        List<String> qualifierList = request.getServerQualifiersList();
        String hostAndPort = request.getHostAndPort();


        if(services.hasService(serviceName)){
            if(debugMode)
                LOGGER.info("Adding server to already existing service");
            services.addServerToEntry(serviceName, hostAndPort, qualifierList);
        } else{
            if(debugMode){
                LOGGER.info("Creating new service and adding server");
                LOGGER.info("ServiceName: " + serviceName + " HostAndPort: " + hostAndPort + " QualifierList: " + qualifierList.toString());    
            }
            
            services.createServerEntry(serviceName, hostAndPort, qualifierList);
        }

        RegisterResponse response = RegisterResponse.newBuilder().setServerId(serverIdCounter).build();
        serverIdCounter++;
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver){
        String serviceName = request.getServiceName();
        List<String> qualifierList = request.getServerQualifiersList();

        if(debugMode){
            LOGGER.info("Lookup Request Received");
            LOGGER.info("Service Name Received: " + serviceName + " Qualifier List Received: " + qualifierList.toString());
        }

        List<ServerEntry> servers = services.getAllServersForService(serviceName, qualifierList);
        List<Server> serversToAdd = new ArrayList<Server>();
        
        for(ServerEntry server: servers){
            Server newServer = Server.newBuilder().setHost(server.getHost()).setPort(server.getPort()).build();
            serversToAdd.add(newServer);
        }

        if(debugMode)
            LOGGER.info("Servers to add: " + serversToAdd.toString());

        //passar ServerEntry para Server e depois adicionar
        LookupResponse response = LookupResponse.newBuilder()
                .addAllServer(serversToAdd)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver){
        String serviceName = request.getServiceName();
        String hostAndPort = request.getHostAndPort();
        
        if(debugMode)
            LOGGER.info("Delete request for server: " + hostAndPort);
        
        services.deleteServerEntry(serviceName, hostAndPort);
        serverIdCounter--;
        responseObserver.onNext(DeleteResponse.getDefaultInstance());
        responseObserver.onCompleted();

    }
}
