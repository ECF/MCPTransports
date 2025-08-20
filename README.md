# MCPTransports - Additional Transports for Model Context Protocol (MCP) Servers and Clients

In the [Java MCP SDK](https://github.com/modelcontextprotocol/java-sdk), MCP Servers and Clients creation require a transport implementation.  The two transports supported in the existing mcp java sdk are [stdio and http](https://github.com/modelcontextprotocol/java-sdk/tree/main/mcp/src/main/java/io/modelcontextprotocol/server/transport).

The purpose of this project is to provide alternative transports for developers interested in building MCP Servers.  The intention is to start with Java Implementations and provide Python and Javascript implementations as well.

## Transport: Unix Domain Sockets (UDS) MCP Transport

Project: [com.composent.ai.mcp.transport.uds](/com.composent.ai.mcp.transport.uds):  Unix Domain Sockets transport to support MCP sync or async server <-> client communication (Java)

UDS supports [inter-process communication in the same operating system](https://en.wikipedia.org/wiki/Unix_domain_socket). For some use cases, this provides a higher level of security than http (e.g. localhost-only MCP server <-> client communication), and is somewhat more flexible than stdio.

### Example UDS MCP Servers and Clients

Project: Example MCP Servers using UDS transport:  [MCP Async and Sync Servers](/com.composent.ai.mcp.examples.transport.uds.mcpserver)  (Java)

Project: Example MCP Clients using UDS transport:  [MCP Async and Sync Clients](/com.composent.ai.mcp.examples.transport.uds.mcpclient)  (Java)

## Running the Examples
This repo is a [Bndtools Workspace](https://bndtools.org/).  [Bndtools 7.1+ with Eclipse](https://bndtools.org/installation.html) is required to run and debug the examples without adding other meta-data.
