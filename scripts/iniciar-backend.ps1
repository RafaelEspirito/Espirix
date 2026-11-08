param([Parameter(Mandatory=$true)][string]$Email)
$ErrorActionPreference = 'Stop'
$env:ESPIRIX_MASTER_EMAIL = $Email
$secure = Read-Host 'Senha nova do AdminMaster (mínimo 12 caracteres)' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:ESPIRIX_MASTER_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}
if ($env:ESPIRIX_MASTER_PASSWORD.Length -lt 12) { throw 'Senha deve conter pelo menos 12 caracteres.' }
try {
    mvn.cmd -f backend/pom.xml spring-boot:run
} finally {
    Remove-Item Env:ESPIRIX_MASTER_PASSWORD -ErrorAction SilentlyContinue
}
