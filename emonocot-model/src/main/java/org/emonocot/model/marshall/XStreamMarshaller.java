/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.emonocot.model.marshall;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.support.AbstractMarshaller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.DomWriter;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.SaxWriter;
import com.thoughtworks.xstream.io.xml.StaxReader;
import com.thoughtworks.xstream.io.xml.StaxWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;

/**
 * Implementation of the <code>Marshaller</code> interface for XStream.
 *
 * <p>
 * By default, XStream does not require any further configuration, though class
 * aliases can be used to have more control over the behavior of XStream.
 *
 * <p>
 * Due to XStream's API, it is required to set the encoding used for writing to
 * OutputStreams. It defaults to <code>UTF-8</code>.
 *
 * <p>
 * <b>NOTE:</b> XStream is an XML serialization library, not a data binding
 * library. Therefore, it has limited namespace support. As such, it is rather
 * unsuitable for usage within Web services.
 *
 * @author Peter Meijer
 * @author Arjen Poutsma
 * @since 3.0
 * @see #setAliases
 * @see #setConverters
 * @see #setEncoding
 */
public class XStreamMarshaller extends AbstractMarshaller implements
        InitializingBean, BeanClassLoaderAware {

    /**
     * The default encoding used for stream access: UTF-8.
     */
    public static final String DEFAULT_ENCODING = "UTF-8";

    /**
     *
     */
    private ReflectionProvider reflectionProvider;

    /**
     *
     */
    private ConverterMatcher[] converters = new ConverterMatcher[0];

   /**
    *
    */
   private boolean autodetectAnnotations;

    /**
     *
     */
    private XStream xstream = new XStream();

    /**
     *
     */
    private HierarchicalStreamDriver streamDriver;

    /**
     *
     */
    private String encoding = DEFAULT_ENCODING;

    /**
     *
     */
    private Class[] supportedClasses;

    /**
     *
     */
    private ClassLoader classLoader;

    /**
     *
     */
    private QNameMap qNameMap;

    /**
     * @param newQNameMap Set the qNameMap to use
     */
    public final void setQNameMap(final QNameMap newQNameMap) {
        this.qNameMap = newQNameMap;
    }

    /**
     * Returns the XStream instance used by this marshaller.
     *
     * @return the xstream instance used by this marshaller
     */
    public final XStream getXStream() {
        return xstream;
    }

    /**
     * Set the XStream mode.
     *
     * @see XStream#XPATH_REFERENCES
     * @see XStream#ID_REFERENCES
     * @see XStream#NO_REFERENCES
     *
     * @param mode Set the XStream mode
     */
    public final void setMode(final int mode) {
        this.getXStream().setMode(mode);
    }

    /**
     * Set the <code>Converters</code> or <code>SingleValueConverters</code> to
     * be registered with the <code>XStream</code> instance.
     *
     * @see Converter
     * @see SingleValueConverter
     *
     * @param newConverters set the converter
     */
    public final void setConverters(final ConverterMatcher[] newConverters) {
        this.converters = newConverters;
        for (int i = 0; i < newConverters.length; i++) {
            if (newConverters[i] instanceof Converter) {
                this.getXStream().registerConverter(
                        (Converter) newConverters[i], i);
            } else if (newConverters[i] instanceof SingleValueConverter) {
                this.getXStream().registerConverter(
                        (SingleValueConverter) newConverters[i], i);
            } else {
                throw new IllegalArgumentException("Invalid ConverterMatcher ["
                        + newConverters[i] + "]");
            }
        }
    }

    /**
     * Sets an alias/attribute map, consisting of string aliases mapped to
     * attributes. Keys are aliases; values are String alias names.
     *
     * @see XStream#aliasAttribute(String, String)
     * @param aliases The alias/attribute map
     */
    public final void setAttributeAliases(final Map<String, String> aliases) {

        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            this.getXStream().aliasAttribute(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Sets an alias/type map, consisting of string aliases mapped to classes.
     * Keys are aliases; values are either {@code Class} instances, or String
     * class names.
     *
     * @see XStream#alias(String, Class)
     * @param aliases The alias/type map
     * @throws ClassNotFoundException
     *             if a string value cannot be translated into a valid class.
     */
    public final void setAliases(final Map<String, ?> aliases)
            throws ClassNotFoundException {
        Map<String, Class<?>> classMap = toClassMap(aliases);

        for (Map.Entry<String, Class<?>> entry : classMap.entrySet()) {
            this.getXStream().alias(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Sets the aliases by type map, consisting of string aliases mapped to
     * classes. Any class that is assignable to this type will be aliased to the
     * same name. Keys are aliases; values are either {@code Class} instances,
     * or String class names.
     *
     * @see XStream#aliasType(String, Class)
     * @param aliases The alias by type map
     * @throws ClassNotFoundException
     *             if a string value cannot be translated into a valid class.
     */
    public final void setAliasesByType(final Map<String, ?> aliases)
            throws ClassNotFoundException {
        Map<String, Class<?>> classMap = toClassMap(aliases);

        for (Map.Entry<String, Class<?>> entry : classMap.entrySet()) {
            this.getXStream().aliasType(entry.getKey(), entry.getValue());
        }
    }

    /**
     *
     * @param map The alias by type map
     * @return A map of aliases to java classes
     * @throws ClassNotFoundException
     *             if a string value cannot be translated into a valid class.
     */
    private Map<String, Class<?>> toClassMap(final Map<String, ?> map)
            throws ClassNotFoundException {
        Map<String, Class<?>> result = new LinkedHashMap<String, Class<?>>(
                map.size());

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Class type;
            if (value instanceof Class) {
                type = (Class) value;
            } else if (value instanceof String) {
                String s = (String) value;
                type = ClassUtils.forName(s, classLoader);
            } else {
                throw new IllegalArgumentException("Unknown value [" + value
                        + "], expected String or Class");
            }
            result.put(key, type);
        }
        return result;
    }

    /**
     * Sets a field alias/type map, consiting of field names.
     *
     * @param aliases a mapping of field name to types
     * @throws ClassNotFoundException
     *              if a string value cannot be translated into a valid class.
     * @throws NoSuchFieldException
     *              if a string value cannot be translated into a valid field.
     * @see XStream#aliasField(String, Class, String)
     */
    public final void setFieldAliases(final Map<String, String> aliases)
            throws ClassNotFoundException, NoSuchFieldException {
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            String alias = entry.getValue();
            String field = entry.getKey();
            int idx = field.lastIndexOf('.');
            if (idx != -1) {
                String className = field.substring(0, idx);
                Class clazz = ClassUtils.forName(className, classLoader);
                String fieldName = field.substring(idx + 1);
                this.getXStream().aliasField(alias, clazz, fieldName);
            } else {
                throw new IllegalArgumentException("Field name [" + field
                        + "] does not contain '.'");
            }
        }
    }

    /**
     * Set types to use XML attributes for.
     *
     * @see XStream#useAttributeFor(Class)
     * @param types Set the array of classes which should be XML attributes
     */
    public final void setUseAttributeForTypes(final Class[] types) {
        for (Class type : types) {
            this.getXStream().useAttributeFor(type);
        }
    }

    /**
     * Set the types to use XML attributes for. The given map can contain either
     * {@code <String, Class>} pairs, in which case
     * {@link XStream#useAttributeFor(String, Class)} is called. Alternatively,
     * the map can contain {@code <Class, String>} or
     * {@code <Class, List<String>>} pairs, which results in
     * {@link XStream#useAttributeFor(Class, String)} calls.
     *
     * @param attributes a map of types to use XML attributes for
     */
    public final void setUseAttributeFor(final Map<?, ?> attributes) {
        for (Map.Entry<?, ?> entry : attributes.entrySet()) {
            if (entry.getKey() instanceof String) {
                if (entry.getValue() instanceof Class) {
                    this.getXStream().useAttributeFor((String) entry.getKey(),
                            (Class) entry.getValue());
                } else {
                    throw new IllegalArgumentException(
                            "Invalid argument 'attributes'. 'useAttributesFor'"
                            + " property takes map of <String, Class>,"
                            + " when using a map key of type String");
                }
            } else if (entry.getKey() instanceof Class) {
                Class<?> key = (Class<?>) entry.getKey();
                if (entry.getValue() instanceof String) {
                    this.getXStream().useAttributeFor(key,
                            (String) entry.getValue());
                } else if (entry.getValue() instanceof List) {
                    List list = (List) entry.getValue();

                    for (Object o : list) {
                        if (o instanceof String) {
                            this.getXStream().useAttributeFor(key, (String) o);
                        }
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Invalid argument 'attributes'. "
                                  + "'useAttributesFor' property takes either"
                                  + " <Class, String> or <Class, List<String>> "
                                  + " when using a map key of type Class");
                }
            } else {
                throw new IllegalArgumentException(
                        "Invalid argument 'attributes. "
                          + "'useAttributesFor' property takes either"
                          + " a map key of type String or Class");
            }
        }
    }

    /**
     * Specify implicit collection fields, as a Map consisting of
     * <code>Class</code> instances mapped to comma separated collection field
     * names.
     *
     * @see XStream#addImplicitCollection(Class, String)
     * @param implicitCollections a collection of fields to map
     */
    public final void setImplicitCollections(
            final Map<Class<?>, String> implicitCollections) {
        for (Map.Entry<Class<?>, String> entry
                : implicitCollections.entrySet()) {
            String[] collectionFields = StringUtils
                    .commaDelimitedListToStringArray(entry.getValue());
            for (String collectionField : collectionFields) {
                this.getXStream().addImplicitCollection(entry.getKey(),
                        collectionField);
            }
        }
    }

    /**
     * Specify omitted fields, as a Map consisting of <code>Class</code>
     * instances mapped to comma separated field names.
     *
     * @see XStream#omitField(Class, String)
     * @param omittedFields A map of fields to omit
     */
    public final void setOmittedFields(
            final Map<Class<?>, String> omittedFields) {
        for (Map.Entry<Class<?>, String> entry : omittedFields.entrySet()) {
            String[] fields = StringUtils.commaDelimitedListToStringArray(entry
                    .getValue());
            for (String field : fields) {
                this.getXStream().omitField(entry.getKey(), field);
            }
        }
    }

    /**
     * Set the classes for which mappings will be read from class-level JDK 1.5+
     * annotation metadata.
     *
     * @see XStream#processAnnotations(Class)
     * @param annotatedClass A class to process
     */
    public final void setAnnotatedClass(final Class<?> annotatedClass) {
        Assert.notNull(annotatedClass, "'annotatedClass' must not be null");
        this.getXStream().processAnnotations(annotatedClass);
    }

    /**
     * Set annotated classes for which aliases will be read from class-level JDK
     * 1.5+ annotation metadata.
     *
     * @see XStream#processAnnotations(Class[])
     * @param annotatedClasses A list of classes to process
     */
    public final void setAnnotatedClasses(final Class<?>[] annotatedClasses) {
        Assert.notEmpty(annotatedClasses,
                "'annotatedClasses' must not be empty");
        this.getXStream().processAnnotations(annotatedClasses);
    }

    /**
     * Set the auto-detection mode of XStream.
     * <p>
     * <strong>Note</strong> that auto-detection implies that the XStream is
     * configured while it is processing the XML steams, and thus introduces a
     * potential concurrency problem.
     *
     * @see XStream#autodetectAnnotations(boolean)
     * @param newAutodetectAnnotations Turn on annotation autodetection
     */
    public final void setAutodetectAnnotations(
            final boolean newAutodetectAnnotations) {
        this.autodetectAnnotations = newAutodetectAnnotations;
        this.getXStream().autodetectAnnotations(newAutodetectAnnotations);
    }

    /**
     * Set the XStream hierarchical stream driver to be used with stream readers
     * and writers.
     *
     * @param newStreamDriver Set the streamDriver
     */
    public final void setStreamDriver(
            final HierarchicalStreamDriver newStreamDriver) {
        this.streamDriver = newStreamDriver;
    }

    /**
     * Set the reflection provider used.
     *
     * @param newReflectionProvider the reflectionProvider
     */
    public final void setReflectionProvider(
            final ReflectionProvider newReflectionProvider) {
        this.reflectionProvider = newReflectionProvider;
    }

    /**
     * Set the encoding to be used for stream access.
     *
     * @see #DEFAULT_ENCODING
     * @param newEncoding the encoding of the stream
     */
    public final void setEncoding(final String newEncoding) {
        this.encoding = newEncoding;
    }

    /**
     * Set the classes supported by this marshaller.
     * <p>
     * If this property is empty (the default), all classes are supported.
     *
     * @see #supports(Class)
     * @param newSupportedClasses the classes supported by this marshaller
     */
    public final void setSupportedClasses(final Class[] newSupportedClasses) {
        this.supportedClasses = newSupportedClasses;
    }

    /**
     *
     * @throws Exception if there is an exception customizing the xstream
     */
    public final void afterPropertiesSet() throws Exception {
        if (reflectionProvider != null) {
            if (streamDriver != null) {
              xstream = new XStream(reflectionProvider, streamDriver);
            } else {
              xstream = new XStream(reflectionProvider);
            }
            xstream.autodetectAnnotations(autodetectAnnotations);
            for (int i = 0; i < converters.length; i++) {
                if (converters[i] instanceof Converter) {
                    this.getXStream().registerConverter(
                            (Converter) converters[i], i);
                } else if (converters[i] instanceof SingleValueConverter) {
                    this.getXStream().registerConverter(
                            (SingleValueConverter) converters[i], i);
                } else {
                    throw new IllegalArgumentException(
                        "Invalid ConverterMatcher [" + converters[i] + "]");
                }
            }
        }

        customizeXStream(getXStream());
    }

    /**
     *
     * @param newClassLoader Set the classloader
     */
    public final void setBeanClassLoader(final ClassLoader newClassLoader) {
        this.classLoader = newClassLoader;
    }

    /**
     * Template to allow for customizing of the given {@link XStream}.
     * <p>
     * Default implementation is empty.
     *
     * @param newXStream
     *            the {@code XStream} instance
     */
    protected void customizeXStream(final XStream newXStream) {
    }

    /**
     *
     * @param clazz The class to test
     * @return true if the marshaller supports this class or false otherwise
     */
    public final boolean supports(final Class clazz) {
        if (ObjectUtils.isEmpty(this.supportedClasses)) {
            return true;
        } else {
            for (Class supportedClass : this.supportedClasses) {
                if (supportedClass.isAssignableFrom(clazz)) {
                    return true;
                }
            }
            return false;
        }
    }

    // Marshalling

    @Override
    protected final void marshalDomNode(final Object graph, final Node node) {
        HierarchicalStreamWriter streamWriter;
        if (node instanceof Document) {
            streamWriter = new DomWriter((Document) node);
        } else if (node instanceof Element) {
            streamWriter = new DomWriter((Element) node,
                    node.getOwnerDocument(), new XmlFriendlyReplacer());
        } else {
            throw new IllegalArgumentException(
                    "DOMResult contains neither Document nor Element");
        }
        marshal(graph, streamWriter);
    }

    @Override
    protected final void marshalXmlEventWriter(final Object graph,
            final XMLEventWriter eventWriter) {
        ContentHandler contentHandler = StaxUtils
                .createContentHandler(eventWriter);
        marshalSaxHandlers(graph, contentHandler, null);
    }

    @Override
    protected final void marshalXmlStreamWriter(final Object graph,
            final XMLStreamWriter streamWriter) {
        try {
            marshal(graph, new StaxWriter(new QNameMap(), streamWriter));
        } catch (XMLStreamException ex) {
            throw convertXStreamException(ex, true);
        }
    }

    @Override
    protected final void marshalOutputStream(
            final Object graph, final OutputStream outputStream)
            throws IOException {
        marshalWriter(graph,
                new OutputStreamWriter(outputStream, this.encoding));
    }

    @Override
    protected final void marshalSaxHandlers(final Object graph,
            final ContentHandler contentHandler,
            final LexicalHandler lexicalHandler) {

        SaxWriter saxWriter = new SaxWriter();
        saxWriter.setContentHandler(contentHandler);
        marshal(graph, saxWriter);
    }

    @Override
    protected final void marshalWriter(final Object graph, final Writer writer)
            throws IOException {
        if (streamDriver != null) {
            marshal(graph, streamDriver.createWriter(writer));
        } else {
            marshal(graph, new CompactWriter(writer));
        }
    }

    /**
     * Marshals the given graph to the given XStream HierarchicalStreamWriter.
     * Converts exceptions using {@link #convertXStreamException}.
     *
     * @param graph The object to marshal
     * @param streamWriter the stream writer to write to
     */
    private void marshal(final Object graph,
            final HierarchicalStreamWriter streamWriter) {
        try {
            getXStream().marshal(graph, streamWriter);
        } catch (Exception ex) {
            throw convertXStreamException(ex, true);
        } finally {
            try {
                streamWriter.flush();
            } catch (Exception ex) {
                logger.debug("Could not flush HierarchicalStreamWriter", ex);
            }
        }
    }

    // Unmarshalling

    @Override
    protected final Object unmarshalDomNode(final Node node) {
        HierarchicalStreamReader streamReader;
        if (node instanceof Document) {
            streamReader = new DomReader((Document) node);
        } else if (node instanceof Element) {
            streamReader = new DomReader((Element) node);
        } else {
            throw new IllegalArgumentException(
                    "DOMSource contains neither Document nor Element");
        }
        return unmarshal(streamReader);
    }

    @Override
    protected final Object unmarshalXmlEventReader(
            final XMLEventReader eventReader) {
        try {
            XMLStreamReader streamReader = StaxUtils
                    .createEventStreamReader(eventReader);
            return unmarshalXmlStreamReader(streamReader);
        } catch (XMLStreamException ex) {
            throw convertXStreamException(ex, false);
        }
    }

    @Override
    protected final Object unmarshalXmlStreamReader(
            final XMLStreamReader streamReader) {
        if (qNameMap == null) {
            return unmarshal(new StaxReader(new QNameMap(), streamReader));
        } else {
            return unmarshal(new StaxReader(qNameMap, streamReader));
        }
    }

    @Override
    protected final Object unmarshalInputStream(final InputStream inputStream)
            throws IOException {
        return unmarshalReader(
                new InputStreamReader(inputStream, this.encoding));
    }

    @Override
    protected final Object unmarshalReader(final Reader reader)
           throws IOException {
        if (streamDriver != null) {
            return unmarshal(streamDriver.createReader(reader));
        } else {
            return unmarshal(new XppReader(reader));
        }
    }

    @Override
    protected final Object unmarshalSaxReader(final XMLReader xmlReader,
            final InputSource inputSource) throws IOException {

        throw new UnsupportedOperationException(
                  "XStreamMarshaller does not support"
                + " unmarshalling using SAX XMLReaders");
    }

    /**
     *
     * @param streamReader The stream reader to use
     * @return the unmarshalled object
     */
    private Object unmarshal(final HierarchicalStreamReader streamReader) {
        try {
            return this.getXStream().unmarshal(streamReader);
        } catch (Exception ex) {
            throw convertXStreamException(ex, false);
        }
    }

    /**
     * Convert the given XStream exception to an appropriate exception from the
     * <code>org.springframework.oxm</code> hierarchy.
     * <p>
     * A boolean flag is used to indicate whether this exception occurs during
     * marshalling or unmarshalling, since XStream itself does not make this
     * distinction in its exception hierarchy.
     *
     * @param ex
     *            XStream exception that occured
     * @param marshalling
     *            indicates whether the exception occurs during marshalling (
     *            <code>true</code>), or unmarshalling (<code>false</code>)
     * @return the corresponding <code>XmlMappingException</code>
     */
    protected final XmlMappingException convertXStreamException(
            final Exception ex, final boolean marshalling) {
        if (ex instanceof StreamException
                || ex instanceof CannotResolveClassException
                || ex instanceof ConversionException) {
            if (marshalling) {
                return new MarshallingFailureException(
                        "XStream marshalling exception", ex);
            } else {
                return new UnmarshallingFailureException(
                        "XStream unmarshalling exception", ex);
            }
        } else {
            // fallback
            return new UncategorizedMappingException(
                    "Unknown XStream exception", ex);
        }
    }
}