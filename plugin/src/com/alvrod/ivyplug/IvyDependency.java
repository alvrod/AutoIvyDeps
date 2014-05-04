package com.alvrod.ivyplug;

public class IvyDependency {
    public String Organisation;
    public String Name;
    public String Rev;
    public String Branch;

    public IvyDependency() {
    }

    public IvyDependency(String organisation, String name, String rev, String branch) {
        Organisation = organisation;
        Name = name;
        Rev = rev;
        Branch = branch;
    }
}
