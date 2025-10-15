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
package com.composent.ai.mcp.transport.uds;

import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Selector;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.eclipse.ecf.ai.mcp.transports.UDSClientStringChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCMessage;
import io.modelcontextprotocol.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class UDSMcpClientTransport implements McpClientTransport {

	private static final Logger logger = LoggerFactory.getLogger(UDSMcpClientTransport.class);

	private final Sinks.Many<JSONRPCMessage> inboundSink;

	private final Sinks.Many<JSONRPCMessage> outboundSink;

	private McpJsonMapper objectMapper;

	private UDSClientStringChannel clientChannel;

	private Path targetAddress;

	private Scheduler outboundScheduler;

	private volatile boolean isClosing = false;

	public UDSMcpClientTransport(Path targetAddress) throws IOException {
		this(McpJsonMapper.getDefault(), UDSClientStringChannel.DEFAULT_INBUFFER_SIZE, targetAddress);
	}

	public UDSMcpClientTransport(int incomingBufferSize, Path targetAddress) throws IOException {
		this(McpJsonMapper.getDefault(), incomingBufferSize, targetAddress);
	}

	public UDSMcpClientTransport(McpJsonMapper objectMapper, int incomingBufferSize, Path targetAddress)
			throws IOException {
		Assert.notNull(objectMapper, "objectMapper can not be null");
		Assert.notNull(targetAddress, "targetAddress cannot be null");
		this.objectMapper = objectMapper;

		this.inboundSink = Sinks.many().unicast().onBackpressureBuffer();
		this.outboundSink = Sinks.many().unicast().onBackpressureBuffer();

		this.outboundScheduler = Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(), "outbound");

		this.clientChannel = new UDSClientStringChannel(Selector.open(), incomingBufferSize);
		this.targetAddress = targetAddress;
	}

	@Override
	public Mono<Void> connect(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
		return Mono.<Void>fromRunnable(() -> {
			handleIncomingMessages(handler);
			try {
				this.clientChannel.connect(UnixDomainSocketAddress.of(targetAddress), (client) -> {
					logger.info("CONNECTED to targetAddress=" + targetAddress);
				}, (data) -> {
					JSONRPCMessage json = McpSchema.deserializeJsonRpcMessage(this.objectMapper, data);
					if (!this.inboundSink.tryEmitNext(json).isSuccess()) {
						if (!isClosing) {
							logger.error("Failed to enqueue inbound message: {}", json);
						}
					}
				});
			} catch (IOException e) {
				this.clientChannel.close();
				throw new RuntimeException(
						"Connect to address=" + targetAddress + " failed message: " + e.getMessage());
			}
			startOutboundProcessing();
		}).subscribeOn(Schedulers.boundedElastic());
	}

	private void handleIncomingMessages(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> inboundMessageHandler) {
		this.inboundSink.asFlux().flatMap(message -> Mono.just(message).transform(inboundMessageHandler)
				.contextWrite(ctx -> ctx.put("observation", "myObservation"))).subscribe();
	}

	@Override
	public Mono<Void> sendMessage(JSONRPCMessage message) {
		if (this.outboundSink.tryEmitNext(message).isSuccess()) {
			return Mono.empty();
		} else {
			return Mono.error(new RuntimeException("Failed to enqueue message"));
		}
	}

	private void startOutboundProcessing() {
		this.handleOutbound(messages -> messages.publishOn(outboundScheduler).handle((message, s) -> {
			if (message != null && !isClosing) {
				try {
					this.clientChannel.writeMessage(objectMapper.writeValueAsString(message));
					s.next(message);
				} catch (IOException e) {
					s.error(new RuntimeException(e));
				}
			}
		}));
	}

	protected void handleOutbound(Function<Flux<JSONRPCMessage>, Flux<JSONRPCMessage>> outboundConsumer) {
		outboundConsumer.apply(outboundSink.asFlux()).doOnComplete(() -> {
			isClosing = true;
			outboundSink.tryEmitComplete();
		}).doOnError(e -> {
			if (!isClosing) {
				logger.error("Error in outbound processing", e);
				isClosing = true;
				outboundSink.tryEmitComplete();
			}
		}).subscribe();
	}

	@Override
	public Mono<Void> closeGracefully() {
		return Mono.fromRunnable(() -> {
			isClosing = true;
			logger.debug("Initiating graceful shutdown");
		}).then(Mono.<Void>defer(() -> {
			inboundSink.tryEmitComplete();
			outboundSink.tryEmitComplete();
			return Mono.delay(Duration.ofMillis(100)).then();
		})).then(Mono.defer(() -> {
			// Close clientChannel
			if (this.clientChannel != null) {
				this.clientChannel.close();
				this.clientChannel = null;
			}
			return Mono.empty();
		})).doOnNext(o -> {
			logger.info("channel closed");
		}).then(Mono.fromRunnable(() -> {
			try {
				outboundScheduler.dispose();
				logger.debug("Graceful shutdown completed");
			} catch (Exception e) {
				logger.error("Error during graceful shutdown", e);
			}
		})).then().subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
		return this.objectMapper.convertValue(data, typeRef);
	}

}
