public class SampleDescriptor {
    public static String Content = "<ivy-module version=\"2.0\" xmlns:e=\"http://ant.apache.org/ivy/extra\">\n" +
            "  <info organisation=\"PayTrue\" module=\"AcquirerMerchantRepository\" revision=\"1.0\" branch=\"ProcesadoraRegional_E1\" />\n" +
            "  <publications>\n" +
            "    <artifact name=\"AcquirerMerchantRepository\" type=\"zip\" />\n" +
            "    <artifact name=\"ClientAcquirerMerchantRepository\" type=\"zip\" />\n" +
            "  </publications>\n" +
            "  <dependencies>\n" +
            "    <dependency org=\"PayTrue\" name=\"AcquirerBusinessConfiguration\" rev=\"1.+\" branch=\"ProcesadoraRegional_E1\" />\n" +
            "    <dependency org=\"PayTrue\" name=\"Events\" rev=\"1.+\" branch=\"ProcesadoraRegional_E1\" />\n" +
            "  </dependencies>\n" +
            "</ivy-module>";
}
