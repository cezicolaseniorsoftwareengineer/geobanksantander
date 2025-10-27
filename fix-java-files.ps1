# Corrigir arquivos Java corrompidos
$basePath = "c:\Users\LENOVO\OneDrive\Área de Trabalho\GeoBankSantander\geobank-api\src"

Get-ChildItem -Path $basePath -Filter "*.java" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    # Corrigir 'ackage' para 'package'
    $content = $content -replace '^ackage\s+', 'package '
    # Remover BOM se presente
    if ($content.StartsWith([char]0xFEFF)) {
        $content = $content.Substring(1)
    }
    # Salvar sem BOM
    [System.IO.File]::WriteAllText($_.FullName, $content, [System.Text.UTF8Encoding]::new($false))
    Write-Host "Fixed: $($_.FullName)"
}
