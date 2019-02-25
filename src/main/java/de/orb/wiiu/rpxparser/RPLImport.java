package de.orb.wiiu.rpxparser;

import lombok.Data;

@Data
public class RPLImport implements Comparable<RPLImport> {
    private final String name;
    private final String rplName;

    @Override
    public int compareTo(RPLImport o) {
        return name.compareTo(o.name);
    }
}
