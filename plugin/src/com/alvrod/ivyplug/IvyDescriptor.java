package com.alvrod.ivyplug;

import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.regex.Pattern;

public class IvyDescriptor {
    public String Organisation = "";
    public String ModuleName = "";
    public String Branch = "trunk";
    public String Revision = "";
    public String Status = "integration";

    public LinkedList<IvyDependency> Dependencies = new LinkedList<IvyDependency>();

    public static int copy(InputStream input, OutputStream output) throws IOException{
        byte[] buffer = new byte[4096];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public IvyDescriptor() {
    }

    public IvyDescriptor(final InputStream input) throws XPathExpressionException, IOException {
        parse(input);
    }

    public IvyDescriptor(byte[] content) throws XPathExpressionException, IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        parse(inputStream);
    }

    public IvyDescriptor(String content) throws XPathExpressionException, IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        parse(inputStream);
    }

    // bloody evaluate() closes the stream
    public void parse(InputStream inputStream) throws XPathExpressionException, IOException {
        ByteArrayOutputStream temp = new ByteArrayOutputStream(4096);
        copy(inputStream, temp);

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        InputSource source = new InputSource(new ByteArrayInputStream(temp.toByteArray()));
        parseInfo(xpath, source);

        InputSource source2 = new InputSource(new ByteArrayInputStream(temp.toByteArray()));
        parseDependencies(xpath, source2);
    }

    private void parseInfo(XPath xpath, InputSource source) throws XPathExpressionException {
        Node node = (Node) xpath.evaluate("//ivy-module/info", source, XPathConstants.NODE);
        NamedNodeMap attributes = node.getAttributes();

        Organisation = attributes.getNamedItem("organisation").getNodeValue();
        ModuleName = attributes.getNamedItem("module").getNodeValue();
        Branch = getBranch(attributes.getNamedItem("branch"));
        Status = getStatus(attributes.getNamedItem("status"));
        Revision = attributes.getNamedItem("revision").getNodeValue();
    }

    private void parseDependencies(XPath xpath, InputSource source) throws XPathExpressionException {
        NodeList nodes = (NodeList) xpath.evaluate("//ivy-module/dependencies/dependency", source, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            NamedNodeMap attributes = node.getAttributes();

            IvyDependency dep = new IvyDependency();
            dep.Organisation = attributes.getNamedItem("org").getNodeValue();
            dep.Name = attributes.getNamedItem("name").getNodeValue();
            dep.Rev = attributes.getNamedItem("rev").getNodeValue();
            dep.Branch = getBranch(attributes.getNamedItem("branch"));
            Dependencies.add(dep);
        }
    }

    private static String getBranch(Node branchNode) {
        if (branchNode == null) {
            return "trunk";
        }
        return branchNode.getNodeValue();
    }

    private static String getStatus(Node statusNode) {
        if (statusNode == null) {
            return "integration";
        }
        return statusNode.getNodeValue();
    }

    public boolean dependsOn(IvyDescriptor ivyDescriptor) {
        if (ivyDescriptor != null) {
            for (IvyDependency dependency : Dependencies) {
                if (ivyDescriptor.Organisation.equals(dependency.Organisation) &&
                        ivyDescriptor.ModuleName.equals(dependency.Name) &&
                        ivyDescriptor.Branch.equals(dependency.Branch) &&
                        ivyDescriptor.matchesRevision(dependency)) {
                    return true;
                }
            }
        }
        return false;
    }

    // support: patterns like 1.0.+ and latest.status
    private boolean matchesRevision(IvyDependency dependency) {
        if (Revision.contains("latest")) {
            return true;
        }

        Pattern pattern = Pattern.compile(dependency.Rev);
        return pattern.matcher(Revision).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IvyDescriptor that = (IvyDescriptor) o;

        if (Branch != null ? !Branch.equals(that.Branch) : that.Branch != null) return false;
        if (ModuleName != null ? !ModuleName.equals(that.ModuleName) : that.ModuleName != null) return false;
        if (Organisation != null ? !Organisation.equals(that.Organisation) : that.Organisation != null) return false;
        if (Revision != null ? !Revision.equals(that.Revision) : that.Revision != null) return false;
        if (Status != null ? !Status.equals(that.Status) : that.Status != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = Organisation != null ? Organisation.hashCode() : 0;
        result = 31 * result + (ModuleName != null ? ModuleName.hashCode() : 0);
        result = 31 * result + (Branch != null ? Branch.hashCode() : 0);
        result = 31 * result + (Revision != null ? Revision.hashCode() : 0);
        result = 31 * result + (Status != null ? Status.hashCode() : 0);
        return result;
    }
}
