package pt.ulisboa.tecnico.classes;

public class StudentClass {

  // student's name
  private String name;

  // student's id
  private String id;



  /**
   * StudentClass constructor
   *
   * @param name (String)
   * @param id (String)
   */
  public StudentClass(String name, String id) {
    this.name = name;
    this.id = id;
  }



  // getters and setters

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
