${labels.title_recipe}: ${recipe.name}
${labels.title_equipment}: ${recipe.equipmentProfile}
================================================================================
<#list recipe.steps as step>
${step?counter}. ${step.type}: ${step.name}
<#list step.instructions as instruction>
 - ${instruction}
</#list>
-------------------------------------------
</#list>

================================================================================
${labels.generated_by} Brewday ${version}
(${labels.with_freemarker} v${.version})
