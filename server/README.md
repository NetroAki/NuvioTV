# Nuvio companion server

Private Rust backend for server-assisted NuvioTV. This first milestone provides the secure runtime foundation: dynamic tailnet binding, versioned APIs, runtime media-tool discovery, bounded requests, local diagnostics, and optional bearer-token defense in depth.

It does not yet proxy addons, sync Stremio accounts, or alter playback.

## Build and test

```sh
cargo build --locked --release
cargo test --all-targets
cargo clippy --all-targets -- -D warnings
```

The checked container build uses Rust 1.95 (edition 2024).

## Run directly

With Tailscale connected and `tailscale0` present:

```sh
cargo run --release
```

The default is `tailscale0:8765`. The server discovers the interface address at runtime. It refuses wildcard, LAN, public, or explicit addresses that are not assigned to the configured tailnet interface.

To use a config file:

```sh
cp config.example.toml server.toml
NUVIO_SERVER_CONFIG="$PWD/server.toml" cargo run --release
```

Do not place secrets in TOML. Optional auth references an environment variable:

```toml
[auth]
token_env = "NUVIO_SERVER_TOKEN"
```

```sh
export NUVIO_SERVER_TOKEN="$(openssl rand -hex 32)"
```

Clients then send `Authorization: Bearer <token>`.

## Docker

Host networking is intentional: it lets the unprivileged container discover and bind the host's `tailscale0` address without publishing a wildcard Docker port.

```sh
cp config.example.toml server.toml
docker compose -f compose.example.yaml up -d --build
```

The example publishes no Docker ports, drops Linux capabilities, uses a read-only root filesystem, and binds only inside the process after validating the address as tailnet-owned.

## API v1

All payloads use:

```json
{
  "apiVersion": 1,
  "data": {}
}
```

Endpoints:

- `GET /v1/health`
- `GET /v1/capabilities`
- `GET /v1/diagnostics`

Capabilities are discovered at process startup. FFmpeg hardware accelerators are obtained from the installed FFmpeg build instead of being assumed from a device/server model.

Diagnostics are held only in process memory and are never transmitted externally.

## HTTP inside Tailscale

Plain HTTP is supported because Tailscale/WireGuard encrypts and authenticates peer transport. Tailscale ACLs remain the primary authorization boundary. Optional application tokens are available for defense in depth. Public forwarding, Funnel, wildcard listening, and internet reverse proxies are not required or enabled.
