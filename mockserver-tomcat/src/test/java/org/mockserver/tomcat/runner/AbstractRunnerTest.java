package org.mockserver.tomcat.runner;

import org.junit.Before;
import org.junit.Test;
import org.mockserver.server.MockServerServlet;
import org.mockserver.socket.PortFactory;
import org.mockserver.tomcat.server.MockServerRunner;

import javax.servlet.http.HttpServlet;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * @author jamesdbloom
 */
public class AbstractRunnerTest {

    private final int port = PortFactory.findFreePort();
    private final int stopPort = PortFactory.findFreePort();
    private AbstractRunner runner;

    @Before
    public void createRunner() {
        runner = new AbstractRunner() {
            protected HttpServlet getServlet() {
                return new MockServerServlet();
            }

            @Override
            protected int stopPort(Integer port, Integer securePort) {
                return stopPort;
            }
        };
    }

    @Test
    public void shouldStartOnNonSecurePortOnly() throws InterruptedException, ExecutionException, UnknownHostException {
        try {
            // when
            runner.start(port, null);

            // then
            assertEquals("localhost", runner.tomcat.getServer().getAddress());
            assertEquals("Connector[HTTP/1.1-" + port + "]", runner.tomcat.getConnector().toString());
        } finally {
            runner.stop();
        }
    }

    @Test
    public void shouldStartOnSecurePortOnly() throws InterruptedException, ExecutionException, UnknownHostException {
        try {
            // when
            runner.start(null, port);

            // then
            assertEquals("localhost", runner.tomcat.getServer().getAddress());
            assertEquals("Connector[HTTP/1.1-" + port + "]", runner.tomcat.getConnector().toString());
        } finally {
            runner.stop();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotStartForBothPortsNull() {
        // when
        runner.start(null, null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotStartTwice() throws InterruptedException, ExecutionException, UnknownHostException {
        try {
            // when
            runner.start(port, null);
            runner.start(port + 1, null);
        } finally {
            runner.stop();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotStopIfNotStarted() {
        runner.stop();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotStopTwice() throws InterruptedException, ExecutionException, UnknownHostException {
        try {
            // when
            runner.start(port, null);
            runner.stop();
        } finally {
            runner.stop();
        }
    }

    @Test
    public void shouldStopRemoteServerAndWaitForResponse() throws InterruptedException, ExecutionException, UnknownHostException {
        try {
            // when
            runner.start(port, null);

            // then
            assertTrue(new MockServerRunner().stop("127.0.0.1", stopPort, 30));
        } finally {
            try {
                runner.stop();
            } catch (RuntimeException re) {
                // do nothing only here in case stop above fails
            }
        }
    }

    @Test
    public void shouldStopRemoteServerAndNotWaitForResponse() throws InterruptedException, ExecutionException, UnknownHostException {
        try {
            // when
            runner.start(port, null);

            // then
            assertTrue(new MockServerRunner().stop("127.0.0.1", stopPort, 0));
        } finally {
            try {
                runner.stop();
            } catch (RuntimeException re) {
                // do nothing only here in case stop above fails
            }
        }
    }

    @Test
    public void shouldIndicateIfCanNotStopRemoteServer() throws InterruptedException, ExecutionException, UnknownHostException {
        // when
        runner.start(port, null);
        new MockServerRunner().stop("127.0.0.1", stopPort, 5);

        // then
        assertFalse(new MockServerRunner().stop("127.0.0.1", stopPort, 3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldValidatePortArgument() {
        new MockServerRunner().stop("127.0.0.1", -1, 5);
    }
}
