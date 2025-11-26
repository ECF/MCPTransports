package com.composent.ai.mcp.examples.transport.uds.mcpserver;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composent.ai.mcp.transport.uds.UDSMcpServerTransportConfig;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

@Component(immediate = true)
public class SyncServerComponent {

	private static Logger logger = LoggerFactory.getLogger(SyncServerComponent.class);

	// file named to be used for client <-> server communication
	private final Path socketPath = Paths.get("").resolve("s.socket").toAbsolutePath();

	private ComponentInstance<McpServerTransportProvider> transportComponentInstance;
	private McpSyncServer server;

	@Activate
	public SyncServerComponent(
			@Reference(target = "(component.factory=UDSMcpServerTransportFactory)") ComponentFactory<McpServerTransportProvider> transportFactory) {
		this.transportComponentInstance = new UDSMcpServerTransportConfig(socketPath).newInstanceFromFactory(transportFactory);
		this.server = McpServer.sync(this.transportComponentInstance.getInstance())
				.serverInfo("example-sync-uds-transport-server", "1.0.0")
				.capabilities(ServerCapabilities.builder().tools(true).build()).build();
		logger.debug("uds sync server activated");
	}

	@Deactivate
	void deactivate() throws Exception {
		if (this.server != null) {
			this.server.closeGracefully();
			this.server = null;
			logger.debug("uds sync server deactivated");
			if (this.transportComponentInstance != null) {
				this.transportComponentInstance.dispose();
				this.transportComponentInstance = null;
			}
		}
	}

}
