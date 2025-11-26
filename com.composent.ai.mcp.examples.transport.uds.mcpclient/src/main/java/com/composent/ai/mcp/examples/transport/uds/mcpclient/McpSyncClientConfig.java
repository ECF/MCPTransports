package com.composent.ai.mcp.examples.transport.uds.mcpclient;

import java.nio.file.Path;

import org.osgi.service.component.annotations.Component;

import com.composent.ai.mcp.transport.uds.UDSMcpClientConfig;
import com.composent.ai.mcp.transport.uds.UDSMcpConfig;

@Component(immediate = true, service = UDSMcpClientConfig.class)
public class McpSyncClientConfig implements UDSMcpClientConfig {

	@Override
	public Path getClientSocketPath() {
		// get file in adjacent directory named 'com.composent.ai.mcp.examples.transport.uds.mcpserver
		return UDSMcpConfig.getFilePath(UDSMcpConfig.getCurrentDir().toAbsolutePath().getParent()
				.resolve("com.composent.ai.mcp.examples.transport.uds.mcpserver"), DEFAULT_FILENAME);
	}
}
