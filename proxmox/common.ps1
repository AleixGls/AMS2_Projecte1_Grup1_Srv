# Funcions compartides pels scripts proxmox*.ps1 (es carrega amb dot-sourcing)

try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

function Import-Config {
    param([Parameter(Mandatory)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "No s'ha trobat el fitxer de configuració: $Path"
    }
    $cfg = @{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $l = $line.Trim()
        if ($l -eq '' -or $l.StartsWith('#')) { continue }
        $idx = $l.IndexOf('=')
        if ($idx -lt 1) { continue }
        $key = $l.Substring(0, $idx).Trim()
        $val = $l.Substring($idx + 1).Trim()
        if ($val.Length -ge 2 -and (($val.StartsWith('"') -and $val.EndsWith('"')) -or ($val.StartsWith("'") -and $val.EndsWith("'")))) {
            $val = $val.Substring(1, $val.Length - 2)
        }
        $val = $val.Replace('${HOME}', $HOME).Replace('$HOME', $HOME)
        $cfg[$key] = $val
    }
    return $cfg
}

function Get-ProxmoxConfig {
    $cfg = Import-Config (Join-Path $PSScriptRoot 'config.env')
    if (-not $cfg['SSH_HOST']) { $cfg['SSH_HOST'] = 'ieticloudpro.ieti.cat' }
    if (-not $cfg['SSH_PORT']) { $cfg['SSH_PORT'] = '20127' }
    return $cfg
}

function Resolve-RsaPath {
    param([string]$Path)
    $Path = $Path.TrimEnd("`r", "`n")
    if ($Path -eq '~' -or $Path.StartsWith('~/') -or $Path.StartsWith('~\')) {
        $Path = $HOME + $Path.Substring(1)
    }
    return $Path
}

function Get-SshOpts {
    param([Parameter(Mandatory)][string]$RsaPath)
    return @(
        '-i', $RsaPath,
        '-o', 'IdentitiesOnly=yes',
        '-o', 'HostKeyAlgorithms=+ssh-rsa',
        '-o', 'PubkeyAcceptedAlgorithms=+ssh-rsa'
    )
}

function Read-SudoPassword {
    $secure = Read-Host 'Pwd sudo remota' -AsSecureString
    return [System.Net.NetworkCredential]::new('', $secure).Password
}

# Executa un script bash al servidor remot. El script viatja en base64 (evita
# problemes de cometes, CRLF i codificació). La contrasenya sudo (opcional) viatja
# per stdin i queda a la variable d'entorn SUDO_PASSWORD, sense aparèixer a la
# línia de comandes. Retorna el codi de sortida d'ssh.
function Invoke-RemoteBash {
    param(
        [Parameter(Mandatory)][string]$Target,
        [Parameter(Mandatory)][string]$Port,
        [Parameter(Mandatory)][string[]]$SshOpts,
        [Parameter(Mandatory)][string]$Script,
        [string]$SudoPassword
    )
    $lf  = $Script -replace "`r`n", "`n"
    $b64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($lf))

    if ($SudoPassword) {
        $remote = 'IFS= read -r SUDO_PASSWORD; SUDO_PASSWORD=${SUDO_PASSWORD%[[:cntrl:]]}; export SUDO_PASSWORD; echo ' + $b64 + ' | base64 -d | bash -s'
        $SudoPassword | & ssh -T -p $Port @SshOpts $Target $remote | Out-Host
    } else {
        $remote = 'echo ' + $b64 + ' | base64 -d | bash -s'
        & ssh -T -p $Port @SshOpts $Target $remote | Out-Host
    }
    return $LASTEXITCODE
}