# Web browser quality matrix

The deployment workflow runs the shared and web Wasm test suites in the latest
headless Chrome and Firefox on Ubuntu. The same suites remain Chrome-only for a
normal local build; use `-PfullBrowserMatrix=true` when both browsers are
installed to reproduce the CI matrix.

## Automated deployment gate

| Engine | Platform | Coverage |
| --- | --- | --- |
| Chrome | Ubuntu CI | Shared-domain and web Wasm tests |
| Firefox | Ubuntu CI | Shared-domain and web Wasm tests |

## Manual release matrix

The following checks are still required before calling the web release fully
browser-validated:

| Browser | Platform | Required checks |
| --- | --- | --- |
| Edge | Current Windows | Install/upgrade, offline restart, sync, print, keyboard, 200% zoom, and Hebrew RTL |
| Safari | Current macOS and iOS | Install where supported, offline restart, sync, print, keyboard/touch, 200% zoom, and Hebrew RTL |
| Chrome | Current Android and desktop | Installed PWA upgrade, offline restart, sync, print, keyboard/touch, and Hebrew RTL |
| Firefox | Current desktop | IndexedDB recovery, offline restart, sync, print, keyboard, 200% zoom, and Hebrew RTL |

Record the browser version, operating system, install mode, test account, and
result for each release candidate. A failing manual row remains a release
blocker even when the automated Wasm suites pass.
