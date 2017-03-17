package xmlCompress;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DOMParser {
	// private static String INPUT = "output.xml";
	// private static boolean compress = false;
	private static String POST_URL = "http://posttestserver.com/post.php";
	private static String INPUT = "books.xml";
	private static boolean compress = true;

	private static String OUTPUT;
	private static final String TEST_ATTR = "test";
	private static final String TEST_VAL = "1";

	public static void main(String[] args) throws XPathExpressionException, DOMException, IOException, ParseException {
		Options options = new Options();
		options.addOption("z", "gzip", false, "Compress XML tags with attribute test=\"1\"");
		options.addOption("u", "gunzip", false, "De-compress XML tags with attribute test=\"1\"");
		options.addOption("o", "output", true, "(Optional) Full path of Output file");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		if (cmd.hasOption("gzip")) {
			compress = true;
			System.out.println("Compressing...");
		} else if (cmd.hasOption("gunzip")) {
			compress = false;
			System.out.println("De-compressing...");
		} else {
			System.out.println("Usage: java -jar f5gzip.jar path/to/file.xml --gzip");
			System.out.println("OR");
			System.out.println("Usage: java -jar f5gzip.jar path/to/file.xml --gunzip");
			System.out.println("OR");
			System.out.println("Usage: java -jar f5gzip.jar path/to/file.xml --gunzip --output path/to/output.xml");
			System.exit(1);
		}
		INPUT = args[0];
		if (cmd.hasOption("output")) {
			OUTPUT = cmd.getOptionValue("output");
		} else {
			OUTPUT = System.getProperty("user.dir") + "/"
					+ INPUT.substring(INPUT.lastIndexOf('/') + 1, INPUT.lastIndexOf('.'));
			if (compress) {
				OUTPUT += "_compressed.xml";
			} else {
				OUTPUT += "_decompressed.xml";
			}
			System.out.println("Output file path: " + OUTPUT);
		}
		parse();
	}

	private static void parse() throws XPathExpressionException, DOMException, IOException {
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		Document doc = null;
		try {
			doc = builder.parse(new FileInputStream(INPUT));
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		XPath xPath = XPathFactory.newInstance().newXPath();
		String expression = String.format("//*[@%s=%s]", TEST_ATTR, TEST_VAL);

		NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = (Node) nodeList.item(i);
			String innerXml = getInnerXml(node);
			if (compress) {
				node.setTextContent(Compressor.compress(innerXml));
			} else {
				String decomp = Compressor.decompress(innerXml);
				replaceContent(node, decomp, doc);
			}
		}
		File opFile = new File(OUTPUT);
		opFile.createNewFile();
		write(doc, new FileOutputStream(opFile));
		// write(doc, System.out);
		System.out.println("\nPosting the XML");
		post(doc, POST_URL);
	}

	/**
	 * HTTP Post to the POST_URL with doc in XML body
	 * 
	 * @param doc
	 * @param pOST_URL2
	 * @throws IOException
	 */
	private static void post(Document doc, String pOST_URL2) throws IOException {
		ByteArrayOutputStream oStream = new ByteArrayOutputStream();
		write(doc, oStream);

		OutputStream os;
		URL url = new URL(pOST_URL2);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setDoOutput(true);
		httpConn.setRequestMethod("POST");
		httpConn.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");

		os = httpConn.getOutputStream();
		BufferedWriter osw = new BufferedWriter(new OutputStreamWriter(os));
		osw.write(oStream.toString());
		osw.flush();
		osw.close();
		System.out.println(httpConn.getResponseCode() + " " + httpConn.getResponseMessage());
		System.out.println(IOUtils.toString(httpConn.getInputStream()));
	}

	/**
	 * Removes all child nodes, parses the string in <code>decomp</code> and
	 * sets the parsed node(s) as child(ren) of the <code>node</code>.
	 * 
	 * @param node
	 * @param decomp
	 */
	private static void replaceContent(Node node, String decomp, Document doc) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			node.removeChild(children.item(i));
		}

		Document parsedDoc = parseFromString(decomp);
		NodeList newChildren = parsedDoc.getDocumentElement().getChildNodes();
		for (int i = 0; i < newChildren.getLength(); i++) {
			Node child = newChildren.item(i);
			Node importedNode = doc.importNode(child, true);
			node.appendChild(importedNode);
		}
	}

	/**
	 * parse the string decomp and return the parsed org.w3c.dom.Document
	 * 
	 * @param decomp
	 * @return
	 */
	private static Document parseFromString(String decomp) {
		StringBuilder sb = new StringBuilder(decomp.length() + 12);
		sb.append("<root>");
		sb.append(decomp);
		sb.append("</root>");
		decomp = sb.toString();
		sb = null;
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		Document doc = null;
		try {
			doc = builder.parse(new InputSource(new StringReader(decomp)));
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// return getChildByName(doc.getDocumentElement(),
		// "root").getChildNodes();
		return doc;
	}

	/**
	 * Writes doc to OutputStream out
	 * 
	 * @param doc
	 * @param out
	 * @throws IOException
	 */
	private static void write(Document doc, OutputStream out) throws IOException {
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			DocumentType dt = doc.getDoctype();
			if (dt != null) {
				String pub = dt.getPublicId();
				if (pub != null) {
					t.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, pub);
				}
				t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, dt.getSystemId());
			}
			t.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); // NOI18N
			t.setOutputProperty(OutputKeys.INDENT, "yes"); // NOI18N
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // NOI18N
			Source source = new DOMSource(doc);
			Result result = new StreamResult(out);
			t.transform(source, result);
		} catch (Exception e) {
			throw (IOException) new IOException(e.toString()).initCause(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw (IOException) new IOException(e.toString()).initCause(e);
		}
	}

	/**
	 * 
	 * @param node
	 * @return All child nodes of this node as a String
	 */
	private static String getInnerXml(Node node) {
		DOMImplementationLS lsImpl = (DOMImplementationLS) node.getOwnerDocument().getImplementation().getFeature("LS",
				"3.0");
		LSSerializer lsSerializer = lsImpl.createLSSerializer();
		lsSerializer.getDomConfig().setParameter("xml-declaration", false);
		NodeList childNodes = node.getChildNodes();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < childNodes.getLength(); i++) {
			sb.append(lsSerializer.writeToString(childNodes.item(i)));
		}
		return sb.toString();
	}

}
