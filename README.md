# MCPTransports - Alternative Transports for MCP Servers and Clients

In the [Java MCP SDK](https://github.com/modelcontextprotocol/java-sdk), MCP Servers and Clients creation require a transport implementation.  The two transports supported in the mcp java sdk (at this time) are [stdio and http](https://github.com/modelcontextprotocol/java-sdk/tree/main/mcp/src/main/java/io/modelcontextprotocol/server/transport).

The purpose of this project is to provide alternative transports for developers interested in building there MCP Servers.  The intention is to start with Java Implementations and provide Python and Javascript implementations as requested by the communitity and/or to use remote services across runtime environments (e.g. [Python <-> Java](https://github.com/ECF/Py4j-RemoteServicesProvider)).

## Transport: Unix Domain Sockets (UDS) MCP Transport

Project: [com.composent.ai.mcp.transport.uds](/com.composent.ai.mcp.transport.uds):  Unix Domain Sockets transport to support MCP sync or async server <-> client communication (Java)

UDS supports [inter-process communication in the same operating system](https://en.wikipedia.org/wiki/Unix_domain_socket). For some use cases, this provides a higher level of security than http (e.g. localhost-only MCP server <-> client communication), and is somewhat more flexible than stdio.

### Example UDS MCP Servers and Clients

Project: Example MCP Servers using UDS transport:  [MCP Async and Sync Servers](/com.composent.ai.mcp.examples.transport.uds.mcpserver)  (Java)

This example creates an [Async](/com.composent.ai.mcp.examples.transport.uds.mcpserver/src/main/java/com/composent/ai/mcp/examples/transport/uds/mcpserver/McpAsyncServerComponent.java) and [Sync](/com.composent.ai.mcp.examples.transport.uds.mcpserver/src/main/java/com/composent/ai/mcp/examples/transport/uds/mcpserver/McpSyncServerComponent.java) MCP server that uses  Unix Domain Sockets (UDS) as transport.

Here is the creation and use of the transport (sync):

```java
		// Create unix domain socket transport
		UDSMcpServerTransportProvider transport = new UDSMcpServerTransportProvider(
				socketPath, true);
		// Create sync server
		this.server = McpServer.sync(transport).serverInfo("example-sync-uds-transport-server", "1.0.0")
				.capabilities(ServerCapabilities.builder().tools(true).build()).build();
		logger.debug("uds sync server started");
```

The socketPath variable has a valid Path instance identifying the file to be used for client connection on the local file system.

Project: Example MCP Clients using UDS transport:  [MCP Async and Sync Clients](/com.composent.ai.mcp.examples.transport.uds.mcpclient)  (Java)

Same form as sync example above.

## Running the Examples
This repo is a [Bndtools Workspace](https://bndtools.org/).  [Bndtools 7.1+ with Eclipse](https://bndtools.org/installation.html) is required to run and debug the examples without adding other meta-data.

