package org.logstashplugins;

public class AgensProperty {

    public String key;
    public String type;
    public String value;

    public AgensProperty(){}
    public AgensProperty(final String key, final String type, final Object value) {
        this.key = key;
        this.type = type;
        this.value = value.toString();
    }

    // @Override public String elementId() { return this.elementId; }
    public String getKey() { return this.key; }
    public String getType() { return this.type; }
    public String getValue() { return this.value; }

    @Override
    public String toString() {
        return String.format("%s<%s>=%s", key, type, value);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        if (this == obj) return true;

        AgensProperty that = (AgensProperty) obj;
        if (this.key == null || that.getKey() == null || !this.key.equals(that.getKey()) )
            return false;
        if (this.value == null || that.getValue() == null || !this.value.equals(that.getValue()))
            return false;
        if (this.type == null || that.getType() == null || !this.type.equals(that.getType()) )
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return 31*value.hashCode() + 43*type.hashCode() + 59*key.hashCode();
    }

}
