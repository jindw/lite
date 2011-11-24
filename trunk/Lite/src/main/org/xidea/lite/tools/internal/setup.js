var Env = require("org/xidea/lite/tools/internal/env");
var Merge = require("org/xidea/lite/tools/internal/merge");
var XHtml = require("org/xidea/lite/tools/internal/xhtml");
var Html = require("org/xidea/lite/tools/internal/html");
var Sprite = require('org/xidea/lite/tools/internal/sprite');
var Code = require('org/xidea/lite/tools/internal/code')

/*
 * png 过滤 + css自动转换
 */
Sprite.setupSprite('/images/','/images/_/');

//$JSI amd js
Env.addBytesFilter('**__define__.js',function(path){
	return Env.getRawBytes(path.replace(/__define__\.js$/,'.js'));
})

Env.addTextFilter('**__define__.js',Code.jsiDefine)

//js / html 过滤处理
Env.addTextFilter('**.js',Merge.jsMergeFilter);
Env.addTextFilter('**.js',Code.jsCodeFilter);

Env.addTextFilter('**.css', Merge.cssMergeFilter);
Env.addTextFilter('**.css', Code.cssCodeFilter);

//预处理HTML中的js/css标签内容（xhtml是Lite模板，相关处理可以直接在DOM中处理）
Env.addTextFilter("/**.html",Html.scFilter);
Env.addTextFilter("/**.ftl",Html.scFilter);
Env.addTextFilter("/**.vm",Html.scFilter);



/* XML正规化，用于兼容html常用不严谨的书写习惯 */
Env.addTextFilter("/**.xhtml",XHtml.xhtmlNormalizeFilter);
/* 添加文档验证器 */
Env.addDocumentFilter("/**.xhtml",XHtml.xhtmlValidateFilter);
Env.addDocumentFilter("/**.xhtml",XHtml.xhtmlDOMFilter);


