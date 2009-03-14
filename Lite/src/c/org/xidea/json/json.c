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

#include "json.h"
#include "../../../json-c/json.h"
#include <stdio.h>
#include <string.h>

int json_get_type(struct json_value * thiz){
	switch(json_object_get_type(thiz)){
	case json_type_boolean:
		return JSON_BOOL;
	case json_type_int:
		return JSON_INT;
	case json_type_double:
		return JSON_FLOAT;
	case json_type_string:
		return JSON_STRING;
	case json_type_object:
		return JSON_OBJECT;
	case json_type_array:
		return JSON_ARRAY;
	case json_type_null:
		return JSON_NULL;
	}
	return -1;
}
int json_get_length (struct json_value * thiz){
	switch(json_object_get_type(thiz)){
	case json_type_string:
		return strlen(json_object_get_string(thiz));
	case json_type_object:
		return json_object_get_object(thiz)->count;
	case json_type_array:
		return json_object_array_length(thiz);
	}
	return -1;

}
struct json_value* json_get_by_key (struct json_value * thiz, const char* key){
	return json_object_object_get(this,key);
}

void json_set_by_key (struct json_value * thiz,const char* key, struct json_value * value){
	json_object_object_add(thiz,key,value);
}

void json_remove_by_key (struct json_value * thiz,const char* key){
	json_object_object_del(thiz,key);
}

struct json_value* json_get_by_index (struct json_value * thiz,int index){
	return json_object_array_get_idx(thiz,index);
}

void json_set_by_index (struct json_value * thiz,int index,struct json_value * value){
	json_object_array_put_idx(thiz,index,value);
}

void json_remove_by_index (struct json_value * thiz,const char* key){
	array* json_object_array_del_idx(thiz,index);

}

void json_add_value (struct json_value * thiz,struct json_value * value){
	json_object_array_add(thiz,index,value);
}


struct json_value* json_create(const int type){
	return NULL;
}

void json_set_bool (struct json_value * thiz,int value){
}

void json_set_int (struct json_value * thiz,const int value){
}

void json_set_float (struct json_value * thiz,const float value){
}

void json_set_string (struct json_value * thiz,const char* value){
}

void json_set_null (struct json_value * thiz){
}

int json_free(struct json_value * value){
	return 0;
}
