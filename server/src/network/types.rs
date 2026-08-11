use std::net::{IpAddr, SocketAddr};

use thiserror::Error;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct InterfaceAddress {
    pub name: String,
    pub ip: IpAddr,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ResolvedBind {
    pub interface: String,
    pub ip: IpAddr,
    pub socket_addr: SocketAddr,
}

#[derive(Debug, Error)]
pub enum BindError {
    #[error("failed to inspect network interfaces: {0}")]
    InterfaceDiscovery(#[from] std::io::Error),
    #[error("wildcard and unspecified bind addresses are forbidden")]
    WildcardAddress,
    #[error("configured address {ip} is not a Tailscale address")]
    NotTailnetAddress { ip: IpAddr },
    #[error("configured address {ip} is not assigned to interface {interface}")]
    AddressNotOnInterface { interface: String, ip: IpAddr },
    #[error("interface {interface} has no assigned Tailscale address")]
    TailnetInterfaceUnavailable { interface: String },
}
