use serde::{Deserialize, Serialize};

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ManifestBatchRequest {
    pub addon_urls: Vec<String>,
    #[serde(default = "default_allow_stale")]
    pub allow_stale: bool,
}

impl ManifestBatchRequest {
    pub fn validate(&self, max_addons: usize) -> Result<(), ManifestRequestError> {
        if self.addon_urls.is_empty() {
            return Err(ManifestRequestError::Empty);
        }
        if self.addon_urls.len() > max_addons {
            return Err(ManifestRequestError::TooMany);
        }
        Ok(())
    }
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ManifestBatchResponse {
    pub entries: Vec<ManifestEntry>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ManifestEntry {
    pub state: ManifestState,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub manifest: Option<serde_json::Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub failure: Option<ManifestFailure>,
}

#[derive(Debug, Clone, Copy, Serialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum ManifestState {
    FreshCache,
    Refreshed,
    StaleCache,
    Failed,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ManifestFailure {
    pub code: &'static str,
    pub message: &'static str,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ManifestRequestError {
    Empty,
    TooMany,
}

const fn default_allow_stale() -> bool {
    true
}
