package com.composent.ai.mcp.examples.transport.uds.mcpserver;

import java.nio.file.Path;

import org.osgi.service.component.annotations.Component;

import com.composent.ai.mcp.transport.uds.UDSMcpConfig;
import com.composent.ai.mcp.transport.uds.UDSMcpServerConfig;

@Component(immediate = true, service = UDSMcpServerConfig.class)
public class McpSyncServerConfig implements UDSMcpServerConfig {

	@Override
	public Path getServerSocketPath() {
		Path p = UDSMcpConfig.getFilePath(UDSMcpConfig.getCurrentDir(), UDSMcpConfig.DEFAULT_FILENAME);
		UDSMcpConfig.deleteIfExists(p.toFile());
		UDSMcpConfig.deleteOnExit(p.toFile());
		return p;
	}
}
