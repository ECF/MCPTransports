package com.composent.ai.mcp.examples.transport.uds.mcpclient;

import java.net.UnixDomainSocketAddress;
import java.nio.file.Path;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composent.ai.mcp.transport.uds.UDSMcpClientTransport;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;

@Component
public class McpAsyncClientComponent {

	private static Logger logger = LoggerFactory.getLogger(McpAsyncClientComponent.class);

	private final Path socketPath = Path.of("").toAbsolutePath().getParent().resolve("com.composent.ai.mcp.examples.transport.uds.mcpserver").resolve("a.socket").toAbsolutePath();

	private McpAsyncClient client;

	@Activate
	void activate() throws Exception {
		logger.debug("starting uds async client with socketPath={}", socketPath);
		// create UDS transport via the socketPath (default is 
		UDSMcpClientTransport transport = new UDSMcpClientTransport(UnixDomainSocketAddress.of(socketPath));
		// Create client with transport
		client = McpClient.async(transport).capabilities(ClientCapabilities.builder().build()).build();
		// initialize will connect to server
		client.initialize();
		logger.debug("client initialized");
		// test list tools from server
		client.listTools().doOnSuccess(
				result -> result.tools().forEach(tool -> logger.debug("client seeing tool=" + tool.toString())));
	}

	@Deactivate
	void deactivate() {
		if (this.client != null) {
			this.client.closeGracefully();
			this.client = null;
			logger.debug("client closed");
		}
	}

}
