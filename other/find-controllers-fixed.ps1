# Script PowerShell per trovare tutti i file Controller.java
# Cerca ricorsivamente in tutte le sottocartelle

# Definisce il percorso base del progetto
$BasePath = "c:\Users\ascol\Progetti\greedys_api"

Write-Host "Cercando file *Controller.java in $BasePath e sottocartelle..." -ForegroundColor Green
Write-Host ""

# Cerca tutti i file che terminano con "Controller.java" ricorsivamente
$ControllerFiles = Get-ChildItem -Path $BasePath -Filter "*Controller.java" -Recurse -File

if ($ControllerFiles.Count -eq 0) {
    Write-Host "Nessun file Controller.java trovato." -ForegroundColor Yellow
} else {
    Write-Host "Trovati $($ControllerFiles.Count) file Controller:" -ForegroundColor Cyan
    Write-Host ""
    
    foreach ($file in $ControllerFiles) {
        Write-Host "📁 $($file.FullName)" -ForegroundColor White
        Write-Host "   Dimensione: $([math]::Round($file.Length / 1KB, 2)) KB" -ForegroundColor Gray
        Write-Host "   Ultima modifica: $($file.LastWriteTime)" -ForegroundColor Gray
        Write-Host ""
    }
    
    # Opzionale: salva la lista in un file di testo
    Write-Host "Vuoi salvare la lista in un file? (y/n): " -NoNewline -ForegroundColor Yellow
    $SaveChoice = Read-Host
    
    if ($SaveChoice -eq "y" -or $SaveChoice -eq "Y") {
        $OutputFile = Join-Path $BasePath "controller-files-list.txt"
        $ControllerFiles | ForEach-Object { $_.FullName } | Out-File -FilePath $OutputFile -Encoding UTF8
        Write-Host "Lista salvata in: $OutputFile" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Ricerca completata!" -ForegroundColor Green
