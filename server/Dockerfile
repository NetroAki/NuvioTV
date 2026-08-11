FROM rust:1.95-bookworm AS builder
WORKDIR /build
COPY Cargo.toml Cargo.lock ./
COPY src ./src
RUN cargo build --locked --release

FROM debian:bookworm-slim
RUN apt-get update \
    && apt-get install --no-install-recommends -y ca-certificates ffmpeg \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 nuvio
COPY --from=builder /build/target/release/nuvio-companion /usr/local/bin/nuvio-companion
USER nuvio
ENTRYPOINT ["/usr/local/bin/nuvio-companion"]
