<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="org.onehippo.forge.content.exim.demo.beans.FaqItem" -->
<#if document??>
<div class="has-edit-button">
<@hst.cmseditlink hippobean=document/>
<h1>${document.question?html}</h1>
<@hst.html hippohtml=document.answer />
</div>
</#if>


