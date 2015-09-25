function* test(a, b){
	yield 1;
	yield 2;
}
function* test2(){
	console.log('##',(yield* test()))
	console.log('#',(yield* test()))
	if ((yield* test()) || true) {
		console.log(1);
	};
	if ((yield* test()) || true) {
		console.log(2);
	};
	if ((yield* test()) || true) {
		console.log(3);
	};
	if ((yield* test()) && true) {
		console.log(4);
	};
	if ((yield* test()) && true) {
		console.log(5);
	};
	if ((yield* test()) && true) {
		console.log(6);
	};
}

var g = test2();
g.next();
g.next();
g.next();
g.next();
g.next();
g.next();
g.next();
g.next();
g.next();
g.next();
g.next();
g.next();
g.next();
g.next();
console.log(1111111)



return;
function* nest(){
	var i = arguments.length;
	while(i--){
		if (arguments[i] instanceof Promise) {yield arguments[i]};
	}
}

var g = test(1,2);
console.log(2222,g)
for(var i = 0;i<=10;i++){
	console.log(0)
	console.log(g,g.next());
}


var p = new Promise(function(resolve,reject){
	console.log('$$$$$$$')
	resolve(1);
})
p.then(function(a){console.log('%%%',a)})
p.then(function(a){console.log('%%%',a)})
p.then(function(a){console.log('%%%',a)})