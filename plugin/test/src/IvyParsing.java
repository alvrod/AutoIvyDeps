import com.alvrod.ivyplug.IvyDependency;
import com.alvrod.ivyplug.IvyDescriptor;
import org.junit.Test;

import javax.xml.xpath.XPathExpressionException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IvyParsing {
    @Test
    public void getsDependencies() {
        try {
            IvyDescriptor descriptor = new IvyDescriptor(SampleDescriptor.Content);

            assertEquals("PayTrue", descriptor.Organisation);
            assertEquals("AcquirerMerchantRepository", descriptor.ModuleName);
            assertEquals("1.0", descriptor.Revision);
            assertEquals("ProcesadoraRegional_E1", descriptor.Branch);
            assertEquals("integration", descriptor.Status);

            assertEquals(2, descriptor.Dependencies.size());

            final IvyDependency abc = descriptor.Dependencies.get(0);
            assertEquals("PayTrue", abc.Organisation);
            assertEquals("AcquirerBusinessConfiguration", abc.Name);
            assertEquals("1.+", abc.Rev);
            assertEquals("ProcesadoraRegional_E1", abc.Branch);

            final IvyDependency events = descriptor.Dependencies.get(1);
            assertEquals("PayTrue", events.Organisation);
            assertEquals("Events", events.Name);
            assertEquals("1.+", events.Rev);
            assertEquals("ProcesadoraRegional_E1", events.Branch);
        } catch (XPathExpressionException e) {
            fail(e.toString());
        } catch (IOException e) {
            fail(e.toString());
        }
    }
}
