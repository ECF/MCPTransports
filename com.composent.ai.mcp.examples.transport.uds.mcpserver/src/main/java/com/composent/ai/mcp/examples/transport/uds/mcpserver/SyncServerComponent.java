package com.composent.ai.mcp.examples.transport.uds.mcpserver;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composent.ai.mcp.transport.uds.UDSMcpServerTransportProvider;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

@Component(immediate = true)
public class SyncServerComponent {

	private static Logger logger = LoggerFactory.getLogger(SyncServerComponent.class);

	private McpSyncServer server;

	@Activate
	public SyncServerComponent(@Reference UDSMcpServerTransportProvider transport) {
		this.server = McpServer.sync(transport).serverInfo("example-sync-uds-transport-server", "1.0.0")
				.capabilities(ServerCapabilities.builder().tools(true).build()).build();
		logger.debug("uds sync server activated");
	}

	@Deactivate
	void deactivate() throws Exception {
		if (this.server != null) {
			this.server.closeGracefully();
			this.server = null;
			logger.debug("uds sync server deactivated");
		}
	}

}
