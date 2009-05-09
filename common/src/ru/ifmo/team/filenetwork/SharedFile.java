package ru.ifmo.team.filenetwork;

/**
 * User: Daniel Penkin
 * Date: May 2, 2009
 * Version: 1.0
 */
public class SharedFile {

    private String name;

    private String description;

    private final int size;

    private final String hash;

    public SharedFile(int size, String hash) {
        this(null, null, size, hash);
    }

    public SharedFile(String name, String description, int size, String hash) {
        this.name = name;
        this.description = description;
        this.size = size;
        this.hash = hash;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        if (description != null) {
            return description;
        } else {
            return "";
        }
    }

    public int getSize() {
        return size;
    }

    public String getHash() {
        return hash;
    }

    public String toString() {
        return name + " [ " + (double) size / 1024 + " Kb ]";
    }

}
