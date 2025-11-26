package com.composent.ai.mcp.transport.uds;

import java.nio.channels.Selector;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class UDSMcpClientTransportConfig extends UDSMcpTransportConfig {

	public UDSMcpClientTransportConfig(Path targetSocketPath, int incomingBufferSize, Selector selector,
			ExecutorService executorService) {
		super(targetSocketPath, incomingBufferSize, selector, executorService);
	}

	public UDSMcpClientTransportConfig(Map<String, ?> properties) {
		super(properties);
	}

	public UDSMcpClientTransportConfig(Path targetSocketPath) {
		super(targetSocketPath);
	}

}
