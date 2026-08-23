$job1 = Start-Job -ScriptBlock {
    $c = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 6379)
    $s = $c.GetStream()
    $w = New-Object System.IO.StreamWriter($s)
    $r = New-Object System.IO.StreamReader($s)
    $w.WriteLine("PING from Client 1")
    $w.Flush()
    $resp = $r.ReadLine()
    $c.Close()
    return "Client 1 got: $resp"
}

$job2 = Start-Job -ScriptBlock {
    $c = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 6379)
    $s = $c.GetStream()
    $w = New-Object System.IO.StreamWriter($s)
    $r = New-Object System.IO.StreamReader($s)
    $w.WriteLine("PING from Client 2")
    $w.Flush()
    $resp = $r.ReadLine()
    $c.Close()
    return "Client 2 got: $resp"
}

Wait-Job $job1, $job2
Receive-Job $job1
Receive-Job $job2
Remove-Job $job1, $job2 | Out-Null
