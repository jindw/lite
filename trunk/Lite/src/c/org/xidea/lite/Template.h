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

#ifndef TEMPLATE_H_
#define TEMPLATE_H_
#define LITE_EL_TYPE 0/* [0,'el']*/
#define LITE_IF_TYPE 1/* [1,[...],'test']*/
#define LITE_BREAK_TYPE 2/* [2,depth]*/
#define LITE_XML_ATTRIBUTE_TYPE 3/* [3,'value','name']*/
#define LITE_XML_TEXT_TYPE 4/* [4,'el']*/
#define LITE_FOR_TYPE 5/* [5,[...],'var','items','status']#status*/
#define LITE_ELSE_TYPE 6/* [6,[...],'test']#test opt?*/
#define LITE_ADD_ONS_TYPE 7/* [7,[...],'var']*/
#define LITE_VAR_TYPE 8/* [8,'value','name']*/
#define LITE_CAPTRUE_TYPE 9/* [9,[...],'var']*/
#define LITE_FOR_KEY "for"
#define LITE_IF_KEY "if"


using namespace std;

#ifdef __cplusplus

namespace lite {

class Template {
public:
	Template();
	void render(json_object *context);
	virtual ~Template();
};
}
#else/*C 实现*/
void lite_render (json_array *stack,json_object *context);
#endif /* TEMPLATE2_H_ */
