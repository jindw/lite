<html>
	<title>模板测试</title>
	<body>
		<p>我是：${name},来自：${city}</p>
		<p>
			<#list friends as friend>
				<span>${friend}</span>,
			</#list>
			都是我的朋友
		</p>
	</body>
</html>