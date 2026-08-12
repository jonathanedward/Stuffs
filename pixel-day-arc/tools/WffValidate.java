import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Validates a Watch Face Format document against the official WFF XSD.
 *
 * The schema uses XSD 1.1 assertions, so this needs Xerces rather than the
 * JDK's built-in 1.0-only validator. tools/validate.sh wires up the
 * classpath and picks the right schema version.
 *
 *   java -cp <xerces jars>:. WffValidate <watchface.xsd> <watchface.xml>
 *
 * Exits non-zero if the document is invalid, so it drops straight into CI.
 */
public class WffValidate {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("usage: WffValidate <schema.xsd> <watchface.xml>");
            System.exit(2);
        }

        SchemaFactory factory =
                SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        Schema schema = factory.newSchema(new File(args[0]));
        Validator validator = schema.newValidator();

        final int[] errors = {0};
        validator.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException e) {
                System.out.println("  warn   line " + e.getLineNumber() + ": " + e.getMessage());
            }
            public void error(SAXParseException e) {
                errors[0]++;
                System.out.println("  error  line " + e.getLineNumber() + ": " + e.getMessage());
            }
            public void fatalError(SAXParseException e) {
                errors[0]++;
                System.out.println("  fatal  line " + e.getLineNumber() + ": " + e.getMessage());
            }
        });

        validator.validate(new StreamSource(new File(args[1])));

        if (errors[0] == 0) {
            System.out.println("  valid");
        } else {
            System.out.println("  " + errors[0] + " error(s)");
            System.exit(1);
        }
    }
}
