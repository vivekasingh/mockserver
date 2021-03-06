package org.mockserver.client.server;

import org.apache.commons.lang3.StringUtils;
import org.mockserver.client.http.ApacheHttpClient;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.client.serialization.HttpRequestSerializer;
import org.mockserver.matchers.Times;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;

/**
 * @author jamesdbloom
 */
public class MockServerClient {
    private final String uriBase;
    private ApacheHttpClient apacheHttpClient;
    private HttpRequestSerializer httpRequestSerializer = new HttpRequestSerializer();
    private ExpectationSerializer expectationSerializer = new ExpectationSerializer();

    /**
     * Start the client communicating to a MockServer at the specified host and port
     * for example:
     *
     *   MockServerClient mockServerClient = new MockServerClient("localhost", 8080);
     *
     * @param host the host for the MockServer to communicate with
     * @param port the port for the MockServer to communicate with
     */
    public MockServerClient(String host, int port) {
        this(host, port, "");
    }

    /**
     * Start the client communicating to a MockServer at the specified host and port
     * and contextPath for example:
     *
     *   MockServerClient mockServerClient = new MockServerClient("localhost", 8080, "/mockserver");
     *
     * @param host the host for the MockServer to communicate with
     * @param port the port for the MockServer to communicate with
     * @param contextPath the context path that the MockServer war is deployed to
     */
    public MockServerClient(String host, int port, String contextPath) {
        if (StringUtils.isEmpty(host)) throw new IllegalArgumentException("Host can not be null or empty");
        if (contextPath == null) throw new IllegalArgumentException("ContextPath can not be null");
        uriBase = "http://" + host + ":" + port + (contextPath.length() > 0 && !contextPath.startsWith("/") ? "/" : "") + contextPath;
        apacheHttpClient = new ApacheHttpClient();
    }

    /**
     * Specify an unlimited expectation that will respond regardless of the number of matching http
     * for example:
     *
     *   mockServerClient
     *           .when(
     *                   request()
     *                           .withPath("/some_path")
     *                           .withBody("some_request_body")
     *           )
     *           .respond(
     *                   request()
     *                           .withBody("some_response_body")
     *                           .withHeaders(
     *                                   new Header("responseName", "responseValue")
     *                           )
     *           );
     *
     * @param httpRequest the http request that must be matched for this expectation to respond
     * @return an Expectation object that can be used to specify the response
     */
    public ForwardChainExpectation when(HttpRequest httpRequest) {
        return when(httpRequest, Times.unlimited());
    }

    /**
     * Specify an limited expectation that will respond a specified number of times when the http is matched
     * for example:
     *
     *   mockServerClient
     *           .when(
     *                   new HttpRequest()
     *                           .withPath("/some_path")
     *                           .withBody("some_request_body"),
     *                   Times.exactly(5)
     *           )
     *           .respond(
     *                   new HttpResponse()
     *                           .withBody("some_response_body")
     *                           .withHeaders(
     *                                   new Header("responseName", "responseValue")
     *                           )
     *           );
     *
     * @param httpRequest the http request that must be matched for this expectation to respond
     * @param times       the number of times to respond when this http is matched
     * @return an Expectation object that can be used to specify the response
     */
    public ForwardChainExpectation when(HttpRequest httpRequest, Times times) {
        return new ForwardChainExpectation(this, new Expectation(httpRequest, times));
    }

    /**
     * Pretty-print the json for all expectations to the log.  They are printed into a dedicated log called mockserver_request.log
     */
    public MockServerClient dumpToLog() {
        dumpToLog(null);
        return this;
    }

    /**
     * Pretty-print the json for all expectations that match the request to the log.  They are printed into a dedicated log called mockserver_request.log
     *
     * @param httpRequest the http request that is matched against when deciding what to log if null all requests are logged
     */
    public MockServerClient dumpToLog(HttpRequest httpRequest) {
        apacheHttpClient.sendPUTRequest(uriBase, "/dumpToLog", httpRequest != null ? httpRequestSerializer.serialize(httpRequest) : "");
        return this;
    }

    /**
     * Reset MockServer by clearing all expectations
     */
    public MockServerClient reset() {
        apacheHttpClient.sendPUTRequest(uriBase, "/reset", "");
        return this;
    }

    /**
     * Clear all expectations that match the http
     *
     * @param httpRequest the http request that is matched against when deciding whether to clear each expectation if null all expectations are cleared
     */
    public MockServerClient clear(HttpRequest httpRequest) {
        apacheHttpClient.sendPUTRequest(uriBase, "/clear", httpRequest != null ? httpRequestSerializer.serialize(httpRequest) : "");
        return this;
    }

    protected void sendExpectation(Expectation expectation) {
        apacheHttpClient.sendPUTRequest(uriBase, "/", expectation != null ? expectationSerializer.serialize(expectation) : "");
    }

}
