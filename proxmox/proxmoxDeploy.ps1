# Compila el JAR, l'envia al servidor i reinicia el procés
param(
    [string]$User,
    [string]$RsaPath,
    [string]$ServerPort
)

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
Write-Host "Ruta RSA: $RsaPath"
Write-Host "Server port: $ServerPort"

$JarName    = 'server-package.jar'
$ProjectDir = Split-Path -Parent $PSScriptRoot
$JarPath    = Join-Path $ProjectDir "target\$JarName"

$RemoteTemplate = @'
cd "$HOME/"

PID_JAVA=$(ps aux | grep 'java -jar __JAR_NAME__' | grep -v 'grep' | awk '{print $2}')
if [ -n "$PID_JAVA" ]; then
  kill -15 $PID_JAVA
  echo "Senyal SIGTERM enviat al procés $PID_JAVA."
  for i in {1..10}; do
    if ! ps -p $PID_JAVA > /dev/null; then
      echo "Procés $PID_JAVA aturat correctament."
      break
    fi
    echo "Esperant que el procés finalitzi..."
    sleep 1
  done
  if ps -p $PID_JAVA > /dev/null; then
    echo "Procés $PID_JAVA encara actiu, forçant aturada..."
    kill -9 $PID_JAVA
  fi
else
  echo "No s'ha trobat el procés __JAR_NAME__."
fi

MAX_RETRIES=10
RETRIES=0
while netstat -an | grep -q ':__SERVER_PORT__.*LISTEN'; do
  echo "Esperant que el port __SERVER_PORT__ es desalliberi..."
  sleep 1
  RETRIES=$((RETRIES + 1))
  if [ $RETRIES -ge $MAX_RETRIES ]; then
    echo "Error: El port __SERVER_PORT__ no es desallibera després de $MAX_RETRIES segons."
    exit 1
  fi
done

setsid nohup java -jar __JAR_NAME__ > output.log 2>&1 < /dev/null &
sleep 1
PID_JAVA=$(ps aux | grep 'java -jar __JAR_NAME__' | grep -v 'grep' | awk '{print $2}')
if [ -n "$PID_JAVA" ]; then
  echo "Nou procés __JAR_NAME__ amb PID $PID_JAVA arrencat correctament."
else
  echo "Error: No s'ha pogut arrencar el nou procés __JAR_NAME__."
  exit 1
fi
'@
$RemoteScript = $RemoteTemplate.Replace('__JAR_NAME__', $JarName).Replace('__SERVER_PORT__', $ServerPort)

Push-Location $ProjectDir
try {
    if (-not (Test-Path -LiteralPath $RsaPath -PathType Leaf)) {
        Write-Host "Error: No s'ha trobat el fitxer de clau privada: $RsaPath"
        exit 1
    }

    Write-Host "Generant el fitxer JAR..."
    & mvn clean package -Pserver -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: No s'ha pogut generar el JAR."
        exit 1
    }

    $BuiltJar = Join-Path $ProjectDir 'target\demo-1.0-SNAPSHOT-server.jar'
    if (Test-Path -LiteralPath $BuiltJar -PathType Leaf) {
        Move-Item -LiteralPath $BuiltJar -Destination $JarPath -Force
    }

    if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
        Write-Host "Error: No s'ha trobat l'arxiu JAR: $JarPath"
        exit 1
    }

    Write-Host "Enviant $JarPath al servidor..."
    & scp -P $SshPort @SshOpts $JarPath "${User}@${SshHost}:~/"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error durant l'enviament SCP"
        exit 1
    }

    $rc = Invoke-RemoteBash -Target "${User}@${SshHost}" -Port $SshPort -SshOpts $SshOpts -Script $RemoteScript
    exit $rc
}
finally {
    Pop-Location
}