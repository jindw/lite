/*function * test(){
	yield 1;
	yield 2;
	yield this;
	yield 3;
}
//*/
var g = test.apply({a:"aaaa"},[]);
console.log( g.next());
console.log( g.next());
console.log( g.next().value.a);
console.log( g.next());
