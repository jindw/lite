var Env = require("org/xidea/lite/tools/internal/env");
var Merge = require("./merge");
var XHtml = require("./xhtml");
var Html = require("./html");
var Sprite = require('./sprite');
var Code = require('./code')

/*
 * png 过滤 + css自动转换
 */
Sprite.setupSprite('/images/','/images/_/');

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

