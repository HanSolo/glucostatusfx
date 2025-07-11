package eu.hansolo.fx.glucostatus;

@Deprecated(since = "ACI_05.01.2025 15:00:00,1736089200", forRemoval = false)
public class ClassForChangeAnnotation {
    private String value;


    public ClassForChangeAnnotation(String value) {
        this.value = value;
    }


    public String getValue() { return this.value; }
    public void setValue(final String value) { this.value = value; }
}
