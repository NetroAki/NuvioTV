# Tailscale-only deployment

The companion server must never use a wildcard listener. Its default configuration discovers an address assigned to `tailscale0` and validates that address against Tailscale's IPv4 or IPv6 ranges before binding.

## Native process

```sh
cd server
cargo build --locked --release
./target/release/nuvio-companion
```

Expected log shape:

```text
Nuvio companion listening on the tailnet interface=tailscale0 address=100.x.y.z:8765
```

Verify the listener:

```sh
ss -ltn 'sport = :8765'
```

The local address must be the node's Tailscale address, not `0.0.0.0`, `[::]`, a LAN address, or a public address.

## Container

```sh
cd server
cp config.example.toml server.toml
docker compose -f compose.example.yaml up -d --build
```

The compose example deliberately uses host networking and publishes no ports. Host networking makes `tailscale0` visible to the container; the application still binds only to the validated tailnet address. The container runs as UID 10001, drops all capabilities, and uses a read-only root filesystem.

Do not replace this with `ports: ["8765:8765"]`: Docker port publication can create wildcard/LAN firewall rules even when the application design intends tailnet-only access.

## Client endpoint

Either form is valid:

```text
http://100.x.y.z:8765
http://node-name.tailnet-name.ts.net:8765
```

HTTP payloads remain encrypted in transit by Tailscale/WireGuard. MagicDNS is optional and does not make the service public.

The Android app will store this as a user/configured endpoint. It must not embed a server address in source or infer one from a particular user's tailnet.

## ACLs

Grant only the intended Android TV devices or user group access to the server port. Policy is managed in Tailscale, not by opening a public firewall port.

Optional bearer auth is available as an additional layer; it does not replace ACLs.

## Validation checklist

1. `tailscale0` is connected and has a tailnet IP.
2. `/v1/health` responds from another allowed tailnet peer.
3. `ss` shows only the Tailscale IP on port 8765.
4. The same port is unreachable through LAN/public addresses.
5. No Funnel, public tunnel, public reverse proxy, or router port forwarding exists.
