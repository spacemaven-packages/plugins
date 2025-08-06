package net.derfruhling.magictests


import org.gradle.api.tasks.testing.GroupTestEventReporter
import org.gradle.api.tasks.testing.TestEventReporter
import org.gradle.api.tasks.testing.TestOutputEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.ClosedChannelException
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

class GTestStreamingReceiver implements AutoCloseable {
    private final AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open()
    private int currentTestIteration = -1
    private String currentTestGroup = null
    private String currentTestName = null

    private static final VERSION = '1.0'

    private class Done extends RuntimeException {}

    GTestStreamingReceiver() {
        server.bind(null)
    }

    int getPort() {
        return (server.localAddress as InetSocketAddress).port
    }

    void accept(GroupTestEventReporter reporter, Runnable afterStarting) {
        Logger logger = LoggerFactory.getLogger(GTestStreamingReceiver)
        var groupStack = [reporter]
        var tasksStack = []
        List<Map<String, String>> partResults = []
        TestEventReporter currentReporter = null

        def acceptFuture = server.accept()
        afterStarting.run()
        try(var socket = acceptFuture.get(10, TimeUnit.SECONDS)) {
            var buffer = ByteBuffer.allocate(1024)
            var string = new String()

            while(socket.isOpen()) {
                socket.read(buffer).get()
                string += StandardCharsets.UTF_8.decode(buffer.flip()).toString()

                while(string.contains('\n')) {
                    String option
                    (option, string) = string.split('\n', 2)

                    Map<String, String> values = [:]
                    for (final def part in option.split('&')) {
                        def (String key, String value) = part.split('=', 2)
                        values.put(key, URLDecoder.decode(value, StandardCharsets.UTF_8))
                    }

                    if(values.isEmpty()) continue

                    if(values.containsKey('gtest_streaming_protocol_version')) {
                        String version = values.gtest_streaming_protocol_version
                        if(version != VERSION) {
                            logger.warn('This receiver is not equipped to handle GTest streaming protocol version ' + version)
                        }
                    } else {
                        switch(values.event) {
                            case 'TestProgramStart':
                                break
                            case 'TestProgramEnd':
                                throw new Done()
                            case 'TestIterationStart':
                                currentTestIteration = values.iteration.toInteger()
                                break
                            case 'TestIterationEnd':
                                break
                            case 'TestCaseStart': // test case is a legacy name for test suites
                                currentTestGroup = values.name
                                groupStack << groupStack.last.reportTestGroup(currentTestGroup)
                                groupStack.last.started(Instant.now())
                                tasksStack << []
                                break
                            case 'TestCaseEnd':
                                currentTestGroup = null
                                var group = groupStack.removeLast()

                                if(values.passed.toInteger() > 0) {
                                    group.succeeded(Instant.now())
                                } else {
                                    group.failed(Instant.now())
                                }

                                group.close()
                                break
                            case 'TestStart':
                                currentTestName = values.name
                                currentReporter = groupStack.last.reportTest(values.name, currentTestGroup + '.' + currentTestName)
                                currentReporter.started(Instant.now())
                                partResults.clear()
                                break
                            case 'TestPartResult':
                                partResults << values
                                currentReporter.output(Instant.now(), TestOutputEvent.Destination.StdErr, "${values.file}:${values.line}:\n${values.message}")
                                break
                            case 'TestEnd':
                                if(values.containsKey('skipped') && values.skipped.toInteger() > 0) {
                                    // this is technically non-standard
                                    // (gtest does not have a skipped property here)
                                    // but i might want to add one so might as well
                                    currentReporter.skipped(Instant.now())
                                } else if(values.passed.toInteger() > 0) {
                                    // passed
                                    currentReporter.succeeded(Instant.now())
                                } else {
                                    def last = partResults.getLast()
                                    currentReporter.failed(
                                            Instant.now(),
                                            "Failed at ${last.file}:${last.line}",
                                            partResults.subList(0, partResults.size() - 1).collect {
                                                "${it.file}:${it.line}:\n${it.message}"
                                            }.join('\n')
                                    )
                                }
                                currentReporter.close()
                                currentReporter = null
                                currentTestName = null
                                break
                            default:
                                logger.warn("Not handling event: {}", values)
                        }
                    }
                }

                buffer.clear()
            }
        } catch (Done ignored) {

        } catch (ClosedChannelException ignored) {

        }
    }

    @Override
    void close() throws Exception {
        server.close()
    }
}
