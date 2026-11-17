# Atura el procés del JAR al servidor remot
param(
    [string]$User,
    [string]$RsaPath,
    [string]$ServerPort
)

. (Join-Path $PSScriptRoot 'common.ps1')
$Config = Get-ProxmoxConfig

# Obtenir configuració dels paràmetres
if (-not $User)       { $User       = $Config['DEFAULT_USER'] }
if (-not $RsaPath)    { $RsaPath    = $Config['DEFAULT_RSA_PATH'] }
if (-not $ServerPort) { $ServerPort = $Config['DEFAULT_SERVER_PORT'] }
$RsaPath = Resolve-RsaPath $RsaPath
$SshHost = $Config['SSH_HOST']
$SshPort = $Config['SSH_PORT']
$SshOpts = Get-SshOpts $RsaPath

Write-Host "User: $User"
Write-Host "Ruta RSA: $RsaPath"
Write-Host "Server port: $ServerPort"

$JarName    = 'server-package.jar'
$ProjectDir = Split-Path -Parent $PSScriptRoot

$RemoteTemplate = @'
PID_JAVA=$(ps aux | grep 'java -jar __JAR_NAME__' | grep -v 'grep' | awk '{print $2}')
if [ -n "$PID_JAVA" ]; then
  # Envia un senyal de terminació suau
  kill -15 $PID_JAVA
  echo "Senyal SIGTERM enviat al procés $PID_JAVA."

  # Espera que el procés acabi correctament
  for i in {1..10}; do
    if ! ps -p $PID_JAVA > /dev/null; then
      echo "Procés $PID_JAVA aturat correctament."
      break
    fi
    echo "Esperant que el procés finalitzi..."
    sleep 1
  done

  # Força la terminació si encara està actiu
  if ps -p $PID_JAVA > /dev/null; then
    echo "Procés $PID_JAVA encara actiu, forçant aturada..."
    kill -9 $PID_JAVA
  fi
else
  echo "No s'ha trobat el procés __JAR_NAME__."
fi

# Comprova si el port està ocupat i espera fins que es desalliberi
while netstat -an | grep -q ':__SERVER_PORT__.*LISTEN'; do
  echo "Esperant que el port __SERVER_PORT__ es desalliberi..."
  sleep 1
done

echo "Port __SERVER_PORT__ desalliberat."
'@
$RemoteScript = $RemoteTemplate.Replace('__JAR_NAME__', $JarName).Replace('__SERVER_PORT__', $ServerPort)

Push-Location $ProjectDir
try {
    # Comprovem que els arxius existeixen
    if (-not (Test-Path -LiteralPath $RsaPath -PathType Leaf)) {
        Write-Host "Error: No s'ha trobat el fitxer de clau privada: $RsaPath"
        exit 1
    }

    $rc = Invoke-RemoteBash -Target "${User}@${SshHost}" -Port $SshPort -SshOpts $SshOpts -Script $RemoteScript
    exit $rc
}
finally {
    Pop-Location
}