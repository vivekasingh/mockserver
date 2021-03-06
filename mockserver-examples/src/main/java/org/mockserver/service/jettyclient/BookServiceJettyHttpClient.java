package org.mockserver.service.jettyclient;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpScheme;
import org.mockserver.model.Book;
import org.mockserver.service.BookService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * @author jamesdbloom
 */
@Component
public class BookServiceJettyHttpClient implements BookService {

    @Resource
    private Environment environment;
    private Integer port;
    private Integer proxyPort;
    private String host;
    private ObjectMapper objectMapper;
    private HttpClient httpClient;

    @PostConstruct
    private void initialise() {
        port = environment.getProperty("bookService.port", Integer.class);
        proxyPort = environment.getProperty("bookService.proxyPort", Integer.class);
        host = environment.getProperty("bookService.host", "localhost");
        objectMapper = createObjectMapper();
        httpClient = createHttpClient();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // ignore failures
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_NUMBERS_FOR_ENUMS, false);
        // relax parsing
        objectMapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationConfig.Feature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        // use arrays
        objectMapper.configure(DeserializationConfig.Feature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        // remove empty values from JSON
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT);
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
        return objectMapper;
    }

    private HttpClient createHttpClient() {
        HttpClient httpClient = new HttpClient();
        try {
            if (Boolean.parseBoolean(System.getProperty("proxySet"))) {
                httpClient.getProxyConfiguration().getProxies().add(new HttpProxy(System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort"))));
            }
            httpClient.start();
        } catch (Exception e) {
            throw new RuntimeException("Exception creating HttpClient", e);
        }
        return httpClient;
    }

    @PreDestroy
    private void stopClient() {
        try {
            httpClient.stop();
        } catch (Exception e) {
            throw new RuntimeException("Exception stopping HttpClient", e);
        }
    }

    @Override
    public Book[] getAllBooks() {
        try {
            ContentResponse response = httpClient.newRequest(host, port)
                    .scheme(HttpScheme.HTTP.asString())
                    .method(HttpMethod.GET)
                    .path("/get_books")
                    .send();
            return objectMapper.readValue(response.getContent(), Book[].class);
        } catch (Exception e) {
            throw new RuntimeException("Exception making request to retrieve all books", e);
        }
    }

    @Override
    public Book getBook(String id) {
        try {
            ContentResponse response = httpClient.newRequest(host, port)
                    .scheme(HttpScheme.HTTP.asString())
                    .method(HttpMethod.GET)
                    .path("/get_book")
                    .param("id", id)
                    .send();
            return objectMapper.readValue(response.getContent(), Book.class);
        } catch (Exception e) {
            throw new RuntimeException("Exception making request to retrieve a book with id [" + id + "]", e);
        }
    }
}
