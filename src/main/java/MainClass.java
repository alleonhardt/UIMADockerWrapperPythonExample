import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.TypeSystemUtil;
import org.apache.uima.util.XmlCasSerializer;
import org.hucompute.uimadockerwrapper.DockerWrappedEnvironment;
import org.hucompute.uimadockerwrapper.DockerWrapperContainerConfiguration;
import org.hucompute.uimadockerwrapper.annotators.ExampleAnnotator;
import org.hucompute.uimadockerwrapper.base_env.DockerBaseJavaEnv;
import org.hucompute.uimadockerwrapper.util.DockerWrapperUtil;
import org.xml.sax.SAXException;

import java.io.*;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Pretty-prints xml, supplied as a string.
 * <p/>
 * eg.
 * <code>
 * String formattedXml = new XmlFormatter().format("<tag><nested>hello</nested></tag>");
 * </code>
 */
class XmlFormatter {

    public XmlFormatter() {
    }

    public static String format(String unformattedXml) {
        System.out.println(unformattedXml);
        try {
            final Document document = parseXmlFile(unformattedXml);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");


            //initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(document);
            transformer.transform(source, result);
            String xmlString = result.getWriter().toString();
            return(xmlString);


        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static String printDocument(String unformattedXml) throws IOException, TransformerException {
        final Document document = parseXmlFile(unformattedXml);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(document);
        transformer.transform(source, result);
        String xmlString = result.getWriter().toString();
        return(xmlString);
    }

    private static Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new ByteArrayInputStream(in.getBytes(StandardCharsets.UTF_8)));
            return db.parse(is);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getString(CAS jCas) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            XmiCasSerializer.serialize(jCas, baos);
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return baos.toString();
    }

    public static String getPrettyString(CAS jcas){
        return format(getString(jcas));
    }
}


public class MainClass {
    public static void main(String []args) throws UIMAException, IOException, SAXException {
        JCas test_c = JCasFactory.createJCas();
        test_c.setDocumentText("Simple change's in the pipelines used.");
        test_c.setDocumentLanguage("en");


// The annotation should be made within a container
        DockerWrapperContainerConfiguration cfg = DockerWrapperContainerConfiguration.default_config()
                .with_run_in_container(true);


        TypeSystemDescription ts = TypeSystemUtil.typeSystem2TypeSystemDescription(test_c.getTypeSystem());
        StringWriter wr = new StringWriter();
        ts.toXML(wr);
// Create the wrapped pipeline from any AnalysisEngineDescription
        DockerWrappedEnvironment env = DockerWrappedEnvironment.from(
                AnalysisEngineFactory.createEngineDescription(ExampleAnnotator.class)
        ).withResource("main.py",new File("main.py"))
                .withResource("typesystem.xml", wr.getBuffer().toString())
                        .with_dockerfile("FROM ubuntu:20.04\n" +
                                        "RUN DEBIAN_FRONTEND=noninteractive apt update -y\n"+
                                "RUN DEBIAN_FRONTEND=noninteractive apt install -y python3 pip\n" +
                                "RUN pip3 install dkpro-cassis spacy\n"+
                                "RUN python3 -m spacy download en_core_web_sm\n"+
                                "ADD main.py main.py\n" +
                                "ADD typesystem.xml typesystem.xml\n"+
                                "CMD python3 main.py"
                        );


        System.out.println(DockerWrapperUtil.cas_to_xmi(test_c));
        SimplePipeline.runPipeline(test_c,env.build(cfg));
        System.out.println(DockerWrapperUtil.cas_to_xmi(test_c));
    }
}
