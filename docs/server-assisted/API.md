# Companion API

Current API version: `1`

Base endpoint is configured by the Android client at runtime. It may be an explicit Tailscale IP or MagicDNS hostname. No server address is compiled into the application.

## Envelope

Successful JSON responses use:

```json
{
  "apiVersion": 1,
  "data": {}
}
```

Clients must reject unsupported major API versions and fall back to the existing direct Nuvio path when possible.

## Authentication

Tailscale identity and ACLs are the default authorization boundary. If `auth.token_env` is configured on the server, all routes additionally require:

```http
Authorization: Bearer <token>
```

Tokens are not accepted through URLs and are never rendered in debug output.

## `GET /v1/health`

Confirms process/API availability.

```json
{
  "apiVersion": 1,
  "data": {
    "status": "ok",
    "serverVersion": "0.1.0"
  }
}
```

## `GET /v1/capabilities`

Returns startup-discovered server capabilities. Example fields:

```json
{
  "apiVersion": 1,
  "data": {
    "apiVersions": [1],
    "transport": {
      "interface": "tailscale0",
      "addressFamily": "ipv4",
      "tailnetOnly": true
    },
    "system": {
      "logicalCpus": 8
    },
    "media": {
      "ffmpegAvailable": true,
      "ffprobeAvailable": true,
      "hardwareAccelerators": ["vaapi"]
    }
  }
}
```

`hardwareAccelerators` describes backends compiled into the installed FFmpeg build. A later playback probe must still verify device access and encoder compatibility before selecting hardware acceleration.

## `GET /v1/diagnostics`

Returns local in-memory process counters:

- uptime
- request count
- requests currently in flight
- failed response count
- average request latency

No diagnostic data is sent outside the tailnet.

## Limits

The server applies configurable whole-request body limits and request timeouts. TOML rejects unknown fields and unsupported configuration versions rather than silently accepting misspelled or future settings.

Playback, sync, catalog, artwork, subtitle, and segment contracts will be added as tested vertical slices. This file documents only implemented endpoints.
