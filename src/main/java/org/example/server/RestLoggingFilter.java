package org.example.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

@PreMatching
@Priority(Integer.MIN_VALUE)
public class RestLoggingFilter
        implements ContainerRequestFilter, ClientRequestFilter, ContainerResponseFilter,
        ClientResponseFilter, WriterInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(RestLoggingFilter.class);
    private static final String ENTITY_LOGGER_PROPERTY =
            RestLoggingFilter.class.getName() + ".entityLogger";

    private static final String NOTIFICATION_PREFIX = "* ";
    private static final String REQUEST_PREFIX = "> ";
    private static final String RESPONSE_PREFIX = "< ";

    private static final Comparator<Map.Entry<String, List<String>>> COMPARATOR =
            (o1, o2) -> o1.getKey().compareToIgnoreCase(o2.getKey());

    private static final int DEFAULT_MAX_ENTITY_SIZE = 8 * 1024;

    private final AtomicLong _id = new AtomicLong(0);
    private final boolean printEntity;
    private final int maxEntitySize;
    private final String logKey;

    /**
     * Create a logging filter with custom logger and custom settings of entity logging.
     *
     * @param printEntity if true, entity will be logged as well up to the default maxEntitySize,
     *                    which is 8KB
     */
    public RestLoggingFilter(String logKey, final boolean printEntity) {
        this.logKey = logKey;
        this.printEntity = printEntity;
        this.maxEntitySize = DEFAULT_MAX_ENTITY_SIZE;
    }

    /**
     * Creates a logging filter with custom logger and entity logging turned on, but potentially
     * limiting the size of entity to be buffered and logged.
     *
     * @param maxEntitySize maximum number of entity bytes to be logged (and buffered) - if the
     *                      entity is larger, logging filter will print (and buffer in memory) only
     *                      the specified number of bytes and print "...more..." string at the end.
     */
    public RestLoggingFilter(String logKey, final int maxEntitySize) {
        this.logKey = logKey;
        this.printEntity = true;
        this.maxEntitySize = maxEntitySize;
    }

    private void log(final StringBuilder b) {
        LOG.info("{}: {}", logKey, b);
    }

    private StringBuilder prefixId(final StringBuilder b, final long id) {
        b.append(id).append(' ');
        return b;
    }

    private void printRequestLine(final StringBuilder b, final String note, final long id,
            final String method, final URI uri) {
        prefixId(b, id)
                .append(NOTIFICATION_PREFIX)
                .append(note)
                .append(" on thread ")
                .append(Thread.currentThread().getName())
                .append('\n');
        prefixId(b, id)
                .append(REQUEST_PREFIX)
                .append(method)
                .append(' ')
                .append(uri.toASCIIString())
                .append('\n');
    }

    private void printResponseLine(final StringBuilder b, final String note, final long id,
            final int status) {
        prefixId(b, id)
                .append(NOTIFICATION_PREFIX)
                .append(note)
                .append(" on thread ")
                .append(Thread.currentThread().getName())
                .append('\n');
        prefixId(b, id).append(RESPONSE_PREFIX).append(status).append('\n');
    }

    private void printPrefixedHeaders(final StringBuilder b, final long id, final String prefix,
            final MultivaluedMap<String, String> headers) {
        for (final Map.Entry<String, List<String>> headerEntry : getSortedHeaders(
                headers.entrySet())) {
            final List<?> val = headerEntry.getValue();
            final String header = headerEntry.getKey();

            if (val.size() == 1) {
                prefixId(b, id)
                        .append(prefix)
                        .append(header)
                        .append(": ")
                        .append(val.get(0))
                        .append('\n');
            } else {
                final StringBuilder sb = new StringBuilder();
                boolean add = false;
                for (final Object s : val) {
                    if (add) {
                        sb.append(',');
                    }
                    add = true;
                    sb.append(s);
                }
                prefixId(b, id)
                        .append(prefix)
                        .append(header)
                        .append(": ")
                        .append(sb.toString())
                        .append('\n');
            }
        }
    }

    private Set<Map.Entry<String, List<String>>> getSortedHeaders(
            final Set<Map.Entry<String, List<String>>> headers) {
        final TreeSet<Map.Entry<String, List<String>>> sortedHeaders = new TreeSet<>(COMPARATOR);
        sortedHeaders.addAll(headers);
        return sortedHeaders;
    }

    private InputStream logInboundEntity(final StringBuilder b, InputStream stream)
            throws IOException {
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        stream.mark(maxEntitySize + 1);
        final byte[] entity = new byte[maxEntitySize + 1];
        final int entitySize = stream.read(entity);
        b.append(new String(entity, 0, Math.min(entitySize, maxEntitySize)));
        if (entitySize > maxEntitySize) {
            b.append("...more...");
        }
        b.append('\n');
        stream.reset();
        return stream;
    }

    @Override
    public void filter(final ClientRequestContext context) {
        final long id = this._id.incrementAndGet();
        final StringBuilder b = new StringBuilder();

        printRequestLine(b, "Sending client request", id, context.getMethod(), context.getUri());
        printPrefixedHeaders(b, id, REQUEST_PREFIX, context.getStringHeaders());

        if (printEntity && context.hasEntity()) {
            final OutputStream stream = new LoggingStream(b, context.getEntityStream());
            context.setEntityStream(stream);
            context.setProperty(ENTITY_LOGGER_PROPERTY, stream);
            // not calling log(b) here - it will be called by the interceptor
        } else {
            log(b);
        }
    }

    @Override
    public void filter(final ClientRequestContext requestContext,
            final ClientResponseContext responseContext) throws IOException {
        final long id = this._id.incrementAndGet();
        final StringBuilder b = new StringBuilder();

        printResponseLine(b, "Client response received", id, responseContext.getStatus());
        printPrefixedHeaders(b, id, RESPONSE_PREFIX, responseContext.getHeaders());

        if (printEntity && responseContext.hasEntity()) {
            responseContext.setEntityStream(logInboundEntity(b, responseContext.getEntityStream()));
        }

        log(b);
    }

    @Override
    public void filter(final ContainerRequestContext context) throws IOException {
        final long id = this._id.incrementAndGet();
        final StringBuilder b = new StringBuilder();

        printRequestLine(b, "Server has received a request", id, context.getMethod(),
                context.getUriInfo().getRequestUri());
        printPrefixedHeaders(b, id, REQUEST_PREFIX, context.getHeaders());

        if (printEntity && context.hasEntity()) {
            context.setEntityStream(logInboundEntity(b, context.getEntityStream()));
        }

        log(b);
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext) {
        final long id = this._id.incrementAndGet();
        final StringBuilder b = new StringBuilder();

        printResponseLine(b, "Server responded with a response", id, responseContext.getStatus());
        printPrefixedHeaders(b, id, RESPONSE_PREFIX, responseContext.getStringHeaders());

        if (printEntity && responseContext.hasEntity()) {
            final OutputStream stream = new LoggingStream(b, responseContext.getEntityStream());
            responseContext.setEntityStream(stream);
            requestContext.setProperty(ENTITY_LOGGER_PROPERTY, stream);
            // not calling log(b) here - it will be called by the interceptor
        } else {
            log(b);
        }
    }

    @Override
    public void aroundWriteTo(final WriterInterceptorContext writerInterceptorContext)
            throws IOException {
        final LoggingStream stream =
                (LoggingStream) writerInterceptorContext.getProperty(ENTITY_LOGGER_PROPERTY);
        writerInterceptorContext.proceed();
        if (stream != null) {
            log(stream.getStringBuilder());
        }
    }

    /**
     * Check if this class's log level is at least INFO, if not then there's no point to install the
     * logger and it'll just cause unnecessary overhead
     */
    public boolean testLoggingEnabled() {
        if (!LoggerFactory.getLogger(RestLoggingFilter.class).isInfoEnabled()) {
            LOG.warn("To trace HTTP requests, INFO logging should be enabled for logger {}",
                    RestLoggingFilter.class.getName());
            return false;
        } else {
            return true;
        }
    }

    private class LoggingStream extends OutputStream {
        private final StringBuilder b;
        private final OutputStream inner;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        LoggingStream(final StringBuilder b, final OutputStream inner) {
            this.b = b;
            this.inner = inner;
        }

        synchronized StringBuilder getStringBuilder() {
            // write entity to the builder
            final byte[] entity = baos.toByteArray();

            b.append(new String(entity, 0, Math.min(entity.length, maxEntitySize)));
            if (entity.length > maxEntitySize) {
                b.append("...more...");
            }
            b.append('\n');

            return b;
        }

        @Override
        public synchronized void write(final int i) throws IOException {
            if (baos.size() <= maxEntitySize) {
                baos.write(i);
            }
            inner.write(i);
        }

        @Override
        public synchronized void write(byte[] b) throws IOException {
            inner.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            inner.write(b, off, len);
        }

        @Override
        public synchronized void flush() throws IOException {
            inner.flush();
        }

        @Override
        public void close() throws IOException {
            inner.close();
        }
    }

}
