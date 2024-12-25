# Make outputs from resource group deployment available to subsequent tasks

$outputs = ConvertFrom-Json $($env:ResourceGroupDeploymentOutputs)

foreach ($output in $outputs.PSObject.Properties) {
  Write-Output $($output.Name)
  Write-Host "##vso[task.setvariable variable=$($output.Name)]$($output.Value.value)"
}