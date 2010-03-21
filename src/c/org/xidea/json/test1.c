#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>

#include "json.h"
#include "../../../json-c/json.h"

int main(int argc, char **argv)
{
  struct json_tokener *tok;
  struct json_object *my_string, *my_int, *my_object, *my_array;
  struct json_object *new_obj;
  int i;

  MC_SET_DEBUG(1);

  my_string = json_object_new_string("\t");
  printf("my_json_type=%d\n", json_get_type(my_string));
   printf("my_string=%s\n", json_object_get_string(my_string));
  printf("my_string.to_string()=%s\n", json_object_to_json_string(my_string));
  json_object_put(my_string);

  my_string = json_object_new_string("\\");
  printf("my_string=%s\n", json_object_get_string(my_string));
  printf("my_string.to_string()=%s\n", json_object_to_json_string(my_string));
  json_object_put(my_string);

  my_string = json_object_new_string("foo");
  printf("my_string=%s\n", json_object_get_string(my_string));
  printf("my_string.to_string()=%s\n", json_object_to_json_string(my_string));

  my_int = json_object_new_int(9);
  printf("my_int=%d\n", json_object_get_int(my_int));
  printf("my_int.to_string()=%s\n", json_object_to_json_string(my_int));

  return 0;
}
