# Simple Build Alert

Posts Flex image build results to Slack via a **Workflow trigger webhook**.

This action is used by `build-ot3-actions.yml`. It does not route to channels itself—the Slack Workflow you connect to the webhook decides where messages go and how they are formatted.

## Required configuration

### GitHub secret

- **`SLACK_WEBHOOK_URL`** — Slack **Workflow trigger** URL (from Workflow Builder → Webhook trigger → copy URL).

This must **not** be a classic Incoming Webhook (`hooks.slack.com/services/...`). Incoming webhooks expect `text`/`blocks` JSON and will return HTTP 400 for this payload shape.

### Slack Workflow variables

Add trigger variables matching the JSON keys emitted by `build_payload.py`:

| Variable | Description |
| -------- | ----------- |
| `tag` | Monorepo tag (e.g. `ot3@4.0.0-beta.1`) or `None` for branch builds |
| `headline` | Short summary (e.g. `Build artifacts deployed`) |
| `status` | `deployed`, `failure`, or `cancelled` |
| `type` | `tag` or `branch` |
| `reflike` | oe-core ref (short form) |
| `monorepo-reflike` | opentrons ref (short form) |
| `firmware-reflike` | ot3-firmware ref (short form) |
| `s3-url` | S3 console URL for the run |
| `full-image` | Full image tar URL |
| `system-update` | System zip URL |
| `version-file` | VERSION.json URL |
| `release-notes` | Release notes URL |

For branch builds, `tag` is the literal string `None`—use a Workflow condition if you want different copy for tagged vs branch builds.

## Usage

```yaml
- uses: ./.github/actions/simple-build-alert
  continue-on-error: true
  with:
    status: deployed # or failure, cancelled
    workflow_name: 'Build Flex image on github workflows'
    webhook_url: ${{ secrets.SLACK_WEBHOOK_URL }}
    console_url: ${{ needs.run-build.outputs.console_url }}
    system_url: ${{ needs.run-build.outputs.system_url }}
    fullimage_url: ${{ needs.run-build.outputs.fullimage_url }}
    version_file_url: ${{ needs.run-build.outputs.version_file_url }}
    release_notes_file_url: ${{ needs.run-build.outputs.release_notes_file_url }}
    oe-core_ref: ${{ needs.decide-refs.outputs.oe-core }}
    monorepo_ref: ${{ needs.decide-refs.outputs.monorepo }}
    firmware_ref: ${{ needs.decide-refs.outputs.ot3-firmware }}
```

## Inputs

| Input | Required | Description |
| ----- | -------- | ----------- |
| `status` | yes | `deployed`, `failure`, or `cancelled` (`success` is treated as `deployed`) |
| `workflow_name` | yes | Workflow display name |
| `webhook_url` | yes | Slack Workflow trigger URL |
| `console_url` | no | S3 console link |
| `system_url` | no | System update zip URL |
| `fullimage_url` | no | Full image tar URL |
| `version_file_url` | no | VERSION.json URL |
| `release_notes_file_url` | no | Release notes URL |
| `oe-core_ref` | no | Full git ref for oe-core |
| `monorepo_ref` | no | Full git ref for opentrons |
| `firmware_ref` | no | Full git ref for ot3-firmware |
