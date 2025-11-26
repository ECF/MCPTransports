package com.composent.ai.mcp.examples.transport.uds.mcpclient;

import java.time.Duration;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composent.ai.mcp.transport.uds.UDSMcpClientTransport;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;

@Component
public class McpSyncClientComponent {

	private static Logger logger = LoggerFactory.getLogger(McpSyncClientComponent.class);

	private McpSyncClient client;

	@Activate
	public McpSyncClientComponent(@Reference UDSMcpClientTransport transport) {
		client = McpClient.sync(transport).capabilities(ClientCapabilities.builder().build()).initializationTimeout(Duration.ofDays(1)).requestTimeout(Duration.ofDays(1)).build();
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
		}
	}

}
