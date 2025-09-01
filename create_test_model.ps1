# 生成一个最小的NCNN二进制权重文件用于测试
$bytes = [byte[]]::new(1024)  # 1KB的测试数据
for ($i = 0; $i -lt $bytes.Length; $i++) {
    $bytes[$i] = [byte]($i % 256)
}
[System.IO.File]::WriteAllBytes("yolov5n_test.bin", $bytes)
Write-Host "Created test binary file: yolov5n_test.bin (1KB)"
