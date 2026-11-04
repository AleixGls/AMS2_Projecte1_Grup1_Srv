$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

$mainClass = if ($args.Count -gt 0) {
    $args[0]
} else {
    "grup1.server.Main"
}

# Compilar y generar classpath
mvn compile dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

# Obtener dependencias Maven
$dependencyPath = (Get-Content "target/classpath.txt" -Raw).Trim()

# Construir classpath
$classPath = "target/classes;$dependencyPath"

# Ejecutar servidor
java -cp $classPath $mainClass

exit $LASTEXITCODE