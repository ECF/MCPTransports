/****************************************************************************
 * Copyright (c) 2025 Composent, Inc. 
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: Apache-2.0
 *****************************************************************************/
package com.composent.ai.mcp.transport.uds.impl;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composent.ai.mcp.transport.uds.UDSMcpServerConfig;
import com.composent.ai.mcp.transport.uds.UDSMcpServerTransportProvider;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCMessage;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.ProtocolVersions;
import io.modelcontextprotocol.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Component(scope = ServiceScope.PROTOTYPE, service = UDSMcpServerTransportProvider.class)
public class UDSMcpServerTransportProviderImpl implements UDSMcpServerTransportProvider {

	private static final Logger logger = LoggerFactory.getLogger(UDSMcpServerTransportProviderImpl.class);

	public static final int DEFAULT_BUFFER_SIZE = Integer
			.valueOf(System.getProperty("UDSMcpTransport.default_buffer_size", "4096"));

	// Required for serializing/deserializing json messages
	private McpJsonMapper objectMapper;
	// Required for configuring session/channel byte buffer size
	private int incomingBufferSize = DEFAULT_BUFFER_SIZE;
	// Required Path for UnixDomainSocket creation
	private Path targetAddress;
	// Determines whether the server allows new client to connect after previous
	// client disconnects
	private boolean restartSession = true;
	// Created/set in setSessionFactory
	private McpServerSession serverSession;
	// Created/set in setSessionFactory
	private UDSMcpSessionTransport sessionTransport;
	private ExecutorService executorService;
	private Selector selector;

	@Activate
	public UDSMcpServerTransportProviderImpl(@Reference UDSMcpServerConfig config) {
		this.objectMapper = McpJsonDefaults.getDefaultMcpJsonMapper();
		Assert.notNull(this.objectMapper, "objectMapper must not be null");
		this.targetAddress = config.getServerSocketPath();
		this.restartSession = config.isAutoRestartSession();
		this.executorService = config.getExecutorService();
		this.selector = config.getSelector();
	}

	@Override
	public Path getServerSocketPath() {
		return this.targetAddress;
	}

	@Reference
	protected void setMcpJsonDefaults(McpJsonDefaults json) {
	}

	@Deactivate
	protected void deactivate() {
		if (this.serverSession != null) {
			this.serverSession.close();
			this.serverSession = null;
		}
		if (this.targetAddress != null) {
			this.targetAddress.toFile().delete();
			this.targetAddress = null;
		}
	}

	@Override
	public List<String> protocolVersions() {
		return List.of(ProtocolVersions.MCP_2024_11_05);
	}

	@Override
	public void setSessionFactory(McpServerSession.Factory sessionFactory) {
		this.sessionTransport = new UDSMcpSessionTransport();
		this.serverSession = sessionFactory.create(sessionTransport);
		this.sessionTransport.initProcessing();
	}

	@Override
	public Mono<Void> notifyClients(String method, Object params) {
		if (this.serverSession == null) {
			return Mono.error(McpError.builder(-1).message("No uds acceptedClient to use for notifyClients").build());
		}
		return this.serverSession.sendNotification(method, params)
				.doOnError(e -> logger.error("Failed to send notification: {}", e.getMessage()));
	}

	@Override
	public Mono<Void> closeGracefully() {
		if (this.serverSession == null) {
			return Mono.empty();
		}
		this.restartSession = false;
		return this.serverSession.closeGracefully();
	}

	public boolean isClientConnected() {
		return (this.sessionTransport != null) ? this.sessionTransport.isClientConnected() : false;
	}

	private class UDSMcpSessionTransport implements McpServerTransport {

		private AtomicBoolean isClosing;

		private Sinks.Many<JSONRPCMessage> inboundSink;

		private Sinks.Many<JSONRPCMessage> outboundSink;

		private AtomicBoolean isStarted;

		private Sinks.One<Void> inboundReady;

		private Scheduler outboundScheduler;

		private Sinks.One<Void> outboundReady;

		private UDSServerStringChannel serverSocketChannel;

		private synchronized void initialize() {
			isClosing = new AtomicBoolean(false);
			isStarted = new AtomicBoolean(false);
			outboundReady = Sinks.one();
			inboundReady = Sinks.one();
			this.inboundSink = Sinks.many().unicast().onBackpressureBuffer();
			this.outboundSink = Sinks.many().unicast().onBackpressureBuffer();
			this.outboundScheduler = Schedulers
					.fromExecutorService(UDSMcpServerTransportProviderImpl.this.executorService, "uds-outbound");
		}

		public UDSMcpSessionTransport() {
			initialize();
		}

		public synchronized void handleMessage(McpSchema.JSONRPCMessage json) throws IOException {
			try {
				if (!this.inboundSink.tryEmitNext(json).isSuccess()) {
					throw new Exception("Failed to enqueue message");
				}
			} catch (Exception e) {
				logIfNotClosing("Error processing inbound message", e);
				throw new IOException("Error in processing inbound message", e);
			}
		}

		@Override
		public synchronized Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
			return Mono.zip(inboundReady.asMono(), outboundReady.asMono()).then(Mono.defer(() -> {
				if (outboundSink.tryEmitNext(message).isSuccess()) {
					return Mono.empty();
				} else {
					return Mono.error(new RuntimeException("Failed to enqueue message"));
				}
			}));
		}

		@Override
		public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
			return objectMapper.convertValue(data, typeRef);
		}

		@Override
		public Mono<Void> closeGracefully() {
			return Mono.fromRunnable(() -> {
				isClosing.set(true);
				logger.debug("Session transport closing gracefully");
				inboundSink.tryEmitComplete();
			});
		}

		@Override
		public synchronized void close() {
			if (logger.isDebugEnabled()) {
				logger.debug("Session transport closing");
			}
			isClosing.set(true);
			serverSocketChannel.close();
			if (logger.isDebugEnabled()) {
				logger.debug("Session transport closed");
			}
		}

		public synchronized boolean isClientConnected() {
			return isClosing.get() ? false : this.serverSocketChannel.isClientConnected();
		}

		private void initProcessing() {
			this.inboundSink.asFlux().flatMap(message1 -> serverSession.handle(message1)).doOnTerminate(() -> {
				this.outboundSink.tryEmitComplete();
			}).subscribe();

			if (isStarted.compareAndSet(false, true)) {
				inboundReady.tryEmitValue(null);
			}

			try {
				this.serverSocketChannel = new UDSServerStringChannel(selector == null ? Selector.open() : selector,
						incomingBufferSize, executorService) {
					public void start(UnixDomainSocketAddress address, IOConsumer<SocketChannel> acceptHandler,
							IOConsumer<String> readHandler) throws IOException {
						super.start(StandardProtocolFamily.UNIX, address, acceptHandler, readHandler);
					}

					public boolean isClientConnected() {
						return this.acceptedClient != null;
					}

					@Override
					protected void handleException(SelectionKey key, Throwable e) {
						// Do this with existing executor
						if (restartSession) {
							executor.execute(() -> {
								try {
									synchronized (UDSMcpSessionTransport.this) {
										UDSMcpSessionTransport.this.close();
										// Delete the file underneath the UDS socket
										Files.deleteIfExists(targetAddress);
										if (logger.isDebugEnabled()) {
											logger.debug("Session transport restarting");
										}
										initialize();
										initProcessing();
										if (logger.isDebugEnabled()) {
											logger.debug("Session transport restarted");
										}
									}
								} catch (IOException e1) {
									logger.error("Could not restart server session", e1);
								}
							});
						}
					}
				};
				this.serverSocketChannel.start(UnixDomainSocketAddress.of(targetAddress), (clientChannel) -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Accepted connect from clientChannel=" + clientChannel);
					}
					startOutboundProcessing();
				}, (dataLine) -> {
					String message = (String) dataLine;
					if (logger.isDebugEnabled()) {
						logger.debug("Received message line=" + message);
					}
					try {
						handleMessage(McpSchema.deserializeJsonRpcMessage(objectMapper, message.trim()));
					} catch (IOException e) {
						this.serverSocketChannel.close();
					}
				});
			} catch (IOException e) {
				this.serverSocketChannel.close();
				throw new RuntimeException("accepterNonBlockSocketChannel could not be started", e);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Session transport initProcessing completed");
			}
		}

		private void startOutboundProcessing() {
			Function<Flux<JSONRPCMessage>, Flux<JSONRPCMessage>> outboundConsumer = messages -> messages // @formatter:off
				 .doOnSubscribe(subscription -> outboundReady.tryEmitValue(null))
				 .publishOn(outboundScheduler)
				 .handle((message, sink) -> {
					 if (message != null && !isClosing.get()) {
						 try {
							 serverSocketChannel.writeMessage(objectMapper.writeValueAsString(message));
							 sink.next(message);
						 }
						 catch (IOException e) {
							 if (!isClosing.get()) {
								 logger.error("Error writing message", e);
								 sink.error(new RuntimeException(e));
							 }
							 else {
								 logger.debug("Stream closed during shutdown", e);
							 }
						 }
					 }
					 else if (isClosing.get()) {
						 sink.complete();
					 }
				 })
				 .doOnComplete(() -> {
					 isClosing.set(true);
					 outboundScheduler.dispose();
				 })
				 .doOnError(e -> {
					 if (!isClosing.get()) {
						 logger.error("Error in outbound processing", e);
						 isClosing.set(true);
						 outboundScheduler.dispose();
					 }
				 })
				 .map(msg -> (JSONRPCMessage) msg);
			
				 outboundConsumer.apply(outboundSink.asFlux()).subscribe();
		 } 

		private void logIfNotClosing(String message, Exception e) {
			if (!isClosing.get()) {
				logger.error(message, e);
			}
		}

	}

}
