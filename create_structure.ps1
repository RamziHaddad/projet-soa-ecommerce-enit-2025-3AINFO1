$root = "mock-catalogue"
$dirs = @(
    "$root\src\main\java\com\example\mockcatalogue\config",
    "$root\src\main\java\com\example\mockcatalogue\controller",
    "$root\src\main\java\com\example\mockcatalogue\model",
    "$root\src\main\java\com\example\mockcatalogue\repository",
    "$root\src\main\java\com\example\mockcatalogue\service",
    "$root\src\main\resources"
)

foreach ($d in $dirs) {
    New-Item -ItemType Directory -Path $d -Force | Out-Null
}

$files = @(
    "$root\src\main\java\com\example\mockcatalogue\MockCatalogueApplication.java",
    "$root\src\main\java\com\example\mockcatalogue\controller\ProductController.java",
    "$root\src\main\java\com\example\mockcatalogue\model\Product.java",
    "$root\src\main\java\com\example\mockcatalogue\repository\ProductRepository.java",
    "$root\src\main\java\com\example\mockcatalogue\service\ProductService.java",
    "$root\src\main\resources\application.properties",
    "$root\pom.xml",
    "$root\Dockerfile"
)

foreach ($f in $files) {
    if (-not (Test-Path $f)) {
        New-Item -ItemType File -Path $f | Out-Null
    }
}

Write-Host "Structure de projet 'mock-catalogue' créée avec succès."