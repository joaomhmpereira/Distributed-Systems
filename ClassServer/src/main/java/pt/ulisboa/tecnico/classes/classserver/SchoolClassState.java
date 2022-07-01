package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.Student;
import pt.ulisboa.tecnico.classes.StudentClass;

import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

public class SchoolClassState {

    // class capacity
    private int classCapacity;

    // enrollments (open/closed)
    private boolean enrollmentOpen;

    // student list enrolled in class
    private ConcurrentHashMap<String, StudentClass> students;

    // student not enrolled list
    private ConcurrentHashMap<String, StudentClass> discarded;

    // private List
    final Logger LOGGER = Logger.getLogger(SchoolClassState.class.getName());



    /**
     * SchoolClassState constructor
     */
    public SchoolClassState() {
        this.classCapacity = 0;
        this.enrollmentOpen = false;
        this.students = new ConcurrentHashMap<String, StudentClass>();
        this.discarded = new ConcurrentHashMap<String, StudentClass>();
    }


    // getters and setters

    public int getClassCapacity() {
        return classCapacity;
    }

    public synchronized void setClassCapacity(int classCapacity) {
        this.classCapacity = classCapacity;
    }

    public boolean isEnrollmentOpen() {
        return enrollmentOpen;
    }

    public synchronized void setEnrollmentState(boolean enrollmentState) {
        this.enrollmentOpen = enrollmentState;
    }

    public ConcurrentHashMap<String, StudentClass> getstudents() {
        return students;
    }

    public synchronized void setEnrolledStudents(ConcurrentHashMap<String, StudentClass> students) {
        this.students = students;
    }

    public ConcurrentHashMap<String,StudentClass> getDiscarded() {
        return discarded;
    }

    public synchronized void setDiscarded(ConcurrentHashMap<String, StudentClass> discarded) {
        this.discarded = discarded;
    }



    // methods



    /**
     * Checks if the class is already full.
     *
     * @return boolean
     */
    public boolean isClassFull(){
        return this.classCapacity <= students.size();
    }



    /**
     * Checks if there is a student with a given id enrolled
     *
     * @param studentId (String)
     *
     * @return boolean
     */
    public boolean isIdEnrolled(String studentId){
        return students.containsKey(studentId);
    }


    /**
     * Checks if student is enrolled in the class
     *
     * @param studentName (String)
     * @param studentId (String)
     *
     * @return boolean
     */
    public boolean isStudentEnrolled(String studentName, String studentId){
        if(students.containsKey(studentId)){
            return studentName.equals(students.get(studentId).getName());
        } else{
            return false;
        }
    }


    /**
     * Checks if student is enrolled in the class
     *
     * @param studentId (String)
     *
     * @return boolean
     */
    public boolean isStudentEnrolled(String studentId){
        return students.containsKey(studentId);
    }


    /**
     * Checks if student is in the list of not enrolled students
     *
     * @param studentName (String)
     * @param studentId (String)
     *
     * @return boolean
     */
    public boolean isStudentDiscardedList(String studentName, String studentId){
        //if(discarded.containsKey(studentId)){
        //    return studentName.equals(students.get(studentId).getName());
        //} else{
        //    return false;
        //}
        return discarded.containsKey(studentId);
    }


    /**
     * gets the student that is already enrolled
     *
     * @param studentId (String)
     *
     * @return StudentClass
     */
    public StudentClass getStudentAlreadyEnrolled(String studentId){
        return students.get(studentId);
    }


    /**
     * Adds the student to the Not enrolled list
     *
     * @param studentName (String)
     * @param studentId (String)
     */
    public void addStudentToDiscardedList(String studentName, String studentId){
        StudentClass studentToAdd = new StudentClass(studentName, studentId);
        discarded.put(studentId, studentToAdd);
    }


    /**
     * Adds student to the enrolled list
     *
     * @param studentName (String)
     * @param studentId (String)
     *
     */
    public void addStudentToEnrolledList(String studentName, String studentId){
        StudentClass studentToAdd = new StudentClass(studentName, studentId);
        students.put(studentId, studentToAdd);
    }


    /**
     * Removes student from Enrolled list
     *
     * @param studentId (String)
     */
    public void removeStudentFromEnrolledlist(String studentId){
        this.students.remove(studentId);
    }


    /**
     * Removes student from not Enrolled list
     *
     * @param studentId (String)
     */
    public void removeStudentFromDiscardedList(String studentId){
        this.discarded.remove(studentId);
    }


    /**
     * Passes the list of Enrolled Students to the classState (contract)
     * enrolled list
     *
     * @param classState (ClassState.Builder)
     */
    public void updateClassStateEnrolledStudents(ClassState.Builder classState){
        for(Entry<String, StudentClass> entry: this.students.entrySet()){
            Student newStudent = Student.newBuilder()
                                        .setStudentId(entry.getValue().getId())
                                        .setStudentName(entry.getValue().getName())
                                        .build();
            classState.addEnrolled(newStudent);
        }
    }


    /**
     * Passes the list of Not Enrolled Students to the classState (contract)
     * Not enrolled list
     *
     * @param classState (ClassState.Builder)
     */
    public synchronized void updateClassStateDiscardedStudents(ClassState.Builder classState){
        for(Entry<String, StudentClass> entry: this.discarded.entrySet()){
            Student newStudent = Student.newBuilder()
                                        .setStudentId(entry.getValue().getId())
                                        .setStudentName(entry.getValue().getName())
                                        .build();
            classState.addDiscarded(newStudent);
        }
    }


    /**
     * changes the list of students enrolled in class from
     * a received classState
     *
     * @param studentsToAdd (List<Student>)
     */
    public synchronized void changeEnrolledList(List<Student> studentsToAdd){
        // claer students enrolled
        this.students.clear();
        // update the enrolled students list
        for(Student student : studentsToAdd){
            StudentClass newStudent = new StudentClass(student.getStudentName(), student.getStudentId());
            this.students.put(newStudent.getId(), newStudent);
        }
    }


    /**
     * change the list of students not enrolled in class from
     * a received classState
     *
     * @param studentsToAdd (List<Student>)
     */
    public synchronized void changeDiscardedList(List<Student> studentsToAdd){
        // claer students enrolled
        this.discarded.clear();
        // update the enrolled students list
        for(Student student : studentsToAdd){
            StudentClass newStudent = new StudentClass(student.getStudentName(), student.getStudentId());
            this.discarded.put(newStudent.getId(), newStudent);
        }
    }


    /**
     * Receive two classStates and merge them according do Merge policies previously defined
     * 
     * Merge policies:
     *  - different classCapacities -> keep the bigger of the two
     *  - different enrollment status -> keep false
     *  - different enrolled list -> if capacity allows, keep every student
     *                               otherwise, randomly choose who gets discarded
     *  - different discarded list -> if student is on both discarded and enrolled lists, 
     *                                keep him on discarded and remove from the enrolled
     *                                
     *                                if student is only in discarded, keep him there 
     * 
     * @param receiveClassState
     */
    public synchronized void mergeTwoClassStates(ClassState receivedClassState){
        //check if classCapacities differ
        if(this.classCapacity != receivedClassState.getCapacity()){
            this.classCapacity = Math.max(this.classCapacity, receivedClassState.getCapacity());
        }
        //check if enrollment status differs
        if(this.isEnrollmentOpen() != receivedClassState.getOpenEnrollments()){
            this.enrollmentOpen = false;
        }

        //check if discarded lists differ
        List<Student> receivedDiscardedToAdd = receivedClassState.getDiscardedList()
                                               .stream()
                                               .filter(student -> {
                                                   return !discarded.containsKey(student.getStudentId());
                                               })
                                               .collect(Collectors.toList());
        
        //for every student that is in discarded list we received and is not in ours
        for(Student student: receivedDiscardedToAdd){
            if(students.containsKey(student.getStudentId())){
                students.remove(student.getStudentId());
            }
            StudentClass newStudent = new StudentClass(student.getStudentName(), student.getStudentId());
            discarded.put(student.getStudentId(), newStudent);
        }
        
        
        //check if enrolled lists differ
        //for every student we receive
        for(Student student : receivedClassState.getEnrolledList()){
            //check if we already have him enrolled
            if(!students.containsKey(student.getStudentId())){
                //if he's not enrolled check if it's possible to enroll
                if(this.students.size() < this.classCapacity){
                    if(!discarded.containsKey(student.getStudentId())){
                        //enroll student
                        StudentClass newStudent = new StudentClass(student.getStudentName(), student.getStudentId());
                        students.put(student.getStudentId(), newStudent);
                    }
                } else{
                    //was not possible to enroll, check to see if he's already in discarded list
                    if(!discarded.containsKey(student.getStudentId())){
                        //add to discarded list
                        StudentClass newStudent = new StudentClass(student.getStudentName(), student.getStudentId());
                        discarded.put(student.getStudentId(), newStudent);
                    }
                }
            }
        }
    }
}
