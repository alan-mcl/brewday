<#macro label beer>
<div align="center"><b>BOTTLE ID FORM</b></div> <br><br>
<b>Brewer Name:</b> _________________________________________<br>
<br>
<b>Address:</b>
<br>
______________________________________________________<br>
<br>
______________________________________________________<br>
<br>
<b>Email:</b> _______________________________________________<br>
<br>
<b>Phone:</b> _______________________________________________<br>
<br>
<b>Name Of Beer:</b> ${beer.name} <br>
<br>
<b>Category Entered:</b> ${beer.style.categoryNumber} (${beer.style.category})<br>
<br>
<b>Subcategory Entered:</b> ${beer.style.styleLetter} (${beer.style.styleGuideName})<br>
<br>
<b>Homebrew Club:</b> _______________________________________<br>
<br>
</#macro>

<html>
<head>
<style>
table, th, td {
  border: 1px solid black;
}
</style>
</head>

<body>
<#list beers as beer>
<table cellpadding="10">
    <tr>
        <td><@label beer/></td>
        <td><@label beer/></td>
    </tr>
    <tr>
        <td><@label beer/></td>
        <td><@label beer/></td>
    </tr>
    <tr>
        <td><@label beer/></td>
        <td><@label beer/></td>
    </tr>
</table>
<div style="page-break-after: always;">&nbsp;</div>
</#list>
</body>
</html>