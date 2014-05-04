import com.alvrod.ivyplug.IvyDependency;
import com.alvrod.ivyplug.IvyDescriptor;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class IvyDependencies {
    @Test
    public void simplePositiveDependency() {
        IvyDescriptor libraryDescriptor = new IvyDescriptor();
        libraryDescriptor.Branch = "trunk";
        libraryDescriptor.ModuleName = "MyLibrary";
        libraryDescriptor.Organisation = "MyOrg";
        libraryDescriptor.Revision = "1.0.4.2332";

        IvyDescriptor appDescriptor = new IvyDescriptor();
        appDescriptor.ModuleName = "App";
        appDescriptor.Organisation = "MyOrg";
        appDescriptor.Branch = "stable";

        appDescriptor.Dependencies.add(new IvyDependency("MyOrg", "AnotherLib", "1.+", "stable"));
        appDescriptor.Dependencies.add(new IvyDependency("MyOrg", "MyLibrary", "1.+", "trunk"));

        assertTrue(appDescriptor.dependsOn(libraryDescriptor));
    }

    @Test
    public void simpleNegativeDependency() {
        IvyDescriptor libraryDescriptor = new IvyDescriptor();
        libraryDescriptor.Branch = "trunk";
        libraryDescriptor.ModuleName = "MyLibrary";
        libraryDescriptor.Organisation = "MyOrg";
        libraryDescriptor.Revision = "1.0.4.2332";

        IvyDescriptor appDescriptor = new IvyDescriptor();
        appDescriptor.ModuleName = "App";
        appDescriptor.Organisation = "MyOrg";
        appDescriptor.Branch = "stable";

        appDescriptor.Dependencies.add(new IvyDependency("MyOrg", "AnotherLib", "1.+", "stable"));
        appDescriptor.Dependencies.add(new IvyDependency("MyOrg", "MyLibrary", "0.+", "trunk"));

        assertFalse(appDescriptor.dependsOn(libraryDescriptor));
    }

}
