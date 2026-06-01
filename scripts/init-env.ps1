param(
    [string]$OutputPath = ".env"
)

$ErrorActionPreference = "Stop"

function ConvertTo-PlainText {
    param([System.Security.SecureString]$SecureValue)

    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function New-HexSecret {
    param([int]$Bytes)

    $buffer = [byte[]]::new($Bytes)
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($buffer)
    return ($buffer | ForEach-Object { $_.ToString("x2") }) -join ""
}

function Format-EnvValue {
    param([string]$Value)

    if ($null -eq $Value) {
        return "''"
    }
    return "'" + $Value.Replace("'", "\'") + "'"
}

function Test-PortAvailable {
    param([int]$Port)

    $listener = $null
    try {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $Port)
        $listener.Start()
        return $true
    } catch {
        return $false
    } finally {
        if ($listener -ne $null) {
            $listener.Stop()
        }
    }
}

function Read-Port {
    param(
        [string]$Prompt,
        [int]$DefaultPort
    )

    while ($true) {
        $inputValue = Read-Host "$Prompt（直接回车使用 $DefaultPort）"
        $selectedPort = if ([string]::IsNullOrWhiteSpace($inputValue)) { $DefaultPort } else { 0 }
        if (-not [string]::IsNullOrWhiteSpace($inputValue) -and -not [int]::TryParse($inputValue, [ref]$selectedPort)) {
            Write-Host "端口必须是 1-65535 之间的数字。" -ForegroundColor Yellow
            continue
        }
        if ($selectedPort -lt 1 -or $selectedPort -gt 65535) {
            Write-Host "端口必须是 1-65535 之间的数字。" -ForegroundColor Yellow
            continue
        }
        if (-not (Test-PortAvailable -Port $selectedPort)) {
            Write-Host "提示：端口 $selectedPort 当前已被占用，按该端口启动可能失败。" -ForegroundColor Yellow
            $answer = Read-Host "仍然使用这个端口吗？输入 YES 确认"
            if ($answer -ne "YES") {
                continue
            }
        }
        return $selectedPort
    }
}

function Read-RequiredSecret {
    param([string]$Prompt)

    while ($true) {
        $value = ConvertTo-PlainText (Read-Host $Prompt -AsSecureString)
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            return $value
        }
        Write-Host "该项不能为空，请重新输入。" -ForegroundColor Yellow
    }
}

$resolvedOutput = Resolve-Path -Path "." | ForEach-Object {
    Join-Path $_.Path $OutputPath
}

if (Test-Path $resolvedOutput) {
    $answer = Read-Host ".env 已存在，是否覆盖？输入 YES 确认覆盖"
    if ($answer -ne "YES") {
        Write-Host "已取消，未修改 $resolvedOutput"
        exit 0
    }
}

$dbUserInput = Read-Host "请输入数据库业务用户名（直接回车使用 cooperation_user）"
$dbUser = if ([string]::IsNullOrWhiteSpace($dbUserInput)) { "cooperation_user" } else { $dbUserInput.Trim() }
$dbPassword = Read-RequiredSecret "请输入数据库业务密码"
$mysqlRootPassword = Read-RequiredSecret "请输入 MySQL root 管理员密码"
$mysqlPort = Read-Port -Prompt "请输入 MySQL 宿主机端口" -DefaultPort 3306
$backendPort = Read-Port -Prompt "请输入后端服务端口" -DefaultPort 8080
$frontendPort = Read-Port -Prompt "请输入前端服务端口" -DefaultPort 5173
$aesKey = New-HexSecret -Bytes 32
$apiBaseUrl = "http://localhost:$backendPort"
$envMysqlDatabase = Format-EnvValue "cooperation"
$envMysqlUser = Format-EnvValue $dbUser
$envMysqlPassword = Format-EnvValue $dbPassword
$envMysqlRootPassword = Format-EnvValue $mysqlRootPassword
$envSpringProfile = Format-EnvValue "local"
$envStorageRoot = Format-EnvValue "./data/storage"
$envAesKey = Format-EnvValue $aesKey
$envApiBaseUrl = Format-EnvValue $apiBaseUrl

$content = @"
# MYSQL_DATABASE：本地 MySQL 数据库名称。
MYSQL_DATABASE=$envMysqlDatabase

# MYSQL_USER：后端连接数据库使用的业务账号，禁止生产环境使用 root。
MYSQL_USER=$envMysqlUser

# MYSQL_PASSWORD：后端连接数据库使用的业务账号密码。
MYSQL_PASSWORD=$envMysqlPassword

# MYSQL_ROOT_PASSWORD：MySQL root 初始化密码。
MYSQL_ROOT_PASSWORD=$envMysqlRootPassword

# MYSQL_PORT：宿主机暴露的 MySQL 端口。
MYSQL_PORT=$mysqlPort

# BACKEND_PORT：后端 HTTP 服务端口。
BACKEND_PORT=$backendPort

# FRONTEND_PORT：前端预览服务端口。
FRONTEND_PORT=$frontendPort

# SPRING_PROFILES_ACTIVE：后端激活环境。
SPRING_PROFILES_ACTIVE=$envSpringProfile

# APP_FILE_STORAGE_ROOT：后端文件存储根目录，必须限制在可控目录内。
APP_FILE_STORAGE_ROOT=$envStorageRoot

# APP_AES_KEY：SMTP 密码加密密钥，由脚本自动生成。
APP_AES_KEY=$envAesKey

# VITE_API_BASE_URL：浏览器访问后端接口的基础地址。
VITE_API_BASE_URL=$envApiBaseUrl

# MAIL_ENABLED：默认不启用部署态 SMTP；用户可在个人中心配置自己的 SMTP。
MAIL_ENABLED=false

# MAIL_HOST：部署态 SMTP 服务器地址，默认留空。
MAIL_HOST=

# MAIL_PORT：部署态 SMTP 端口。
MAIL_PORT=465

# MAIL_USERNAME：部署态 SMTP 登录账号，默认留空。
MAIL_USERNAME=

# MAIL_PASSWORD：部署态 SMTP 授权码或密码，默认留空。
MAIL_PASSWORD=

# MAIL_FROM：部署态发件人邮箱，默认留空。
MAIL_FROM=

# MAIL_SSL_ENABLED：是否启用 SSL 直连模式。
MAIL_SSL_ENABLED=true

# MAIL_STARTTLS_ENABLED：是否启用 STARTTLS。
MAIL_STARTTLS_ENABLED=false
"@

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($resolvedOutput, $content + [Environment]::NewLine, $utf8NoBom)

Write-Host "已生成 $resolvedOutput" -ForegroundColor Green
Write-Host "配置端口：MySQL=$mysqlPort，Backend=$backendPort，Frontend=$frontendPort"
Write-Host "SMTP 已保持关闭；需要时可登录系统后在个人中心配置。"
Write-Host "下一步运行：docker compose up --build"
