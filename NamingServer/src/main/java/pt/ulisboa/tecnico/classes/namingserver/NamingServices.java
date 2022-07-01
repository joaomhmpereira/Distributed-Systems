package pt.ulisboa.tecnico.classes.namingserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NamingServices {

    private ConcurrentHashMap<String, ServiceEntry> services;

    public NamingServices(){
        this.services = new ConcurrentHashMap<String, ServiceEntry>();
    }

    public boolean hasService(String serviceName){
        return services.containsKey(serviceName);
    }

    public void addServerToEntry(String serviceName, String hostAndPort, List<String> qualifierList){
        ServiceEntry serviceEntry = services.get(serviceName);
        serviceEntry.addToEntry(hostAndPort, qualifierList);
    }

    public void createServerEntry(String serviceName, String hostAndPort, List<String> qualifierList){
        ServiceEntry serviceEntry = new ServiceEntry(serviceName);
        this.services.put(serviceName, serviceEntry);
        addServerToEntry(serviceName, hostAndPort, qualifierList);
    }

    public void deleteServerEntry(String serviceName, String hostAndPort){
        ServiceEntry serviceEntry = services.get(serviceName);
        serviceEntry.removeEntry(hostAndPort);
    }

    /**
     * Gets all servers with a specific service name and specific qualifier.
     * @param serviceName
     * @param qualifierList
     * @return servers (List<ServerEntry>)
     */
    public List<ServerEntry> getAllServersForService(String serviceName, List<String> qualifierList){
        
        boolean getAll = false;
        ArrayList<ServerEntry> servers = new ArrayList<ServerEntry>();

        // if the qualifier list of the request is empty
        if(qualifierList.isEmpty()){ getAll = true; }

        for(Map.Entry<String,ServiceEntry> entry : services.entrySet())
            if(entry.getKey().equals(serviceName)){
                ServiceEntry serviceEntry = entry.getValue();
                for(ServerEntry serverEntry : serviceEntry.getServerEntries())
                    if (getAll || qualifierList.contains(serverEntry.getQualifierList().get(0)))
                        servers.add(serverEntry);
        }
        return servers;
    }
    
}
