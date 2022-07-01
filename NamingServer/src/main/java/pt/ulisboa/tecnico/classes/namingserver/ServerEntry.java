package pt.ulisboa.tecnico.classes.namingserver;

import java.util.List;

public class ServerEntry {

    private String host;

    private Integer port;

    private List<String> qualifierList;

    public ServerEntry(String hostAndPort, List<String> qualifierList){
        String[] lst = hostAndPort.split(":");
        host = lst[0];
        port = Integer.parseInt(lst[1]);
        this.qualifierList = qualifierList;
    }

    public void addQualifier(String qualifier){
        this.qualifierList.add(qualifier);
    }

    public List<String> getQualifierList(){
        return this.qualifierList;
    }

    public String getHost(){
        return this.host;
    }

    public Integer getPort(){
        return this.port;
    }
}
