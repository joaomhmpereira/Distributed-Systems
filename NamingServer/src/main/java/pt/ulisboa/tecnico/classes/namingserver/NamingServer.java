package pt.ulisboa.tecnico.classes.namingserver;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.util.logging.Logger;

public class NamingServer {

	private static boolean debugMode = false;
	public static void main(String[] args) throws IOException, InterruptedException{
		final Logger LOGGER = Logger.getLogger(NamingServer.class.getName());

		if(args.length == 1 && args[0].equals("-debug")){
			debugMode = true;
		}
		
		NamingServices services = new NamingServices();

		final BindableService namingImpl = new NamingServerServiceImpl(services, debugMode);

		Server server = ServerBuilder.forPort(5000).addService(namingImpl).build();
		server.start();
		if(debugMode)
			LOGGER.info("Server Started on port 5000");

		server.awaitTermination();

	}
}
