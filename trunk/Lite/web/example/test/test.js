var modelA ={
  id  : 'a',
  tpl : liteWrap(function(title,list){
    //E4X 语法
    <div class="modelA">
      <!-- 插入标题 -->
        <h2 class="model-title">${title}</h2>
        <ul>
          <!-- 循环输出内容 -->
          <li c:for="${item:list}">
            <strong>${item.title}</string>
            <c:if test="${item.content}">
              <p>${item.content}</p>
            </c:if>
          </li>
        </ul>
    </div>
  }),
  render:function(title,list){
    $(this.id).innerHTML = this.tpl(title,list)
  }
}