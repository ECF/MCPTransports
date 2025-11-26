package com.composent.ai.mcp.transport.uds;

import java.nio.file.Path;

import io.modelcontextprotocol.spec.McpServerTransportProvider;

public interface UDSMcpServerTransportProvider extends McpServerTransportProvider {

	Path getServerSocketPath();
	
}
