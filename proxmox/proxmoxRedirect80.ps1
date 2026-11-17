# Setup NAT redirect: 80 -> $ServerPort (idempotent)
param(
    [string]$User,
    [string]$RsaPath,
    [string]$ServerPort
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')
$Config = Get-ProxmoxConfig

if (-not $User)       { $User       = $Config['DEFAULT_USER'] }
if (-not $RsaPath)    { $RsaPath    = $Config['DEFAULT_RSA_PATH'] }
if (-not $ServerPort) { $ServerPort = $Config['DEFAULT_SERVER_PORT'] }
$RsaPath = Resolve-RsaPath $RsaPath
$SshHost = $Config['SSH_HOST']
$SshPort = $Config['SSH_PORT']
$SshOpts = Get-SshOpts $RsaPath

Write-Host "Server port: $ServerPort"
if (-not (Test-Path -LiteralPath $RsaPath -PathType Leaf)) {
    Write-Host "Error: no troba la clau: $RsaPath"
    exit 1
}

$SudoPassword = Read-SudoPassword

$RemoteTemplate = @'
set -euo pipefail
run_sudo() { echo "$SUDO_PASSWORD" | sudo -S -p '' "$@"; }

export DEBIAN_FRONTEND=noninteractive
run_sudo apt-get update -qq
run_sudo apt-get install -y -qq iptables-persistent >/dev/null

# Idempotent check
if run_sudo iptables -t nat -C PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports __SERVER_PORT__ 2>/dev/null; then
  echo "Ja existeix la redirecció 80 -> __SERVER_PORT__"
else
  echo "Afegint redirecció 80 -> __SERVER_PORT__..."
  run_sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports __SERVER_PORT__
fi

# Persist
TMP=$(mktemp)
run_sudo iptables-save > "$TMP"
run_sudo install -m 600 "$TMP" /etc/iptables/rules.v4
rm -f "$TMP"
command -v systemctl >/dev/null 2>&1 && run_sudo systemctl restart netfilter-persistent || true

echo "✔︎ Redirecció 80 -> __SERVER_PORT__ configurada i persistida."
'@
$RemoteScript = $RemoteTemplate.Replace('__SERVER_PORT__', $ServerPort)

$rc = Invoke-RemoteBash -Target "${User}@${SshHost}" -Port $SshPort -SshOpts $SshOpts -Script $RemoteScript -SudoPassword $SudoPassword
$SudoPassword = $null
exit $rc