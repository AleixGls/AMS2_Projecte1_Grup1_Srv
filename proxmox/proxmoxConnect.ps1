# Connects to the remote server using SSH
param(
    [string]$User,
    [string]$RsaPath
)

. (Join-Path $PSScriptRoot 'common.ps1')
$Config = Get-ProxmoxConfig

if (-not $User)    { $User    = $Config['DEFAULT_USER'] }
if (-not $RsaPath) { $RsaPath = $Config['DEFAULT_RSA_PATH'] }
$RsaPath = Resolve-RsaPath $RsaPath
$SshHost = $Config['SSH_HOST']
$SshPort = $Config['SSH_PORT']

Write-Host "User: $User"
Write-Host "Ruta RSA: $RsaPath"

if (-not (Test-Path -LiteralPath $RsaPath -PathType Leaf)) {
    Write-Host "Error: No s'ha trobat el fitxer de clau privada: $RsaPath"
    exit 1
}

# Establish SSH connection
$SshOpts = Get-SshOpts $RsaPath
& ssh -p $SshPort @SshOpts "${User}@${SshHost}"
exit $LASTEXITCODE