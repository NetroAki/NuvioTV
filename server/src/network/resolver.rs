use std::net::{IpAddr, Ipv4Addr, Ipv6Addr};

use crate::config::NetworkConfig;

use super::{BindError, InterfaceAddress, ResolvedBind};

pub fn resolve_bind(config: &NetworkConfig) -> Result<ResolvedBind, BindError> {
    let addresses = if_addrs::get_if_addrs()?
        .into_iter()
        .map(|interface| {
            let ip = interface.ip();
            InterfaceAddress {
                name: interface.name,
                ip,
            }
        })
        .collect::<Vec<_>>();
    resolve_from(config, &addresses)
}

pub fn resolve_from(
    config: &NetworkConfig,
    addresses: &[InterfaceAddress],
) -> Result<ResolvedBind, BindError> {
    let interface_addresses = addresses
        .iter()
        .filter(|address| address.name == config.interface)
        .collect::<Vec<_>>();

    let ip = match config.bind_ip {
        Some(ip) => {
            validate_explicit_ip(&config.interface, ip, &interface_addresses)?;
            ip
        }
        None => interface_addresses
            .iter()
            .map(|address| address.ip)
            .filter(|ip| is_tailnet_ip(*ip))
            .min_by_key(|ip| matches!(ip, IpAddr::V6(_)))
            .ok_or_else(|| BindError::TailnetInterfaceUnavailable {
                interface: config.interface.clone(),
            })?,
    };

    Ok(ResolvedBind {
        interface: config.interface.clone(),
        ip,
        socket_addr: (ip, config.port).into(),
    })
}

fn validate_explicit_ip(
    interface: &str,
    ip: IpAddr,
    interface_addresses: &[&InterfaceAddress],
) -> Result<(), BindError> {
    if ip.is_unspecified() {
        return Err(BindError::WildcardAddress);
    }
    if !is_tailnet_ip(ip) {
        return Err(BindError::NotTailnetAddress { ip });
    }
    if !interface_addresses.iter().any(|address| address.ip == ip) {
        return Err(BindError::AddressNotOnInterface {
            interface: interface.to_owned(),
            ip,
        });
    }
    Ok(())
}

pub fn is_tailnet_ip(ip: IpAddr) -> bool {
    match ip {
        IpAddr::V4(ip) => is_tailnet_ipv4(ip),
        IpAddr::V6(ip) => is_tailnet_ipv6(ip),
    }
}

fn is_tailnet_ipv4(ip: Ipv4Addr) -> bool {
    let octets = ip.octets();
    octets[0] == 100 && (64..=127).contains(&octets[1])
}

fn is_tailnet_ipv6(ip: Ipv6Addr) -> bool {
    let segments = ip.segments();
    segments[0] == 0xfd7a && segments[1] == 0x115c && segments[2] == 0xa1e0
}
