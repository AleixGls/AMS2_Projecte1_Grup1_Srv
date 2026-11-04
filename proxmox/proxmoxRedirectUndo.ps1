# Remove only NAT PREROUTING REDIRECT rules for dport 80 and persist.
param(
    [string]$User,
    [string]$RsaPath,
    [string]$ServerPort   # no es necessari; s'eliminen TOTES les redireccions del 80
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

Write-Host "User: $User"
Write-Host "RSA:  $RsaPath"

if (-not (Test-Path -LiteralPath $RsaPath -PathType Leaf)) {
    Write-Host "Error: no troba la clau: $RsaPath"
    exit 1
}

$SudoPassword = Read-SudoPassword

$RemoteScript = @'
set -euo pipefail

# Sanity check: ensure sudo works non-interactively
echo "$SUDO_PASSWORD" | sudo -S -k true >/dev/null 2>&1

# List candidate NAT rules for dport 80 with REDIRECT
CANDIDATES=$(echo "$SUDO_PASSWORD" | sudo -S iptables-save -t nat | awk '
  BEGIN{inNat=0}
  /^\*nat/{inNat=1; next}
  /^COMMIT/{inNat=0}
  inNat && /-A PREROUTING/ && /--dport 80/ && /-j REDIRECT/ {print}
')

if [[ -z "$CANDIDATES" ]]; then
  echo "No hi ha redireccions de port 80 a eliminar."
else
  echo "Eliminant redireccions de port 80:"
  while read -r LINE; do
    [[ -z "$LINE" ]] && continue
    CMD=$(echo "$LINE" | sed 's/^-A PREROUTING/-D PREROUTING/')
    echo " - iptables -t nat $CMD"
    echo "$SUDO_PASSWORD" | sudo -S iptables -t nat $CMD
  done <<< "$CANDIDATES"
fi

# Persist full rules (no deletes of rules.v4)
TMP=$(mktemp)
echo "$SUDO_PASSWORD" | sudo -S iptables-save > "$TMP"
echo "$SUDO_PASSWORD" | sudo -S install -m 600 "$TMP" /etc/iptables/rules.v4
rm -f "$TMP"
echo "$SUDO_PASSWORD" | sudo -S systemctl restart netfilter-persistent || true

echo "✔︎  Redireccions del port 80 eliminades i configuració persistent actualitzada."
'@

$rc = Invoke-RemoteBash -Target "${User}@${SshHost}" -Port $SshPort -SshOpts $SshOpts -Script $RemoteScript -SudoPassword $SudoPassword
$SudoPassword = $null
exit $rc