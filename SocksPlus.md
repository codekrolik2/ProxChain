## ProxAK: Enhanced Security and Control with SOCKS+ Proxy Chains

ProxAK introduces a comprehensive security solution that combines secure proxy chains with firewall capabilities. Elevate your online privacy, anonymity, and control by seamlessly integrating our services with existing VPN clients. Enjoy features such as blacklisting and whitelisting hosts and/or ports, as well as real-time monitoring of active connections. Add the benefits of PKCS#11 certificate support to the mix for enhanced security and peace of mind.

Key Features:

1.  Anonymity and Traffic Obfuscation: Achieve anonymity and obfuscation of your IP address and online activities by routing traffic through secure proxy chains.
3.  Proxy Chains with Firewall Capabilities: Build powerful proxy chains using both SOCKS+ (SOCKS over TLS) and regular SOCKS protocols, with support for 3rd party servers.
4.  Blacklisting and Whitelisting: Exercise granular control by blacklisting or whitelisting specific hosts and/or ports, allowing only trusted connections.
5.  Real-Time Connection Monitoring: Gain comprehensive visibility into active connections, empowering you to detect and respond to potential threats.
6.  TLS-Protected DNS Resolution: Choose to resolve DNS queries on proxy side via modern TLS-encrypted DNS providers, ensuring privacy and protection.
7.  Enhanced Security: Utilize PKCS#11 tokens to protect your authentication certificates, bolstering the security of your proxy connections.
8.  Open Source Proxy Client: Benefit from our Open Source client, ensuring transparency and allowing you to verify the security and privacy measures implemented.
9.  Proxy Chain Integration for VPN Clients: Seamlessly integrate proxy chains with popular VPN clients like OpenVPN, expanding the security capabilities of your existing setup.

Conclusion: Experience enhanced security, control, and privacy with ProxAK's proxy chains. Utilize blacklisting and whitelisting features, monitor active connections in real-time, and seamlessly integrate with existing VPN clients. Take charge of your online security and enjoy a protected browsing experience with ProxAK.

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
*We don't support BIND in SOCKS+ anyway so we just won't support it.*
*We need to alert that UDP ASSOCIATE can't be run if the exit node is SOCKS v4.*

6. SOCKS chain server can't expose SOCKS+ interface due to the fact that it needs to be interoperable with software that only knows about standard SOCKS v4/v5 protocols.
*For that reason SOCKS chain server self-identification should be determined by its exit node: as SOCKS v4 if the exit node is SOCKS v4, and as SOCKS v5 if the exit node is SOCKS v5 or SOCKS+.*