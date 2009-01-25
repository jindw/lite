package org.xidea.el.parser;

/**
 * @author jindw
 */
public abstract interface ExpressionToken {

	//编译期间标记，compile time object
	public static final int BRACKET_BEGIN = 0xFFFE;//([{;
	public static final int BRACKET_END = 0xFFFF;//)]};
	
	//值类型（<=0）
	public static final int VALUE_VAR = -0x00;//n;
	//常量标记（String,Number,Boolean,Null）
	public static final int VALUE_CONSTANTS = -0x1;//c;
	public static final int VALUE_LAZY = -0x02;
	public static final int VALUE_NEW_LIST = -0x03;//[;
	public static final int VALUE_NEW_MAP = -0x04;//{;
	
	//符号标记 ????? !!

	//与正负符号共享字面值
	public static final int OP_ADD = (1<<2) +2;//+;//6
	public static final int OP_SUB = (2<<2) +2;//-;
	
	public static final int OP_MUL = (3<<2) +2;//*;
	public static final int OP_DIV = (4<<2) +2;///;
	public static final int OP_MOD = (5<<2) +2;//%;
	public static final int OP_QUESTION = (6<<2) +2;//?;
	public static final int OP_QUESTION_SELECT = (7<<2) +2;//:;

	public static final int OP_GET_PROP = (8<<2) +2;//.;
	public static final int OP_STATIC_GET_PROP = (9<<2) +1;//.#;解析时可以忽略改节点，表达式优化的时候处理
	
	public static final int OP_LT = (0xA<<2) +2;//<;
	public static final int OP_GT = (0xB<<2) +2;//>;
	public static final int OP_LTEQ = (0xC<<2) +2;//<=;
	public static final int OP_GTEQ = (0xD<<2) +2;//>=;
	public static final int OP_EQ = (0xE<<2) +2;//==;
	public static final int OP_NOTEQ = (0xF<<2) +2;//!=;
	public static final int OP_AND = (0x10<<2) +2;//&&;
	public static final int OP_OR = (0x12<<2) +2;//||;
	
	

	public static final int OP_NOT = (0x13<<2) +1;//!;
	public static final int OP_POS = (0x14<<2) +1;//+;//正数
	public static final int OP_NEG = (0x15<<2) +1;//-;//负数

	//public static final int OP_GET_METHOD = (0x16<<2) +2;//.();
	public static final int OP_INVOKE_METHOD = (0x17<<2) +2;//()


	//与Map Join 共享字面量（map join 会忽略）
	public static final int OP_PARAM_JOIN = (0x18<<2) +2;//,
	//与三元运算符共享字面值
	public static final int OP_MAP_PUSH = (0x19<<2) +2;//:,
	
	
	public abstract int getType();

	public abstract String toString();

	public abstract Object getParam();
}
