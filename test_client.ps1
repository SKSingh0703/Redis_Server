$client = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 6379)
$stream = $client.GetStream()
$writer = New-Object System.IO.StreamWriter($stream)
$reader = New-Object System.IO.StreamReader($stream)

Write-Host "Sending PING #1..."
$writer.WriteLine("PING")
$writer.Flush()
Write-Host "Response #1:" $reader.ReadLine()

Write-Host "Sending PING #2..."
$writer.WriteLine("PING")
$writer.Flush()
Write-Host "Response #2:" $reader.ReadLine()

$client.Close()
