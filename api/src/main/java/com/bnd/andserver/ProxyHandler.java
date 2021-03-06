
package com.bnd.andserver;

import com.bnd.andserver.error.NotFoundException;
import com.bnd.andserver.server.ProxyServer;
import com.bnd.andserver.util.IOUtils;

import org.apache.httpcore.HttpException;
import org.apache.httpcore.HttpHeaders;
import org.apache.httpcore.HttpHost;
import org.apache.httpcore.HttpRequest;
import org.apache.httpcore.HttpResponse;
import org.apache.httpcore.entity.StringEntity;
import org.apache.httpcore.impl.DefaultBHttpClientConnection;
import org.apache.httpcore.impl.DefaultConnectionReuseStrategy;
import org.apache.httpcore.protocol.HttpContext;
import org.apache.httpcore.protocol.HttpCoreContext;
import org.apache.httpcore.protocol.HttpProcessor;
import org.apache.httpcore.protocol.HttpRequestExecutor;
import org.apache.httpcore.protocol.HttpRequestHandler;
import org.apache.httpcore.protocol.ImmutableHttpProcessor;
import org.apache.httpcore.protocol.RequestConnControl;
import org.apache.httpcore.protocol.RequestContent;
import org.apache.httpcore.protocol.RequestExpectContinue;
import org.apache.httpcore.protocol.RequestTargetHost;
import org.apache.httpcore.protocol.RequestUserAgent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static com.bnd.andserver.server.ProxyServer.PROXY_CONN_CLIENT;

public class ProxyHandler implements HttpRequestHandler {

    private static final int BUFFER = 8 * 1024;

    private final static Set<String> HOP_BY_HOP = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        HttpHeaders.HOST,
        HttpHeaders.CONTENT_LENGTH,
        HttpHeaders.TRANSFER_ENCODING,
        HttpHeaders.CONNECTION,
        HttpHeaders.PROXY_AUTHENTICATE,
        HttpHeaders.TE,
        HttpHeaders.TRAILER,
        HttpHeaders.UPGRADE
    )));

    private final Map<String, HttpHost> mHostList;

    private final SSLSocketFactory mSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

    private final HttpRequestExecutor mHttpExecutor = new HttpRequestExecutor();
    private final HttpProcessor mRequestProcessor = new ImmutableHttpProcessor(
        new RequestContent(),
        new RequestTargetHost(),
        new RequestConnControl(),
        new RequestUserAgent(AndServer.INFO),
        new RequestExpectContinue(true));

    public ProxyHandler(Map<String, HttpHost> hostList) {
        this.mHostList = hostList;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
        throws HttpException, IOException {
        String hostHeader = request.getFirstHeader(HttpHeaders.HOST).getValue();
        String hostName = HttpHost.create(hostHeader).getHostName();
        HttpHost host = mHostList.get(hostName.toLowerCase(Locale.ROOT));
        if (host == null) {
            NotFoundException e = new NotFoundException(request.getRequestLine().getUri());
            response.setStatusCode(e.getStatusCode());
            response.setEntity(new StringEntity(e.getMessage()));
            return;
        }

        // Remove hop-by-hop headers.
        for (String name: HOP_BY_HOP) {
            request.removeHeaders(name);
        }

        DefaultBHttpClientConnection conn = (DefaultBHttpClientConnection) context.getAttribute(PROXY_CONN_CLIENT);
        if (!conn.isOpen() || conn.isStale()) {
            Socket socket = createSocket(host);
            conn.bind(socket);
        }

        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, conn);
        context.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, host);

        mHttpExecutor.preProcess(request, mRequestProcessor, context);
        HttpResponse outResponse = mHttpExecutor.execute(request, conn, context);
        mHttpExecutor.postProcess(response, mRequestProcessor, context);

        // Remove hop-by-hop headers.
        for (String name: HOP_BY_HOP) {
            outResponse.removeHeaders(name);
        }

        response.setStatusLine(outResponse.getStatusLine());
        response.setHeaders(outResponse.getAllHeaders());
        response.setEntity(outResponse.getEntity());

        boolean keepAlive = DefaultConnectionReuseStrategy.INSTANCE.keepAlive(response, context);
        context.setAttribute(ProxyServer.PROXY_CONN_ALIVE, keepAlive);
    }

    private Socket createSocket(HttpHost host) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(60 * 1000);
        socket.setReuseAddress(true);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setReceiveBufferSize(BUFFER);
        socket.setSendBufferSize(BUFFER);
        socket.setSoLinger(true, 0);

        String scheme = host.getSchemeName();
        String hostName = host.getHostName();
        int port = host.getPort();

        InetSocketAddress address = resolveAddress(scheme, hostName, port);
        socket.connect(address, 10 * 1000);

        if ("https".equalsIgnoreCase(scheme)) {
            SSLSocket sslSocket = (SSLSocket) mSocketFactory.createSocket(socket, hostName, port, true);
            try {
                sslSocket.startHandshake();
                final SSLSession session = sslSocket.getSession();
                if (session == null) {
                    throw new SSLHandshakeException("SSL session not available.");
                }
            } catch (final IOException ex) {
                IOUtils.closeQuietly(sslSocket);
                throw ex;
            }
            return sslSocket;
        }
        return socket;
    }

    private InetSocketAddress resolveAddress(String scheme, String hostName, int port) {
        if (port < 0) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }
        return new InetSocketAddress(hostName, port);
    }
}