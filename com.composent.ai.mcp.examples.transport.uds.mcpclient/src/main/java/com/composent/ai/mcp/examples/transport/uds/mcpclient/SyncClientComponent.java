package com.composent.ai.mcp.examples.transport.uds.mcpclient;

import java.nio.file.Path;
import java.time.Duration;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composent.ai.mcp.transport.uds.UDSMcpClientTransportConfig;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;

@Component
public class SyncClientComponent {

	private static Logger logger = LoggerFactory.getLogger(SyncClientComponent.class);

	private final Path socketPath = Path.of("").toAbsolutePath().getParent()
			.resolve("com.composent.ai.mcp.examples.transport.uds.mcpserver").resolve("s.socket").toAbsolutePath();

	private ComponentInstance<McpClientTransport> transportComponent;
	private McpSyncClient client;

	@Activate
	public SyncClientComponent(
			@Reference(target = "(component.factory=UDSMcpClientTransportFactory)") ComponentFactory<McpClientTransport> transportFactory) {
		this.transportComponent = new UDSMcpClientTransportConfig(socketPath).newInstanceFromFactory(transportFactory);
		client = McpClient.sync(this.transportComponent.getInstance())
				.capabilities(ClientCapabilities.builder().build()).initializationTimeout(Duration.ofDays(1))
				.requestTimeout(Duration.ofDays(1)).build();
		// initialize will connect to server
		client.initialize();
		logger.debug("uds sync client initialized");
		// test list tools from server
		client.listTools().tools().forEach(t -> logger.debug("uds sync client seeing tool=" + t.toString()));
	}

	@Deactivate
	void deactivate() {
		if (this.client != null) {
			this.client.closeGracefully();
			this.client = null;
			logger.debug("uds sync client closed");
			if (this.transportComponent != null) {
				this.transportComponent.dispose();
				this.transportComponent = null;
			}
		}
	}

}
