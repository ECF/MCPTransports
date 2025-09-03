package com.composent.ai.mcp.examples.transport.uds.mcpserver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composent.ai.mcp.transport.uds.UDSMcpServerTransportProvider;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

@Component
public class McpAsyncServerComponent {
	
	private static Logger logger = LoggerFactory.getLogger(McpAsyncServerComponent.class);
	// file named to be used for client <-> server communication
	private final Path socketPath = Paths.get("").resolve("a.socket").toAbsolutePath();

	private McpAsyncServer server;

	@Activate
	void activate() throws Exception {
		// The s.socket file might still be there from previous run
		Files.deleteIfExists(socketPath);
		logger.debug("starting uds async server with socket at path={}", socketPath);
		// Create unix domain socket transport
		UDSMcpServerTransportProvider transport = new UDSMcpServerTransportProvider(
				socketPath, true);
		// Create sync server
		this.server = McpServer.async(transport).serverInfo("example-async-uds-transport-server", "1.0.0")
				.capabilities(ServerCapabilities.builder().tools(true).build()).build();
		logger.debug("uds async server started");
	}

	@Deactivate
	void deactivate() throws Exception {
		if (this.server != null) {
			this.server.closeGracefully();
			this.server = null;
			logger.debug("uds async server stopped");
		}
	}
}
