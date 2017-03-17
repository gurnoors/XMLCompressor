package xmlCompress;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

//TODO: just try DOM XPath
//or even JDOM.  after exam, max .5 to 1hr if possible

public class StreamingParser {
	private static boolean compress;
	private static String INPUT = null;// = "books.xml";
	private static final String OUTPUT = System.getProperty("user.dir") + "/output.xml";
	private static final String TEST_ATTR = "test";
	private static final String TEST_VAL = "1";

	public static void main(String[] args) throws XMLStreamException, IOException, ParseException {
		Options options = new Options();
		options.addOption("z", "gzip", false, "Compress XML tags with attribute test=\"1\"");
		options.addOption("u", "gunzip", false, "De-compress XML tags with attribute test=\"1\"");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(   options, args);
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
		}
		INPUT = args[0];
		parse();

	}

	private static void parse() throws XMLStreamException, IOException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		InputStream inputStream = new FileInputStream(new File(INPUT));
		XMLEventReader reader = factory.createXMLEventReader(inputStream);

		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		factory.setProperty("javax.xml.stream.isNamespaceAware", false);

		File outputFile = new File(OUTPUT);
		outputFile.createNewFile();
		FileWriter fileWriter = new FileWriter(outputFile);
		// XMLEventWriter writer =
		// outputFactory.createXMLEventWriter(fileWriter);
		XMLEventWriter writer = outputFactory.createXMLEventWriter(System.out);

		XMLEventFactory eventFactory = XMLEventFactory.newFactory();
		while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();

			if (event.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement startElement = event.asStartElement();
				Iterator attributesItr = startElement.getAttributes();
				boolean found = false;
				while (attributesItr.hasNext()) {
					Attribute attribute = (Attribute) attributesItr.next();
					if (attribute.getName().getLocalPart().equals(TEST_ATTR) && attribute.getValue().equals(TEST_VAL)) {
						found = true;

						String content = getContent(reader, event);
						String toWrite;
						if (compress) {
							toWrite = Compressor.compress(content);
							XMLEvent startEvent = eventFactory.createStartElement(startElement.getName(),
									startElement.getAttributes(), startElement.getNamespaces());
							writer.add(startEvent);
							XMLEvent compressedEvent = eventFactory.createCharacters(toWrite);
							writer.add(compressedEvent);
						} else {
							toWrite = Compressor.decompress(content);
							XMLEvent startEvent = eventFactory.createStartElement(startElement.getName(),
									startElement.getAttributes(), startElement.getNamespaces());
							// writer.add(startEvent);
							writer.add(startEvent);
							//XMLEvent compressedEvent = eventFactory.createCharacters(toWrite);
							parseFromString(writer, toWrite);
							//writer.add(compressedEvent);
						}

						break;
					}
				}
				if (!found) {
					writer.add(event);
				}
			} else {
				if (event.isCharacters() && event.asCharacters().isWhiteSpace()) {
					continue;
				}

				writer.add(event);

			}
			// writer.flush();
			// fileWriter.flush();
		}

		inputStream.close();
		writer.close();
		fileWriter.close();

	}

	/**
	 * Parse XML from String and add it to writer.
	 * 
	 * @param writer
	 * @param toWrite
	 * @throws UnsupportedEncodingException
	 * @throws XMLStreamException
	 */
	private static void parseFromString(XMLEventWriter writer, String toWrite)
			throws UnsupportedEncodingException, XMLStreamException {
		toWrite = "<root>" + toWrite + "</root>";
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty("javax.xml.stream.isNamespaceAware", false);

		InputStream inputStream = new ByteArrayInputStream(toWrite.getBytes("UTF-8"));
		XMLEventReader reader = factory.createXMLEventReader(inputStream);

		while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("root")) {
				continue;
			}
			if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals("root")) {
				continue;
			}
			if (event.isStartDocument()) {
				continue;
			}
			if (event.isCharacters() && event.asCharacters().isIgnorableWhiteSpace()) {
				continue;
			}
			writer.add(event);
		}
		// writer.flush();

	}

	/**
	 * Returns the content between the current startElement (assumes reader is
	 * at startElement) and it corresponding endElement.
	 * 
	 * @param reader
	 * @param event
	 * @return
	 * @return child content of this element
	 * @throws XMLStreamException
	 */
	private static String getContent(XMLEventReader reader, XMLEvent event) throws XMLStreamException {
		StringWriter sw = new StringWriter();
		XMLOutputFactory of = XMLOutputFactory.newInstance();
		XMLEventWriter xw = of.createXMLEventWriter(sw);

		StartElement startElement = event.asStartElement();
		QName tagName = startElement.getName();
		event = reader.nextEvent();

		while (reader.hasNext()) {
			if (xw != null) {
				xw.add(event);
			}
			if (reader.hasNext()) {
				if (reader.peek().isEndElement() && reader.peek().asEndElement().getName().equals(tagName)) {
					break;
				}
				event = reader.nextEvent();
			}
		}
		xw.close();
		return sw.toString();
	}

}
