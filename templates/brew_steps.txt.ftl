RECIPE: ${recipe.name}
EQUIPMENT: ${recipe.equipmentProfile}
================================================================================
<#list recipe.steps as step>
STEP: ${step.name}
<#list step.ingredients as ingredient>
 - ${ingredient.name}
</#list>
-------------------------------------------
</#list>

