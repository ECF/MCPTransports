package com.composent.ai.mcp.transport.uds;

import java.io.File;
import java.nio.channels.Selector;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class UDSMcpServerTransportConfig extends UDSMcpTransportConfig {

	public static final String AUTO_RESTART_SESSION_PROP = "udsAutoRestartSession";

	protected boolean autoRestartSession = true;

	public UDSMcpServerTransportConfig(Path targetSocketPath, int incomingBufferSize, Selector selector,
			ExecutorService executorService, boolean autoRestartSession) {
		super(targetSocketPath, incomingBufferSize, selector, executorService);
		this.autoRestartSession = autoRestartSession;
	}

	public UDSMcpServerTransportConfig(Path targetSocketPath, boolean autoRestartSession) {
		super(targetSocketPath);
		File f = targetSocketPath.toFile();
		f.delete();
		f.deleteOnExit();
		this.autoRestartSession = autoRestartSession;
	}

	public UDSMcpServerTransportConfig(Path targetSocketPath) {
		this(targetSocketPath, true);
	}

	public UDSMcpServerTransportConfig(Map<String, Object> properties) {
		super(properties);
		Boolean b = (Boolean) properties.get(AUTO_RESTART_SESSION_PROP);
		if (b != null) {
			this.autoRestartSession = b;
		}
	}

	public Map<String, Object> asProperties() {
		Map<String, Object> props = super.asProperties();
		props.put(AUTO_RESTART_SESSION_PROP, autoRestartSession());
		return props;
	}

	public boolean autoRestartSession() {
		return autoRestartSession;
	}
}
