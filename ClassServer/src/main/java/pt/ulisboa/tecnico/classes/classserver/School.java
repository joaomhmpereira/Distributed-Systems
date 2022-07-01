package pt.ulisboa.tecnico.classes.classserver;

import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;

public class School {

    // all students
    private ConcurrentHashMap<String, Student> allStudents;

    // (only) class
    private SchoolClass schoolClass = null;

    //server status: true -> active; false -> not active
    private boolean serverStatus = true;

    //gossip status: true -> active; false -> not active
    private boolean gossipStatus = true;

    //server id
    private int id;

    //vector clock
    private int[] vectorClock = {0,0};

    /**
     * School constructor
     */
    public School() {
        this.allStudents = new ConcurrentHashMap<String, Student>();
        this.schoolClass = new SchoolClass();
    }

    // getters and setters
    public ConcurrentHashMap<String,Student> getAllStudents() {
        return allStudents;
    }

    public void setAllStudents(ConcurrentHashMap<String, Student> allStudents) {
        this.allStudents = allStudents;
    }

    public SchoolClass getSchoolClass(){
        return this.schoolClass;
    }

    public boolean isServerActive(){
        return this.serverStatus;
    }

    public void activateServer(){
        this.serverStatus = true;
    }

    public void deactivateServer(){
        this.serverStatus = false;
    }

    public void deactivateGossip(){
        this.gossipStatus = false;
    }

    public void activateGossip(){
        this.gossipStatus = true;
    }

    public boolean isGossipActive(){
        return this.gossipStatus;
    }
    
    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return this.id;
    }

    public int[] getVectorClock(){
        return this.vectorClock;
    }

    public synchronized void setVectorClockByIndex(int index, int newValue){
        this.vectorClock[index] = newValue;
    }

    public int getVectorClockByIndex(int index){
        return this.vectorClock[index];
    }


    /**
     * Checks if student id already exists
     *
     * @param studentId (string)
     *
     * @return boolean
     */
    public boolean checkIfIdAlreadyExists(String studentId){
        return allStudents.containsKey(studentId);
    }


    /**
     * checks if the name and id given equals a student
     *
     * @param name (String)
     * @param id (String)
     *
     * @return boolean
     */
    public boolean checkIfStudentAlreadyExists(String name, String id){
        if(allStudents.containsKey(id))
            return name.equals(allStudents.get(id).getStudentName());
        else
            return false;
    }

    public SchoolClassState mergeClassStates(ClassState classState){
        this.schoolClass.getClassState().mergeTwoClassStates(classState);
        return this.schoolClass.getClassState();
    }
}
