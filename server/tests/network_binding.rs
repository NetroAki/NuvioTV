use std::net::{IpAddr, Ipv4Addr, Ipv6Addr};

use nuvio_companion::{
    config::NetworkConfig,
    network::{BindError, InterfaceAddress, is_tailnet_ip, resolve_from},
};

fn address(name: &str, ip: IpAddr) -> InterfaceAddress {
    InterfaceAddress {
        name: name.to_owned(),
        ip,
    }
}

#[test]
fn discovers_tailnet_address_on_configured_interface() {
    let tailnet = IpAddr::V4(Ipv4Addr::new(100, 105, 18, 59));
    let addresses = [
        address("eth0", IpAddr::V4(Ipv4Addr::new(192, 168, 1, 4))),
        address("tailscale0", tailnet),
    ];
    let resolved = resolve_from(&NetworkConfig::default(), &addresses).unwrap();
    assert_eq!(resolved.ip, tailnet);
    assert_eq!(resolved.socket_addr.port(), 8765);
}

#[test]
fn rejects_explicit_wildcard_and_non_tailnet_addresses() {
    let wildcard = NetworkConfig {
        bind_ip: Some(IpAddr::V4(Ipv4Addr::UNSPECIFIED)),
        ..NetworkConfig::default()
    };
    assert!(matches!(
        resolve_from(&wildcard, &[]),
        Err(BindError::WildcardAddress)
    ));

    let lan = NetworkConfig {
        bind_ip: Some(IpAddr::V4(Ipv4Addr::new(192, 168, 1, 4))),
        ..NetworkConfig::default()
    };
    assert!(matches!(
        resolve_from(&lan, &[]),
        Err(BindError::NotTailnetAddress { .. })
    ));
}

#[test]
fn recognizes_tailscale_ipv4_and_ipv6_ranges() {
    assert!(is_tailnet_ip(IpAddr::V4(Ipv4Addr::new(100, 64, 0, 1))));
    assert!(is_tailnet_ip(IpAddr::V4(Ipv4Addr::new(100, 127, 255, 254))));
    assert!(!is_tailnet_ip(IpAddr::V4(Ipv4Addr::new(100, 128, 0, 1))));
    assert!(is_tailnet_ip(IpAddr::V6(
        "fd7a:115c:a1e0::1".parse::<Ipv6Addr>().unwrap()
    )));
    assert!(!is_tailnet_ip(IpAddr::V6(Ipv6Addr::LOCALHOST)));
}

#[test]
fn explicit_address_must_belong_to_configured_interface() {
    let ip = IpAddr::V4(Ipv4Addr::new(100, 90, 1, 2));
    let config = NetworkConfig {
        bind_ip: Some(ip),
        ..NetworkConfig::default()
    };
    let error = resolve_from(&config, &[address("other0", ip)]).unwrap_err();
    assert!(matches!(error, BindError::AddressNotOnInterface { .. }));
}
