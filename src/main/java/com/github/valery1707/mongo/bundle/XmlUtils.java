package com.github.valery1707.mongo.bundle;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class XmlUtils {
    private XmlUtils() {
    }

    public static Optional<String> extractFromXml(Path xml, String searchTag) throws IOException {
        try (
                InputStream stream = Files.newInputStream(xml);
        ) {
            String name = "";
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader reader = factory.createFilteredReader(
                    factory.createXMLEventReader(stream, "UTF-8"),
                    event -> event.isStartElement() || event.isEndElement() || event.isCharacters()
            );
            while (reader.hasNext()) {
                XMLEvent event = (XMLEvent) reader.next();
                if (event.isCharacters() && !event.asCharacters().isWhiteSpace() && searchTag.equals(name)) {
                    return Optional.ofNullable(event.asCharacters().getData());
                } else if (event.isStartElement()) {
                    name += "/" + event.asStartElement().getName().getLocalPart();
                } else if (event.isEndElement()) {
                    name = name.substring(0, name.lastIndexOf('/'));
                }
            }
        } catch (IOException | XMLStreamException e) {
            throw new IOException("Fail to parse xml from " + xml.normalize().toAbsolutePath().toString(), e);
        }
        return Optional.empty();
    }
}
