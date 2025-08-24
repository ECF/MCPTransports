package com.composent.ai.mcp.examples.transport.uds.mcpclient;

import java.net.UnixDomainSocketAddress;
import java.nio.file.Path;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composent.ai.mcp.transport.uds.UDSMcpClientTransport;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;

@Component
public class McpSyncClientComponent {

	private static Logger logger = LoggerFactory.getLogger(McpSyncClientComponent.class);

	private final Path socketPath = Path.of("").toAbsolutePath().getParent().resolve("com.composent.ai.mcp.examples.transport.uds.mcpserver").resolve("s.socket").toAbsolutePath();

	private McpSyncClient client;

	@Activate
	void activate() throws Exception {
		logger.debug("starting uds client with socket at path={}", socketPath);
		// create UDS transport via the socketPath (default is 
		UDSMcpClientTransport transport = new UDSMcpClientTransport(UnixDomainSocketAddress.of(socketPath));
		// Create client with transport
		client = McpClient.sync(transport).capabilities(ClientCapabilities.builder().build()).build();
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
