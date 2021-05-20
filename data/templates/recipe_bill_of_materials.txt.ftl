${labels.title_recipe}: ${recipe.name}
${labels.title_equipment}: ${recipe.equipmentProfile}
================================================================================
<#list recipe.ingredientsBillOfMaterials as ia>
${ia.describe()}
</#list>
================================================================================
${labels.generated_by} Brewday ${version}
