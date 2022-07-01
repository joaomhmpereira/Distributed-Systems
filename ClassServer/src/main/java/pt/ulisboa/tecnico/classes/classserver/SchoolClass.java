package pt.ulisboa.tecnico.classes.classserver;

// import GRPC

/**
 * class for implementing the methods used in GRPCS
 */
public class SchoolClass {

    // school class
    private SchoolClassState classState;



    /**
     * schoolClass constructor
     */
    public SchoolClass() {
        classState = new SchoolClassState();
    }



    // getters ans setters


    public SchoolClassState getClassState() {
        return this.classState;
    }

    public void setClassState(SchoolClassState classState) {
        this.classState = classState;
    }
}
