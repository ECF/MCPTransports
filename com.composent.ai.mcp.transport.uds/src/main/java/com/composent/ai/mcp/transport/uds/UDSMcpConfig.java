package com.composent.ai.mcp.transport.uds;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface UDSMcpConfig {

	public static final int DEFAULT_INCOMING_BUFFER_SIZE = 4096;
	public static final String DEFAULT_FILENAME = "UDSMcp.s";

	static Path getCurrentDir() {
		return Paths.get("");
	}

	public static Path getParentDir() {
		return Paths.get("..");
	}

	static void deleteIfExists(File f) {
		if (f.exists()) {
			f.delete();
		}
	}

	static void deleteOnExit(File f) {
		f.deleteOnExit();
	}

	static Path getFilePath(Path dir, String fileName) {
		return dir.resolve(DEFAULT_FILENAME);
	}

	default int getIncomingBufferSize() {
		return DEFAULT_INCOMING_BUFFER_SIZE;
	}

	default ExecutorService getExecutorService() {
		return Executors.newCachedThreadPool();
	}

	default Selector getSelector() {
		try {
			return Selector.open();
		} catch (IOException e) {
			throw new RuntimeException("Cannot open new selector", e);
		}
	}

}
