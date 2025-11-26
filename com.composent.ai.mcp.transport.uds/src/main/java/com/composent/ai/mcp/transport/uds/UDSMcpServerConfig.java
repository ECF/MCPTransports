package com.composent.ai.mcp.transport.uds;

import java.nio.file.Path;

public interface UDSMcpServerConfig extends UDSMcpConfig {

	public static final int DEFAULT_INCOMING_BUFFER_SIZE = 4096;

	Path getServerSocketPath();

	default boolean isAutoRestartSession() {
		return true;
	}
}
