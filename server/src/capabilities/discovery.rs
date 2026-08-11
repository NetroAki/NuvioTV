use std::{net::IpAddr, process::Stdio, time::Duration};

use tokio::{process::Command, time::timeout};

use crate::network::ResolvedBind;

use super::{MediaCapabilities, ServerCapabilities, SystemCapabilities, TransportCapabilities};

pub async fn discover(bind: &ResolvedBind) -> ServerCapabilities {
    let (ffmpeg, ffprobe) = tokio::join!(ffmpeg_capabilities(), command_available("ffprobe"));
    ServerCapabilities {
        api_versions: vec![1],
        transport: TransportCapabilities {
            interface: bind.interface.clone(),
            address_family: match bind.ip {
                IpAddr::V4(_) => "ipv4",
                IpAddr::V6(_) => "ipv6",
            }
            .to_owned(),
            tailnet_only: true,
        },
        system: SystemCapabilities {
            logical_cpus: std::thread::available_parallelism()
                .map(usize::from)
                .unwrap_or(1),
        },
        media: MediaCapabilities {
            ffmpeg_available: ffmpeg.is_some(),
            ffprobe_available: ffprobe,
            hardware_accelerators: ffmpeg.unwrap_or_default(),
        },
    }
}

async fn command_available(command: &str) -> bool {
    run_command(command, &["-version"])
        .await
        .is_some_and(|output| output.status.success())
}

async fn ffmpeg_capabilities() -> Option<Vec<String>> {
    let output = run_command("ffmpeg", &["-hide_banner", "-hwaccels"]).await?;
    if !output.status.success() {
        return None;
    }
    let stdout = String::from_utf8_lossy(&output.stdout);
    Some(parse_hwaccels(&stdout))
}

async fn run_command(command: &str, arguments: &[&str]) -> Option<std::process::Output> {
    let mut process = Command::new(command);
    process
        .args(arguments)
        .stdin(Stdio::null())
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .kill_on_drop(true);
    timeout(Duration::from_secs(3), process.output())
        .await
        .ok()?
        .ok()
}

pub(crate) fn parse_hwaccels(output: &str) -> Vec<String> {
    output
        .lines()
        .map(str::trim)
        .skip_while(|line| !line.eq_ignore_ascii_case("Hardware acceleration methods:"))
        .skip(1)
        .filter(|line| !line.is_empty())
        .filter(|line| {
            line.chars()
                .all(|character| character.is_ascii_alphanumeric() || character == '_')
        })
        .map(str::to_owned)
        .collect()
}

#[cfg(test)]
mod tests {
    use super::parse_hwaccels;

    #[test]
    fn parses_dynamic_accelerator_list() {
        let output = "Hardware acceleration methods:\nvdpau\ncuda\nvaapi\nqsv\n";
        assert_eq!(parse_hwaccels(output), ["vdpau", "cuda", "vaapi", "qsv"]);
    }

    #[test]
    fn ignores_unexpected_output_lines() {
        let output = "Hardware acceleration methods:\nvaapi\nnot an accelerator\n";
        assert_eq!(parse_hwaccels(output), ["vaapi"]);
    }
}
