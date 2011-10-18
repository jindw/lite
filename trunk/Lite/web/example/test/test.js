var modelA ={
  id  : 'a',
  tpl : liteWrap(function(title,list){
  		//E4X 语法
	    <div class="modelA">
          <h2 class="model-title">${title}</h2>
          <ul>
            <li c:for="${item:list}">${item}</li>
          </ul>
        </div>
  }),
  render:function(title,list){
      $(this.id).innerHTML = this.tpl(title,list)
  }
}