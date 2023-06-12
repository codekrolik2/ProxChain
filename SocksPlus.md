## ProxAK: Enhanced Security and Control with SOCKS+ Proxy Chains

ProxAK introduces a comprehensive security solution that combines secure SOCKS proxy chains with firewall capabilities. Elevate your online privacy, anonymity, and control by routing your traffic through multiple hops on our encrypted proxies, or seamlessly integrate our services with existing VPN clients. Enjoy features such as blacklisting and whitelisting hosts and/or ports, as well as real-time monitoring of active connections. Add the benefits of PKCS#11 token support to the mix for enhanced security and peace of mind.

Key Features:

1.  Anonymity and Traffic Obfuscation: Achieve anonymity and obfuscation of your IP address and online activities by routing traffic through secure proxy chains.
3.  Proxy Chains with Firewall Capabilities: Build powerful proxy chains using both SOCKS+ (SOCKS over TLS) and regular SOCKS protocols, with support for 3rd party servers.
4.  Blacklisting and Whitelisting: Exercise granular control by blacklisting or whitelisting specific hosts and/or ports, allowing only trusted connections.
5.  Real-Time Connection Monitoring: Gain comprehensive visibility into active connections, empowering you to detect and respond to potential threats.
6.  TLS-Protected DNS Resolution: Choose to resolve DNS queries on proxy side via modern TLS-encrypted DNS providers, ensuring privacy and protection.
7.  Enhanced Security: Utilize PKCS#11 tokens to protect your authentication certificates, bolstering the security of your proxy connections.
8.  Open Source Proxy Client: Benefit from our Open Source client, ensuring transparency and allowing you to verify the security and privacy measures implemented.
9.  Proxy Chain Integration for VPN Clients: Seamlessly integrate proxy chains with popular VPN clients like OpenVPN, expanding the security capabilities of your existing setup.

Conclusion: Experience enhanced security, control, and privacy with SOCKS proxy chains. Utilize blacklisting and whitelisting features, monitor active connections in real-time, and seamlessly integrate with existing VPN clients. Take charge of your online security and enjoy a protected browsing experience with ProxAK.

---

SOCKS+:
- Authentication: SOCKS+ uses certificate-based authentication on TLS layer.
- IPv4 and IPv6 Support: SOCKS+ can work with both IPv4 and IPv6 addresses.
- UDP Support: SOCKS+ can proxy UDP connections via `UDP ASSOCIATE`.
- DNS Resolution: SOCKS+ can handle DNS resolution on the server side, eliminating the need for the client to perform DNS resolution locally.
- Commands Support: SOCKS+ supports two commands: `CONNECT` (establishing a TCP connection) and `UDP ASSOCIATE`. Command `BIND` is not supported due to security concerns.

---

Remarks on SOCKS proxy chaining.

1. Chaining is TCP-only.
   *Since in SOCKS protocols client connections are established using TCP, UDP support can only relate to exit node communication, while the whole proxy chain will operate over TCP.*
   *Theoretically TLS over UDP is possible, but such feature is not planned and is beyond the current scope / goals for SOCKS+ project. Maybe wait for SOCKS++ to support that (as well as QUIC, HTTP3, etc. you name it).*

2.  Authentication: SOCKS v4 supports only a simple username/password authentication method. SOCKS v5 supports various authentication methods, including username/password, GSSAPI (Kerberos), and no authentication.
    *For practical reasons we probably should just support username/password and no authentication for SOCKS v4 and SOCKS v5 protocols, with full support of SOCKS+ authentication.*

3. IPv4 Only: SOCKS v4 is limited to working with IPv4 addresses, while SOCKS v5 supports both IPv4 and IPv6.
   *Since we can't guarantee that a given hostname will be resolved as IPv4, and not IPv6, we need to substitute hostnames with IPv4 addresses for the following:*
   *- SOCKS v4 as exit node will require user to run its own name resolution and send IPv4 address of the destination in the inner request*
   *- SOCKS v4 as intermediate node will require user to resolve and specify the IPv4 address of the server next in the chain*
   *- Those should be shown as alerts in our UI.*

4. DNS Resolution: SOCKS v4 does not handle DNS resolution, while Socks v5 can handle DNS resolution on the server side.
   *Since we can't pass hostnames to SOCKS v4 proxies, they should be substituted with IPv4 addresses, in the same way as described in (3). This, however, is only applicable if user chooses to rely on proxy-side DNS resolution. Another option would be to resolve all DNS names locally and send raw IP addresses in the requests.*

5. Limited Command Support: SOCKS v4 supports only two commands: `CONNECT` (establishing a TCP connection) and `BIND` (binds the incoming connection to a local port), while SOCKS v5 introduces additional commands, including `CONNECT`, `BIND`, and `UDP ASSOCIATE`.
   *We don't support BIND in SOCKS+ anyway so we just won't support it in SOCKS4/5 either.*
   *We need to alert that UDP ASSOCIATE can't be run if the exit node is SOCKS v4.*

6. SOCKS chain program can't expose SOCKS+ interface due to the fact that it needs to be interoperable with software that only knows about standard SOCKS v4/v5 protocols.
   *For that reason SOCKS chain server self-identification should be determined by its exit node: as SOCKS v4 if the exit node is SOCKS v4, and as SOCKS v5 if the exit node is SOCKS v5 or SOCKS+.*

7. SOCKS+ only on our servers.
   *While SOCKS chain can build proxy chains with SOCKS4, SOCKS5, and SOCKS+ servers, our servers only expose SOCKS+ interface. There are SOCKS proxy providers that support insecure interfaces, and if a user is willing to expose sensitive data about request routing on the wire, SOCKS chain program supports it. However, we strongly encourage to use SOCKS+ server only, and believe that providing such inherently insecure services to our users can be viewed as a reputational risk.*

8. Proxy chain management.
   1 - Fixed chain, pre-built by a user.
   2 - Random chain built from the list of servers with weights (as in selection probability), with the ability to control minimum and maximum length.
   3 - Additionally, configuration can be refined to support separate weighted lists for entry nodes, exit nodes, intermediate nodes.
   4 - Alert on cycles in a chain (or prevent if chosen randomly), due to simultaneous per-server connection limit and bandwidth limit.
   *Advanced controls in chain client UI*

9. TLS management
   *E.g., enforce cipher suite that should be used.*
   Refer to the following and similar sources for more details: https://www.microsoft.com/en-us/security/blog/2020/08/20/taking-transport-layer-security-tls-to-the-next-level-with-tls-1-3/

10. Number of simultaneous connections vs number of simultaneous servers (chain length).
    Modern applications are known to use hundreds and thousands simultaneous connections, notably browsers, which can utilize hundreds of active connections per busy page. For that reason we've decided to implement 2 different connection limits:
    1 - based on number of connections per server, this limit is reasonably large to allow raw browsing experience, and can reach anywhere from hundreds to thousands of connections.
    2 - based on number of ProxAK SOCKS+ servers used simultaneously (doesn't affect 3rd party servers participating in the chain). This limits the number of ProxAK SOCKS+ servers you can connect to at once, e.g. when forming a chain of servers. Anywhere from 5-10 hops is considered a reasonable amount nowadays, and our limits on parallel server usage reflect this.
    *Note 1: Those limits don't limit your ability to add 3rd party servers to the chain.*
    *Note 2: We still impose bandwidth limits on each server you're connected to, per-server rather than per-connection, and that limit is applied to raw traffic, i.e. the actual number of bytes transferred, including the envelopes and wrappers like TLS negotiation, etc.*
    *Note 3: The most efficient way to use SOCKS proxy chain is to pass-through a VPN channel (e.g. OpenVPN), minimizing the number of physical connections and consequently the TLS overhead corresponding to each connection.*
    *Note 4: maybe basic plan should include like 5 connections, specifically aimed at VPN tunneling. Thousands of connections per user might be impractical, given Linux file descriptor limits.*
    **Note 5: !!!! or maybe SOCKS+ should be able to multiplex connections via single TLS channel !!!! This is actually relatively not too hard to implement, as long as we state that SOCKS+ is not SOCKS5 over TLS, but rather its own enhanced protocol.**
    **This will enhance our security, because we won't leak anything about individual connections on the wire**
    **Note 6: On the downside, this will kill proxy chain interoperability with SOCKS 4/5. Boo hoo.
    Or will it? Let's think about it in terms of transitions:
    1 - regular SOCKS -> regular SOCKS: individual connections are chained;
    2 - SOCKS+ -> SOCKS+: multiple connection traffic is represented as SOCKS+ packets.
    3 - SOCKS+ -> regular SOCKS -> exit: SOCKS+ packets are spread into individual connections and channeled to regular SOCKS as separate physical connections. Similarly a SOCKS+ exit node would spread the traffic to multiple destination servers.
    4 - SOCKS+ -> regular SOCKS -> SOCKS+: regular SOCKS will see a single connection with SOCKS+ traffic.
    5 - regular SOCKS -> SOCKS+: options are
    5.1 - regular SOCKS streams SOCKS+ stream as is, a single TLS connection
    5.2 - traffic from multiple physical connections from the same user is combined into a single multiplexed stream consisting of SOCKS+ packets.**
    *From the perspective of chain layout -
    1- SOCKS+ server will merge connections from the same user to the same SOCKS+ destination whenever applicable, only spreading it to multiple connections if its an exit node or if there are no further SOCKS+ servers in the trailing chain.
    2- In a situation, when all nodes in the trailing chain are all Regular SOCKS, they will receive multiple connections and channel them as is, including the exit node.
    3- In a situation, when Regular SOCKS or a SOCKS+ server is followed by another SOCKS+ server, it will transmit a single stream via a single TLS connection (encrypted SOCKS+ stream).
    4- The whole split-merge route is never known to the servers and should be programmed up-front on the chaining program side.*
    **Thinking of, SOCKS+ -> SOCKS+ shouldn't even care to run more than 1 connection between the servers.
    Just  throw all the garbage that's destined to that SOCKS+ server into that channel, and that's it. Let the connection time out if its not used.
    And that in its turn means that we at some point can add UDP support, given TLS over UDP?
    We need to be optimized for the chaining scenario, because it's a selling point for the SOCKS+ technology.**
---
Simply put, SOCKS+ channel will split connections either at the exit SOCKS+ node or at the last SOCKS+ node in the chain, followed by regular SOCKS servers. Prior to that it's going to be a single SOCKS+ channel all the way down.
---
As a matter of fact, the only node that matters is the final SOCKS+ node, before that we just tunnel the whole shebang through the nested TLS channels, while after that we spread it into multiple nested SOCKS channels.
The implementation can be split into 2 parts:
1) How it goes all the way to last SOCKS+ server, let's say the limitation here is that an exit node should always be SOCKS+.
2) How it splits into multi-connection for the trailing servers that are regular SOCKS, in case the chain is finalized in this manner.

Should be good.

# SOCKS5t and SOCKS+

Let's define the following terminology:
SOCKS5t - Simply Socks5 over TLS with TLS certificate-based authentication.
SOCKS+ - next-gen protocol with connection multiplexing, based on SOCKS5, also featuring TLS and TLS certificate-based authentication.

# Firewall and connection monitor

There should be a function to disable one or another or both, for paranoid users.
Monitor is UI only, while firewall config can be used in console version as well.
UI can be used locally or for debug/configuration purposes, while console version is server-deployable.

# Empty chain
With empty chain the server can run as a simple SOCKS4/5 proxy server, but I don't think it's any kind of secure.
This feature can be useful for debugging though, to ensure the server works on its own.
Another usage would be to just use firewall/monitoring, without chaining.
Probably we still can support it, while only allowing it with a special config flag, something like "allow insecure empty chain".

# Configuration

### Basic config

- Node / Node Group config.
- Chain builder config (entry nodes : node group; relay nodes : node group; exit nodes : node group; fixed chain).
- Firewall config (blacklist/whitelist).

### Local server config

- Enable SOCKS4 clients.
- Enable SOCKS5 clients.
- SOCKS5 password (show alert to disable SOCKS4 if password is enabled for SOCKS5).
- Allow/Prohibit direct IP requests from clients
- DNS resolution
    - Resolve all domain names on the exit node.
    - Ro, resolve all domain names locally (on chain client)
        - Via local DNS
        - or, Specify DNS over HTTPS server

### Chain-per connection config

Could be desirable in some cases, more chaotic, but less performant (TLS handshake overhead).
Options can be:
- create a new chain per connection.
- create a fixed chain for all connection and switch to a new one every N minutes.

### Enforce server and client to run on a particular interface

Use `Bootstrap.group().localAddress(new InetSocketAddress("192.168.0.100", 0))` to ensure client and server only use a specific interface for their activities.
E.g. a layout in which local machines connect to a server via local wifi, while chain client sends requests to the internet via Ethernet.

```
SslContext  sslContext  = SslContextBuilder.forClient() .protocols("TLSv1.2", "TLSv1.3") .ciphers(Collections.singletonList(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384)) .trustManager(new  File("truststore.jks")) .keyManager(new  File("keystore.jks"), "password") .build();
```