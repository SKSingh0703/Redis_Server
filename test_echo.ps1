$c = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 6379)
$s = $c.GetStream()
$w = New-Object System.IO.StreamWriter($s)
$r = New-Object System.IO.StreamReader($s)

$w.WriteLine("*2")
$w.WriteLine("$4")
$w.WriteLine("ECHO")
$w.WriteLine("$11")
$w.WriteLine("hello world")
$w.Flush()

$line1 = $r.ReadLine()
$line2 = $r.ReadLine()

Write-Host "Response Header:" $line1
Write-Host "Response Body:  " $line2

$c.Close()
