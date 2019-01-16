/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.codec.xml;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamAttributeWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * A {@link NormalizedNodeStreamWriter} which translates the events into an {@link XMLStreamWriter},
 * resulting in a RFC 6020 XML encoding. There are 2 versions of this class, one that takes a
 * SchemaContext and encodes values appropriately according to the yang schema. The other is
 * schema-less and merely outputs values using toString. The latter is intended for debugging
 * where doesn't have a SchemaContext available and isn't meant for production use.
 */
public abstract class XMLStreamNormalizedNodeStreamWriter<T> implements NormalizedNodeStreamAttributeWriter {
    private static final Logger LOG = LoggerFactory.getLogger(XMLStreamNormalizedNodeStreamWriter.class);
    private static final String COM_SUN_TRANSFORMER =
        "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";

    static final TransformerFactory TRANSFORMER_FACTORY;

    static {
        TransformerFactory fa = TransformerFactory.newInstance();
        if (!fa.getFeature(StAXResult.FEATURE)) {
            LOG.warn("Platform-default TransformerFactory {} does not support StAXResult, attempting fallback to {}",
                    fa, COM_SUN_TRANSFORMER);
            fa = TransformerFactory.newInstance(COM_SUN_TRANSFORMER, null);
            if (!fa.getFeature(StAXResult.FEATURE)) {
                throw new TransformerFactoryConfigurationError("No TransformerFactory supporting StAXResult found.");
            }
        }

        TRANSFORMER_FACTORY = fa;
    }

    private static final Set<String> BROKEN_NAMESPACES = ConcurrentHashMap.newKeySet();

    private final @NonNull XMLStreamWriter writer;
    private final RandomPrefix prefixes;

    XMLStreamNormalizedNodeStreamWriter(final XMLStreamWriter writer) {
        this.writer = requireNonNull(writer);
        this.prefixes = new RandomPrefix(writer.getNamespaceContext());
    }

    /**
     * Create a new writer with the specified context as its root.
     *
     * @param writer Output {@link XMLStreamWriter}
     * @param context Associated {@link SchemaContext}.
     * @return A new {@link NormalizedNodeStreamWriter}
     */
    public static NormalizedNodeStreamWriter create(final XMLStreamWriter writer, final SchemaContext context) {
        return create(writer, context, SchemaPath.ROOT);
    }

    /**
     * Create a new writer with the specified context and rooted in the specified schema path.
     *
     * @param writer Output {@link XMLStreamWriter}
     * @param context Associated {@link SchemaContext}.
     * @param path path
     *
     * @return A new {@link NormalizedNodeStreamWriter}
     */
    public static @NonNull NormalizedNodeStreamWriter create(final XMLStreamWriter writer, final SchemaContext context,
            final SchemaPath path) {
        return new SchemaAwareXMLStreamNormalizedNodeStreamWriter(writer, context, path);
    }

    /**
     * Create a new schema-less writer. Note that this version is intended for debugging
     * where doesn't have a SchemaContext available and isn't meant for production use.
     *
     * @param writer Output {@link XMLStreamWriter}
     *
     * @return A new {@link NormalizedNodeStreamWriter}
     */
    public static @NonNull NormalizedNodeStreamWriter createSchemaless(final XMLStreamWriter writer) {
        return new SchemalessXMLStreamNormalizedNodeStreamWriter(writer);
    }

    abstract void writeValue(@NonNull XMLStreamWriter xmlWriter, QName qname, @NonNull Object value, T context)
            throws IOException, XMLStreamException;

    abstract void startList(NodeIdentifier name);

    abstract void startListItem(PathArgument name) throws IOException;

    private void writeAttributes(final @NonNull Map<QName, String> attributes) throws IOException {
        for (final Entry<QName, String> entry : attributes.entrySet()) {
            try {
                final QName qname = entry.getKey();
                final String namespace = qname.getNamespace().toString();

                if (!Strings.isNullOrEmpty(namespace)) {
                    final String prefix = getPrefix(qname.getNamespace(), namespace);
                    writer.writeAttribute(prefix, namespace, qname.getLocalName(), entry.getValue());
                } else {
                    writer.writeAttribute(qname.getLocalName(), entry.getValue());
                }
            } catch (final XMLStreamException e) {
                throw new IOException("Unable to emit attribute " + entry, e);
            }
        }
    }

    private String getPrefix(final URI uri, final String str) throws XMLStreamException {
        final String prefix = writer.getPrefix(str);
        if (prefix != null) {
            return prefix;
        }

        // This is needed to recover from attributes emitted while the namespace was not declared. Ordinarily
        // attribute namespaces would be bound in the writer, so the resulting XML is efficient, but we cannot rely
        // on that having been done.
        if (BROKEN_NAMESPACES.add(str)) {
            LOG.info("Namespace {} was not bound, please fix the caller", str, new Throwable());
        }

        return prefixes.encodePrefix(uri);
    }

    private void writeStartElement(final QName qname) throws XMLStreamException {
        final String ns = qname.getNamespace().toString();
        final NamespaceContext context = writer.getNamespaceContext();
        final boolean needDefaultNs;
        if (context != null) {
            final String parentNs = context.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
            needDefaultNs = !ns.equals(parentNs);
        } else {
            needDefaultNs = false;
        }

        writer.writeStartElement(XMLConstants.DEFAULT_NS_PREFIX, qname.getLocalName(), ns);
        if (needDefaultNs) {
            writer.writeDefaultNamespace(ns);
        }
    }

    final void writeElement(final QName qname, final Object value, final @Nullable Map<QName, String> attributes,
            final T context) throws IOException {
        try {
            writeStartElement(qname);

            if (attributes != null) {
                writeAttributes(attributes);
            }
            if (value != null) {
                writeValue(writer, qname, value, context);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IOException("Failed to emit element", e);
        }
    }

    final void startElement(final QName qname) throws IOException {
        try {
            writeStartElement(qname);
        } catch (XMLStreamException e) {
            throw new IOException("Failed to start element", e);
        }
    }

    final void endElement() throws IOException {
        try {
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IOException("Failed to end element", e);
        }
    }

    final void anyxmlNode(final QName qname, final Object value) throws IOException {
        if (value != null) {
            checkArgument(value instanceof DOMSource, "AnyXML value must be DOMSource, not %s", value);
            final DOMSource domSource = (DOMSource) value;
            requireNonNull(domSource.getNode());
            checkArgument(domSource.getNode().getNodeName().equals(qname.getLocalName()));
            checkArgument(domSource.getNode().getNamespaceURI().equals(qname.getNamespace().toString()));
            try {
                // TODO can the transformer be a constant ? is it thread safe ?
                final Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
                // Writer has to be wrapped in a wrapper that ignores endDocument event
                // EndDocument event forbids any other modification to the writer so a nested anyXml breaks
                // serialization
                transformer.transform(domSource, new StAXResult(new DelegateWriterNoEndDoc(writer)));
            } catch (final TransformerException e) {
                throw new IOException("Unable to transform anyXml(" + qname + ") value: " + value, e);
            }
        }
    }

    @Override
    public final void startContainerNode(final NodeIdentifier name, final int childSizeHint,
                                         final Map<QName, String> attributes) throws IOException {
        startContainerNode(name, childSizeHint);
        writeAttributes(attributes);
    }

    @Override
    public final void startYangModeledAnyXmlNode(final NodeIdentifier name, final int childSizeHint,
                                                 final Map<QName, String> attributes) throws IOException {
        startYangModeledAnyXmlNode(name, childSizeHint);
        writeAttributes(attributes);
    }

    @Override
    public final void startUnkeyedListItem(final NodeIdentifier name, final int childSizeHint,
                                           final Map<QName, String> attributes) throws IOException {
        startUnkeyedListItem(name, childSizeHint);
        writeAttributes(attributes);
    }

    @Override
    public final void startUnkeyedListItem(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startListItem(name);
    }

    @Override
    public final void startMapEntryNode(final NodeIdentifierWithPredicates identifier, final int childSizeHint,
                                        final Map<QName, String> attributes) throws IOException {
        startMapEntryNode(identifier, childSizeHint);
        writeAttributes(attributes);
    }

    @Override
    public final void startMapEntryNode(final NodeIdentifierWithPredicates identifier, final int childSizeHint)
            throws IOException {
        startListItem(identifier);
    }

    @Override
    public final void startUnkeyedList(final NodeIdentifier name, final int childSizeHint) {
        startList(name);
    }

    @Override
    public final void startMapNode(final NodeIdentifier name, final int childSizeHint) {
        startList(name);
    }

    @Override
    public final void startOrderedMapNode(final NodeIdentifier name, final int childSizeHint) {
        startList(name);
    }

    public static String toString(final Element xml) {
        try {
            final Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            final StreamResult result = new StreamResult(new StringWriter());
            final DOMSource source = new DOMSource(xml);
            transformer.transform(source, result);

            return result.getWriter().toString();
        } catch (IllegalArgumentException | TransformerException e) {
            throw new IllegalStateException("Unable to serialize xml element " + xml, e);
        }
    }

    @Override
    public final void close() throws IOException {
        try {
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Failed to close writer", e);
        }
    }

    @Override
    public final void flush() throws IOException {
        try {
            writer.flush();
        } catch (XMLStreamException e) {
            throw new IOException("Failed to flush writer", e);
        }
    }

    /**
     * Delegate writer that ignores writeEndDocument event. Used for AnyXml serialization.
     */
    private static final class DelegateWriterNoEndDoc implements XMLStreamWriter {
        private final XMLStreamWriter writer;

        DelegateWriterNoEndDoc(final XMLStreamWriter writer) {
            this.writer = writer;
        }

        @Override
        public void writeStartElement(final String localName) throws XMLStreamException {
            writer.writeStartElement(localName);
        }

        @Override
        public void writeStartElement(final String namespaceURI, final String localName) throws XMLStreamException {
            writer.writeStartElement(namespaceURI, localName);
        }

        @Override
        public void writeStartElement(final String prefix, final String localName, final String namespaceURI)
                throws XMLStreamException {
            writer.writeStartElement(prefix, localName, namespaceURI);
        }

        @Override
        public void writeEmptyElement(final String namespaceURI, final String localName) throws XMLStreamException {
            writer.writeEmptyElement(namespaceURI, localName);
        }

        @Override
        public void writeEmptyElement(final String prefix, final String localName, final String namespaceURI)
                throws XMLStreamException {
            writer.writeEmptyElement(prefix, localName, namespaceURI);
        }

        @Override
        public void writeEmptyElement(final String localName) throws XMLStreamException {
            writer.writeEmptyElement(localName);
        }

        @Override
        public void writeEndElement() throws XMLStreamException {
            writer.writeEndElement();

        }

        @Override
        public void writeEndDocument() throws XMLStreamException {
            // End document is disabled
        }

        @Override
        public void close() throws XMLStreamException {
            writer.close();
        }

        @Override
        public void flush() throws XMLStreamException {
            writer.flush();
        }

        @Override
        public void writeAttribute(final String localName, final String value) throws XMLStreamException {
            writer.writeAttribute(localName, value);
        }

        @Override
        public void writeAttribute(final String prefix, final String namespaceURI, final String localName,
                                   final String value) throws XMLStreamException {
            writer.writeAttribute(prefix, namespaceURI, localName, value);
        }

        @Override
        public void writeAttribute(final String namespaceURI, final String localName, final String value)
                throws XMLStreamException {
            writer.writeAttribute(namespaceURI, localName, value);
        }

        @Override
        public void writeNamespace(final String prefix, final String namespaceURI) throws XMLStreamException {
            // Workaround for default namespace
            // If a namespace is not prefixed, it is is still treated as prefix namespace.
            // This results in the NamespaceSupport class ignoring the namespace since xmlns is not a valid prefix
            // Write the namespace at least as an attribute
            // TODO this is a hotfix, the transformer itself should write namespaces passing the namespace
            // in writeStartElement method
            if (prefix.equals("xml") || prefix.equals("xmlns")) {
                writer.writeAttribute(prefix, namespaceURI);
            } else {
                writer.writeNamespace(prefix, namespaceURI);
            }
        }

        @Override
        public void writeDefaultNamespace(final String namespaceURI) throws XMLStreamException {
            writer.writeDefaultNamespace(namespaceURI);
        }

        @Override
        public void writeComment(final String data) throws XMLStreamException {
            writer.writeComment(data);
        }

        @Override
        public void writeProcessingInstruction(final String target) throws XMLStreamException {
            writer.writeProcessingInstruction(target);
        }

        @Override
        public void writeProcessingInstruction(final String target, final String data) throws XMLStreamException {
            writer.writeProcessingInstruction(target, data);
        }

        @Override
        public void writeCData(final String data) throws XMLStreamException {
            writer.writeCData(data);
        }

        @Override
        public void writeDTD(final String dtd) throws XMLStreamException {
            writer.writeDTD(dtd);
        }

        @Override
        public void writeEntityRef(final String name) throws XMLStreamException {
            writer.writeEntityRef(name);
        }

        @Override
        public void writeStartDocument() throws XMLStreamException {
        }

        @Override
        public void writeStartDocument(final String version) throws XMLStreamException {
        }

        @Override
        public void writeStartDocument(final String encoding, final String version) throws XMLStreamException {
        }

        @Override
        public void writeCharacters(final String text) throws XMLStreamException {
            writer.writeCharacters(text);
        }

        @Override
        public void writeCharacters(final char[] text, final int start, final int len) throws XMLStreamException {
            writer.writeCharacters(text, start, len);
        }

        @Override
        public String getPrefix(final String uri) throws XMLStreamException {
            return writer.getPrefix(uri);
        }

        @Override
        public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
            // Disabled since it causes exceptions in the underlying writer
        }

        @Override
        public void setDefaultNamespace(final String uri) throws XMLStreamException {
            writer.setDefaultNamespace(uri);
        }

        @Override
        public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
            writer.setNamespaceContext(context);
        }

        @Override
        public NamespaceContext getNamespaceContext() {
            return writer.getNamespaceContext();
        }

        @Override
        public Object getProperty(final String name) {
            return writer.getProperty(name);
        }
    }
}
