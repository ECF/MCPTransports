package com.composent.ai.mcp.transport.uds;

import java.nio.file.Path;

import io.modelcontextprotocol.spec.McpClientTransport;

public interface UDSMcpClientTransport extends McpClientTransport {

	Path getClientSocketPath();
}
