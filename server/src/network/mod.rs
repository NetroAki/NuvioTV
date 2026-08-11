mod resolver;
mod types;

pub use resolver::{is_tailnet_ip, resolve_bind, resolve_from};
pub use types::{BindError, InterfaceAddress, ResolvedBind};
