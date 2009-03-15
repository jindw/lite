/*
 * Expression.c
 *
 *  Created on: 2009-3-15
 *      Author: jindw
 */
#include "Expression.h"
#include "../json/json.h"
void do_evaluate(struct json_value* value_stack, struct json_value* el,
		struct json_value* context);
struct json_value* getTokenValue(int type,struct json_value* context,
		struct json_value* op);
struct json_value* compute(int type,struct json_value* op,
		struct json_value* arg1,
		struct json_value* arg2);

struct json_value* lite_evaluate(struct json_value *el,
		struct json_value *context) {
	json_value* value_stack = json_create_array();

	do_evaluate(value_stack, el, context);
	return json_get_by_index(value_stack, 0);
}

void do_evaluate(struct json_value* value_stack, struct json_value* el,
		struct json_value* context) {
	int el_length = json_get_length(el);
	int i = 0;
	for (; i < el_length; i++) {
		struct json_value* item = json_get_by_index(el, i);
		int type = json_get_int(json_get_by_index(item, 0));
		if (type > 0) {
			int pos = json_get_length(value_stack);
			struct json_value* arg1 = json_get_by_index(value_stack,pos - 1);
			struct json_value* arg2 = arg1;
			if (type & 1) {
				arg1 = json_get_by_index(value_stack,pos - 2);
			}
			struct json_value* result = compute(type,item, arg1, arg2);
			//if($result instanceof LazyToken) {
			//	_evaluate($stack, $result->children, $context);
			//} else {
			json_add_value(value_stack, result);
			//}
		} else {
			json_add_value(value_stack, getTokenValue(type,context, item));
		}
	}
}
struct json_value* getTokenValue(int type,struct json_value* context,
		struct json_value* op) {
	char* key;
	switch (type) {
	case LITE_EL_VALUE_CONSTANTS:
		return json_get_by_index(op,1);
	case LITE_EL_VALUE_VAR:
		key = json_get_string(json_get_by_index(op,1));
		return json_get_by_key(context,key);
	case LITE_EL_VALUE_NEW_LIST:
		return json_create_array();
	case LITE_EL_VALUE_NEW_MAP:
		return json_create_object();
	case LITE_EL_VALUE_LAZY:
		//TODO:
		return NULL;//LazyToken($item[1]);
	}
	return NULL;
}
struct json_value* compute(int type,struct json_value* op,
		struct json_value* arg1,
		struct json_value* arg2){
	if (type == LITE_EL_OP_INVOKE_METHOD) {
//		if(arg1 instanceof PropertyValue) {
//			$base = $arg1->base;
//			$name = $arg1->name;
//			if (is_array($arg1) && array_key_exists($name, $base)) {
//				return call_user_func_array($base[$name], $arg2);
//			} else {
//				return call_user_func_array(array($base,$name),$arg2);
//			}
//		} else {
//			return call_user_func($arg1, $arg2);
//		}
	}
//	if (arg1 instanceof PropertyValue) {
//		arg1 = $arg1->base[$arg1->name];
//	}
//	if ($arg2 instanceof PropertyValue) {
//		$arg2 = $arg2->base[$arg2->name];
//	}

	switch(type) {
//		case LITE_EL_OP_STATIC_GET_PROP:
//			return new PropertyValue($arg1, $op[1]);
//		case LITE_EL_OP_GET_PROP:
//			return new PropertyValue($arg1, $arg2);
		case LITE_EL_OP_PARAM_JOIN:
			json_add_value(arg1,arg2);
			return arg1;
		case LITE_EL_OP_MAP_PUSH:

			json_set_by_key(arg1,json_get_string(json_get_by_index(op,1)),arg2);
			return arg1;
		case LITE_EL_OP_NOT:
			//TODO:GC
			return json_create_bool(!json_get_bool(arg1));
		case LITE_EL_OP_POS:
			//TODO:GC
			return arg1;
		case LITE_EL_OP_NEG:
			//TODO:GC
			//TODO:OP
			return arg1;
		/* +-*%/ */
		case LITE_EL_OP_ADD:
			//TODO:GC
			return json_create_int(json_get_int(arg1)+json_get_int(arg2));
//		case LITE_EL_OP_SUB:
//			return $arg1-$arg2;
//		case LITE_EL_OP_MUL:
//			return $arg1*$arg2;
//		case LITE_EL_OP_DIV:
//			return $arg1/$arg2;
//		case LITE_EL_OP_MOD:
//			return $arg1%$arg2;
//		/* boolean */
//		case LITE_EL_OP_GT:
//			return $arg1>$arg2;
//		case LITE_EL_OP_GTEQ:
//			return $arg1>=$arg2;
//		case LITE_EL_OP_NOTEQ:
//			return $arg1!=$arg2;
//		case LITE_EL_OP_EQ:
//			return $arg1==$arg2;
//		case LITE_EL_OP_LT:
//			return $arg1<$arg2;
//		case LITE_EL_OP_LTEQ:
//			return $arg1<=$arg2;
//
//		/* and or */
//		case LITE_EL_OP_AND:
//			return $arg2 && $arg1;
//		case LITE_EL_OP_OR:
//			return $arg1 || $arg2;
//		case LITE_EL_OP_QUESTION:
//			return $arg1 ? $arg2 : new stdClass();
//		case LITE_EL_OP_MAP_PUSH:
//			$arg1[$op[1]] = $arg2;return $arg1;
//		default:break;
	}
	return NULL;
}
