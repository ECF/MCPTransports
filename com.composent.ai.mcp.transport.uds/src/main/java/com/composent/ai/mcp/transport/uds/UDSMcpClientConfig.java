package com.composent.ai.mcp.transport.uds;

import java.nio.file.Path;

public interface UDSMcpClientConfig extends UDSMcpConfig {

	Path getClientSocketPath();
}
