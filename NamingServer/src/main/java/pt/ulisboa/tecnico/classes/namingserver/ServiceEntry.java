package pt.ulisboa.tecnico.classes.namingserver;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ServiceEntry {

    private String serviceName;

    private Set<ServerEntry> serverEntries;

    public ServiceEntry(String serviceName){
        this.serviceName = serviceName;
        serverEntries = new HashSet<ServerEntry>();
    }

    public void addToEntry(String hostAndPort, List<String> qualifierList){
        ServerEntry entry = new ServerEntry(hostAndPort, qualifierList);
        serverEntries.add(entry);
    }

    public void removeEntry(String hostAndPort){
        String[] lst = hostAndPort.split(":");
        String host = lst[0];
        Integer port = Integer.parseInt(lst[1]);

        for (Iterator<ServerEntry> i = serverEntries.iterator(); i.hasNext();) {
            ServerEntry element = i.next();
            if(element.getHost().equals(host) && element.getPort().equals(port)){
                serverEntries.remove(element);
                System.out.println("Server removed");
            }
        }
    }

    public Set<ServerEntry> getServerEntries(){
        return this.serverEntries;
    }

    public String getServiceName(){
        return this.serviceName;
    }
}
