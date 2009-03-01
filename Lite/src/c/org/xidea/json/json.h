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

#ifndef JSON_H
#define JSON_H
#include <stdio.h>

#ifdef __cplusplus
extern "C" {
#endif

#define JSON_MAX_STRING_LENGTH SIZE_MAX-1
#define JSON_ARRAY_STEP 8

enum json_type {
	JSON_INT = 0,
	JSON_FLOAT,
	JSON_BOOLEAN,
	JSON_STRING,
	JSON_OBJECT,
	JSON_ARRAY,
	JSON_NULL
};


typedef struct json_value {
	enum json_type type;
	union {
		int int_value;
		int intValue;
		float float_value;
		float floatValue;
		int boolean_value;
		int booleanValue;
		const char* string;
		struct json_object *object;
		struct json_array *array;
	};
} json_value;


typedef struct json_array {
	int length;
	struct json_value values[];
} json_array;
typedef struct json_key_value {
	const char* key;
	struct json_value* value;
} json_key_value;
typedef struct json_object {
	int length;
	struct json_key_value values[];
} json_object;
struct json_value* json_get_by_key (struct json_value * thiz,const char* key);

struct json_value* json_put_value (struct json_value * thiz,const char* key, struct json_value * value);

struct json_value* json_get_by_index (struct json_value * thiz,int index);

struct json_value* json_add_value (struct json_value * thiz,struct json_value * value);

struct json_value* json_new(enum json_type type);

void json_free (struct json_value ** value)

#ifdef __cplusplus
}
#endif
#endif
