use std::{env, sync::Arc};

use super::{ConfigError, ServerConfig};

#[derive(Clone)]
pub struct AuthToken(Arc<str>);

impl ServerConfig {
    pub fn auth_token(&self) -> Result<Option<AuthToken>, ConfigError> {
        let Some(variable) = self.auth.token_env.as_deref() else {
            return Ok(None);
        };
        let value = env::var(variable).map_err(|_| ConfigError::MissingTokenEnvironment {
            variable: variable.to_owned(),
        })?;
        if value.trim().is_empty() {
            return Err(ConfigError::EmptyTokenEnvironment {
                variable: variable.to_owned(),
            });
        }
        Ok(Some(AuthToken(value.into())))
    }
}

impl AuthToken {
    pub fn matches(&self, candidate: &str) -> bool {
        constant_time_eq(self.0.as_bytes(), candidate.as_bytes())
    }
}

impl std::fmt::Debug for AuthToken {
    fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        formatter.write_str("AuthToken([REDACTED])")
    }
}

fn constant_time_eq(left: &[u8], right: &[u8]) -> bool {
    let mut difference = left.len() ^ right.len();
    let length = left.len().max(right.len());
    for index in 0..length {
        let a = left.get(index).copied().unwrap_or_default();
        let b = right.get(index).copied().unwrap_or_default();
        difference |= usize::from(a ^ b);
    }
    difference == 0
}

#[cfg(test)]
mod tests {
    use super::AuthToken;

    #[test]
    fn token_comparison_is_exact_and_debug_is_redacted() {
        let token = AuthToken("private-value".into());
        assert!(token.matches("private-value"));
        assert!(!token.matches("private-valuE"));
        assert!(!token.matches("private-value-longer"));
        assert_eq!(format!("{token:?}"), "AuthToken([REDACTED])");
    }
}
