/*
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * @author jindw@xidea.org
 *
 */
#include "../json/json.h"

#ifndef EXPRESSION_H_
#define EXPRESSION_H_

/*value token（<=0）*/
#define LITE_EL_VALUE_CONSTANTS -0
#define LITE_EL_VALUE_VAR -1
#define LITE_EL_VALUE_LAZY -2
#define LITE_EL_VALUE_NEW_LIST -3
#define LITE_EL_VALUE_NEW_MAP -4

/*符号标记*/
/*9*/
#define LITE_EL_OP_GET_PROP 17;/*0 | 16 | 1;*/
#define LITE_EL_OP_STATIC_GET_PROP 48;/*32 | 16 | 0;*/
#define LITE_EL_OP_INVOKE_METHOD 81;/*64 | 16 | 1;*/
/*8*/
#define LITE_EL_OP_NOT 14;/*0 | 14 | 0;*/
#define LITE_EL_OP_POS 46;/*32 | 14 | 0;*/
#define LITE_EL_OP_NEG 78;/*64 | 14 | 0;*/
/*7*/
#define LITE_EL_OP_MUL 13;/*0 | 12 | 1;*/
#define LITE_EL_OP_DIV 45;/*32 | 12 | 1;*/
#define LITE_EL_OP_MOD 77;/*64 | 12 | 1;*/
/*6*/
#define LITE_EL_OP_ADD 11;/*0 | 10 | 1;*/
/*5*/
#define LITE_EL_OP_SUB 41;/*32 | 8 | 1;*/
/*4*/
#define LITE_EL_OP_LT 7;/*0 | 6 | 1;*/
#define LITE_EL_OP_GT 39;/*32 | 6 | 1;*/
#define LITE_EL_OP_LTEQ 71;/*64 | 6 | 1;*/
#define LITE_EL_OP_GTEQ 103;/*96 | 6 | 1;*/
#define LITE_EL_OP_EQ 135;/*128 | 6 | 1;*/
#define LITE_EL_OP_NOTEQ 167;/*160 | 6 | 1;*/
/*3*/
#define LITE_EL_OP_AND 5;/*0 | 4 | 1;*/
#define LITE_EL_OP_OR 37;/*32 | 4 | 1;*/
/*2*/
#define LITE_EL_OP_QUESTION 3;/*0 | 2 | 1;*/
#define LITE_EL_OP_QUESTION_SELECT 35;/*32 | 2 | 1;*/
/*1*/
#define LITE_EL_OP_PARAM_JOIN 1;/*0 | 0 | 1;*/
#define LITE_EL_OP_MAP_PUSH 33;/*32 | 0 | 1;*/

#ifdef __cplusplus
namespace lite {
class Expression {
public:
	Expression();
	virtual ~Expression();
	Object evaluate(json_object *context);
protected:
	json_array *stack;
};
}
#else
json_value* lite_evaluate (json_array *stack,json_object *context);
#endif

#endif /* EXPRESSION_H_ */
