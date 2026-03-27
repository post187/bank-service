# Chay tu thu muc demo:  .\spark-labs\run-spark-labs.ps1
$ErrorActionPreference = "Stop"
$demo = Split-Path -Parent $PSScriptRoot
Set-Location $demo

Write-Host ">>> docker compose pull / up ..."
docker compose -f docker-compose.spark.yml pull
docker compose -f docker-compose.spark.yml up -d

Write-Host ">>> Cho master + worker san sang (25s) ..."
Start-Sleep -Seconds 25

$sb = "/opt/bitnami/spark/bin/spark-submit"
$m = "spark://spark:7077"

Write-Host "`n========== BAI 1: WordCount RDD ==========`n"
docker exec spark-master $sb --master $m --deploy-mode client /data/wordcount_rdd.py "file:///data/sample_words.txt" 2>&1 | Tee-Object -FilePath "$PSScriptRoot\output_bai1.txt"

Write-Host "`n========== BAI 2: NYC Taxi DataFrame ==========`n"
docker exec spark-master $sb --master $m --deploy-mode client /data/nyc_taxi_tips_df.py "file:///data/sample_taxi.csv" 2>&1 | Tee-Object -FilePath "$PSScriptRoot\output_bai2.txt"

Write-Host "`n>>> Ket qua luu: spark-labs\output_bai1.txt, output_bai2.txt"
Write-Host ">>> Chup anh: http://localhost:8080 (Master) | http://localhost:4040 (App UI khi job chay) | worker:8081, :8082"
Write-Host ">>> Dung cum: docker compose -f docker-compose.spark.yml down"
